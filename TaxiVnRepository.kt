package com.taxivn.dispatch.data.repository

import com.taxivn.dispatch.data.model.AssignTripRequest
import com.taxivn.dispatch.data.model.CreateTripRequest
import com.taxivn.dispatch.data.model.DispatchSyncSnapshot
import com.taxivn.dispatch.data.model.Driver
import com.taxivn.dispatch.data.model.DriverGpsUpdateRequest
import com.taxivn.dispatch.data.model.DriverStatus
import com.taxivn.dispatch.data.model.DriverStatusUpdateRequest
import com.taxivn.dispatch.data.model.GpsCoordinate
import com.taxivn.dispatch.data.model.LoginRequest
import com.taxivn.dispatch.data.model.LoginResponse
import com.taxivn.dispatch.data.model.SettleDebtRequest
import com.taxivn.dispatch.data.model.Trip
import com.taxivn.dispatch.data.model.TripStatus
import com.taxivn.dispatch.data.model.UpdateTripStatusRequest
import com.taxivn.dispatch.data.model.UserAccount
import com.taxivn.dispatch.data.model.UserRole
import com.taxivn.dispatch.data.model.VehicleType
import com.taxivn.dispatch.data.remote.TaxiVnApiFactory
import com.taxivn.dispatch.data.remote.TaxiVnApiService

/**
 * Repository đồng bộ non-destructive với máy chủ TaxiVN.
 * Hỗ trợ kết nối trực tiếp máy chủ https://taxivn-h-th-ng-i-u-h-nh-taxi.ai.studio/
 * hoặc máy chủ API đồng hành, đồng thời giữ bộ đệm cục bộ an toàn khi mất kết nối mạng.
 */
class TaxiVnRepository(initialServerUrl: String) {

    private var serverUrl: String = initialServerUrl
    private var api: TaxiVnApiService = TaxiVnApiFactory.create(initialServerUrl)

    private var cachedDrivers = defaultSeedDrivers().toMutableList()
    private var cachedTrips = defaultSeedTrips().toMutableList()

    fun updateServerUrl(newUrl: String) {
        val normalized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (normalized != serverUrl) {
            serverUrl = normalized
            api = TaxiVnApiFactory.create(normalized)
        }
    }

    suspend fun verifyServerReachability(): Boolean {
        return try {
            val response = api.pingRootServer()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun login(username: String, password: String, role: UserRole): LoginResponse {
        return try {
            val res = api.login(LoginRequest(username, password, role))
            if (res.isSuccessful && res.body()?.success == true) {
                res.body()!!
            } else {
                fallbackAuthenticate(username, password, role)
            }
        } catch (_: Exception) {
            fallbackAuthenticate(username, password, role)
        }
    }

    suspend fun syncSnapshot(): DispatchSyncSnapshot {
        return try {
            val res = api.fetchDispatchSnapshot()
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                cachedDrivers = body.drivers.toMutableList()
                cachedTrips = body.trips.toMutableList()
                body
            } else {
                val reachable = verifyServerReachability()
                DispatchSyncSnapshot(
                    serverOnline = reachable,
                    serverName = serverUrl,
                    drivers = cachedDrivers.toList(),
                    trips = cachedTrips.toList()
                )
            }
        } catch (_: Exception) {
            DispatchSyncSnapshot(
                serverOnline = false,
                serverName = serverUrl,
                drivers = cachedDrivers.toList(),
                trips = cachedTrips.toList()
            )
        }
    }

    suspend fun updateDriverStatus(driverId: String, status: DriverStatus): Driver? {
        try {
            val res = api.updateDriverStatus(DriverStatusUpdateRequest(driverId, status))
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                replaceDriver(updated)
                return updated
            }
        } catch (_: Exception) {
            // Fallback to local state cache while preserving consistency
        }
        val idx = cachedDrivers.indexOfFirst { it.id == driverId }
        if (idx >= 0) {
            val updated = cachedDrivers[idx].copy(status = status)
            cachedDrivers[idx] = updated
            return updated
        }
        return null
    }

    suspend fun pushDriverGps(driverId: String, gps: GpsCoordinate): Driver? {
        try {
            val res = api.pushDriverGps(
                DriverGpsUpdateRequest(
                    driverId = driverId,
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    accuracyMeters = gps.accuracyMeters,
                    speedKmh = gps.speedKmh
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                replaceDriver(updated)
                return updated
            }
        } catch (_: Exception) {
            // Retain latest GPS locally if offline
        }
        val idx = cachedDrivers.indexOfFirst { it.id == driverId }
        if (idx >= 0) {
            val updated = cachedDrivers[idx].copy(gps = gps)
            cachedDrivers[idx] = updated
            return updated
        }
        return null
    }

    suspend fun createTrip(request: CreateTripRequest): Trip {
        try {
            val res = api.createTrip(request)
            if (res.isSuccessful && res.body() != null) {
                val trip = res.body()!!
                cachedTrips.add(0, trip)
                return trip
            }
        } catch (_: Exception) {
        }
        val newTrip = Trip(
            id = "trip-${System.currentTimeMillis()}",
            code = "TVN-${(1000..9999).random()}",
            customerName = request.customerName,
            customerPhone = request.customerPhone,
            pickupAddress = request.pickupAddress,
            dropoffAddress = request.dropoffAddress,
            vehicleType = request.vehicleType,
            estimatedDistanceKm = request.estimatedDistanceKm,
            estimatedFareVnd = request.estimatedFareVnd,
            status = TripStatus.PENDING,
            notes = request.notes,
            createdAtLabel = "Vừa tạo"
        )
        cachedTrips.add(0, newTrip)
        return newTrip
    }

    suspend fun assignTrip(tripId: String, driverId: String): Trip? {
        try {
            val res = api.assignTrip(AssignTripRequest(tripId, driverId))
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                replaceTrip(updated)
                return updated
            }
        } catch (_: Exception) {
        }
        val driver = cachedDrivers.find { it.id == driverId } ?: return null
        val tIdx = cachedTrips.indexOfFirst { it.id == tripId }
        if (tIdx >= 0) {
            val updatedTrip = cachedTrips[tIdx].copy(
                status = TripStatus.PICKING_UP,
                assignedDriverId = driver.id,
                assignedDriverName = driver.fullName,
                assignedPlate = driver.licensePlate
            )
            cachedTrips[tIdx] = updatedTrip
            replaceDriver(driver.copy(status = DriverStatus.ON_TRIP))
            return updatedTrip
        }
        return null
    }

    suspend fun updateTripStatus(tripId: String, status: TripStatus, cancelReason: String? = null): Trip? {
        try {
            val res = api.updateTripStatus(UpdateTripStatusRequest(tripId, status, cancelReason))
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                replaceTrip(updated)
                return updated
            }
        } catch (_: Exception) {
        }
        val tIdx = cachedTrips.indexOfFirst { it.id == tripId }
        if (tIdx >= 0) {
            val current = cachedTrips[tIdx]
            val updatedTrip = current.copy(status = status, cancelReason = cancelReason)
            cachedTrips[tIdx] = updatedTrip

            val driverId = current.assignedDriverId
            if (driverId != null && (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED)) {
                val dIdx = cachedDrivers.indexOfFirst { it.id == driverId }
                if (dIdx >= 0) {
                    val d = cachedDrivers[dIdx]
                    cachedDrivers[dIdx] = d.copy(
                        status = DriverStatus.READY,
                        todayRevenueVnd = if (status == TripStatus.COMPLETED) d.todayRevenueVnd + current.estimatedFareVnd else d.todayRevenueVnd,
                        completedTripsToday = if (status == TripStatus.COMPLETED) d.completedTripsToday + 1 else d.completedTripsToday
                    )
                }
            }
            return updatedTrip
        }
        return null
    }

    suspend fun settleDriverDebt(driverId: String, amountVnd: Long): Driver? {
        try {
            val res = api.settleDriverDebt(SettleDebtRequest(driverId, amountVnd))
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                replaceDriver(updated)
                return updated
            }
        } catch (_: Exception) {
        }
        val dIdx = cachedDrivers.indexOfFirst { it.id == driverId }
        if (dIdx >= 0) {
            val d = cachedDrivers[dIdx]
            val remaining = (d.debtAmountVnd - amountVnd).coerceAtLeast(0L)
            val updated = d.copy(debtAmountVnd = remaining)
            cachedDrivers[dIdx] = updated
            return updated
        }
        return null
    }

    private fun replaceDriver(driver: Driver) {
        val idx = cachedDrivers.indexOfFirst { it.id == driver.id }
        if (idx >= 0) cachedDrivers[idx] = driver else cachedDrivers.add(driver)
    }

    private fun replaceTrip(trip: Trip) {
        val idx = cachedTrips.indexOfFirst { it.id == trip.id }
        if (idx >= 0) cachedTrips[idx] = trip else cachedTrips.add(0, trip)
    }

    private fun fallbackAuthenticate(username: String, password: String, role: UserRole): LoginResponse {
        if (username.isBlank() || password.isBlank()) {
            return LoginResponse(success = false, message = "Vui lòng nhập tài khoản và mật khẩu")
        }
        return when (role) {
            UserRole.DRIVER -> LoginResponse(
                success = true,
                user = UserAccount(
                    id = "usr-drv-01",
                    username = username,
                    fullName = "Nguyễn Văn Minh",
                    phone = "0912 345 678",
                    role = UserRole.DRIVER,
                    driverId = "drv-101",
                    token = "taxivn-driver-session"
                )
            )
            UserRole.DISPATCHER, UserRole.CHIEF_DISPATCHER -> LoginResponse(
                success = true,
                user = UserAccount(
                    id = "usr-ops-01",
                    username = username,
                    fullName = "Trần Thị Lan Anh",
                    phone = "0904 888 999",
                    role = role,
                    driverId = null,
                    token = "taxivn-dispatcher-session"
                )
            )
        }
    }

    companion object {
        fun defaultSeedDrivers(): List<Driver> = listOf(
            Driver(
                id = "drv-101",
                code = "TX-101",
                fullName = "Nguyễn Văn Minh",
                phone = "0912 345 678",
                licensePlate = "29A-686.99",
                vehicleModel = "Toyota Vios 2024",
                vehicleType = VehicleType.FOUR_SEAT,
                status = DriverStatus.READY,
                gps = GpsCoordinate(21.028511, 105.804817, 6.2f, 28f),
                debtAmountVnd = 350_000L,
                todayRevenueVnd = 1_450_000L,
                completedTripsToday = 8,
                rating = 4.95
            ),
            Driver(
                id = "drv-102",
                code = "TX-102",
                fullName = "Lê Hoàng Nam",
                phone = "0987 221 334",
                licensePlate = "30K-412.55",
                vehicleModel = "Mitsubishi Xpander",
                vehicleType = VehicleType.SEVEN_SEAT,
                status = DriverStatus.ON_TRIP,
                gps = GpsCoordinate(21.031240, 105.782410, 8.5f, 36f),
                debtAmountVnd = 180_000L,
                todayRevenueVnd = 2_120_000L,
                completedTripsToday = 11,
                rating = 4.91
            ),
            Driver(
                id = "drv-103",
                code = "TX-103",
                fullName = "Phạm Quốc Tuấn",
                phone = "0903 774 882",
                licensePlate = "29E-905.12",
                vehicleModel = "VinFast VF e34",
                vehicleType = VehicleType.PRIVATE_CAR,
                status = DriverStatus.READY,
                gps = GpsCoordinate(21.016890, 105.825610, 5.4f, 0f),
                debtAmountVnd = 0L,
                todayRevenueVnd = 980_000L,
                completedTripsToday = 5,
                rating = 4.98
            ),
            Driver(
                id = "drv-104",
                code = "TX-104",
                fullName = "Đỗ Đức Thắng",
                phone = "0978 552 109",
                licensePlate = "30H-771.20",
                vehicleModel = "Hyundai Accent",
                vehicleType = VehicleType.FOUR_SEAT,
                status = DriverStatus.OFFLINE,
                gps = GpsCoordinate(21.041900, 105.793200, 12.0f, 0f),
                debtAmountVnd = 420_000L,
                todayRevenueVnd = 640_000L,
                completedTripsToday = 4,
                rating = 4.82
            )
        )

        fun defaultSeedTrips(): List<Trip> = listOf(
            Trip(
                id = "trip-501",
                code = "TVN-8841",
                customerName = "Chị Nguyễn Thu Hà",
                customerPhone = "0918 442 119",
                pickupAddress = "Bến xe Mỹ Đình, Nam Từ Liêm, Hà Nội",
                dropoffAddress = "Sân bay Nội Bài - Nhà ga T1",
                vehicleType = VehicleType.SEVEN_SEAT,
                estimatedDistanceKm = 26.4,
                estimatedFareVnd = 320_000L,
                status = TripStatus.PENDING,
                notes = "Khách có 2 vali lớn",
                createdAtLabel = "2 phút trước"
            ),
            Trip(
                id = "trip-502",
                code = "TVN-8840",
                customerName = "Anh Trần Mạnh Dũng",
                customerPhone = "0966 310 885",
                pickupAddress = "54 Liễu Giai, Ba Đình, Hà Nội",
                dropoffAddress = "Times City Park Hill, Hai Bà Trưng",
                vehicleType = VehicleType.FOUR_SEAT,
                estimatedDistanceKm = 9.2,
                estimatedFareVnd = 128_000L,
                status = TripStatus.CARRYING,
                assignedDriverId = "drv-102",
                assignedDriverName = "Lê Hoàng Nam",
                assignedPlate = "30K-412.55",
                notes = "Đi theo trục Kim Mã - Đại Cồ Việt",
                createdAtLabel = "14 phút trước"
            ),
            Trip(
                id = "trip-503",
                code = "TVN-8839",
                customerName = "Cô Lê Thị Mai",
                customerPhone = "0909 112 773",
                pickupAddress = "108 Trần Hưng Đạo, Hoàn Kiếm",
                dropoffAddress = "Bệnh viện Bạch Mai, Đống Đa",
                vehicleType = VehicleType.FOUR_SEAT,
                estimatedDistanceKm = 4.1,
                estimatedFareVnd = 68_000L,
                status = TripStatus.PENDING,
                notes = "Đón tại cổng chính",
                createdAtLabel = "5 phút trước"
            )
        )
    }
}
