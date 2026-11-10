package com.taxivn.dispatch.data.model

import com.google.gson.annotations.SerializedName

enum class UserRole(val labelVi: String) {
    DRIVER("Tài xế"),
    DISPATCHER("Nhân viên Điều hành"),
    CHIEF_DISPATCHER("Trưởng nhóm Tổng Điều Hành")
}

enum class DriverStatus(val labelVi: String) {
    @SerializedName("OFFLINE")
    OFFLINE("Chưa bật nhận khách"),

    @SerializedName("READY")
    READY("Sẵn Sàng"),

    @SerializedName("ON_TRIP")
    ON_TRIP("Đang Chở Khách"),

    @SerializedName("LOCKED")
    LOCKED("Khóa bởi Trưởng nhóm điều hành")
}

enum class VehicleType(val labelVi: String) {
    @SerializedName("FOUR_SEAT")
    FOUR_SEAT("Xe 4 chỗ"),

    @SerializedName("SEVEN_SEAT")
    SEVEN_SEAT("Xe 7 chỗ"),

    @SerializedName("PRIVATE_CAR")
    PRIVATE_CAR("Xe riêng"),

    @SerializedName("SHARED_RIDE")
    SHARED_RIDE("Khách đi ghép")
}

enum class TripStatus(val labelVi: String, val stepIndex: Int) {
    @SerializedName("PENDING")
    PENDING("Đang chờ gán xe", 0),

    @SerializedName("PICKING_UP")
    PICKING_UP("1. Đang đến điểm đón khách", 1),

    @SerializedName("ARRIVED_PICKUP")
    ARRIVED_PICKUP("2. Đã đến - Đang chờ khách lên xe", 2),

    @SerializedName("CARRYING")
    CARRYING("3. Đang chở khách tới điểm trả", 3),

    @SerializedName("COMPLETED")
    COMPLETED("Hoàn thành", 4),

    @SerializedName("CANCELLED")
    CANCELLED("Đã hủy", -1)
}

data class GpsCoordinate(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 8.0f,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class UserAccount(
    val id: String,
    val username: String,
    val fullName: String,
    val phone: String,
    val role: UserRole,
    val driverId: String? = null,
    val token: String = ""
)

data class Driver(
    val id: String,
    val code: String,
    val fullName: String,
    val phone: String,
    val licensePlate: String,
    val vehicleModel: String,
    val vehicleType: VehicleType,
    val status: DriverStatus,
    val gps: GpsCoordinate,
    val debtAmountVnd: Long = 0L,
    val todayRevenueVnd: Long = 0L,
    val completedTripsToday: Int = 0,
    val rating: Double = 4.9
)

data class Trip(
    val id: String,
    val code: String,
    val customerName: String,
    val customerPhone: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val vehicleType: VehicleType,
    val estimatedDistanceKm: Double,
    val estimatedFareVnd: Long,
    val status: TripStatus,
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val assignedPlate: String? = null,
    val notes: String = "",
    val cancelReason: String? = null,
    val createdAtLabel: String
)

data class LoginRequest(
    val username: String,
    val password: String,
    val role: UserRole
)

data class LoginResponse(
    val success: Boolean,
    val user: UserAccount? = null,
    val message: String? = null
)

data class DriverStatusUpdateRequest(
    val driverId: String,
    val status: DriverStatus
)

data class DriverGpsUpdateRequest(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedKmh: Float
)

data class AssignTripRequest(
    val tripId: String,
    val driverId: String
)

data class UpdateTripStatusRequest(
    val tripId: String,
    val status: TripStatus,
    val cancelReason: String? = null
)

data class CreateTripRequest(
    val customerName: String,
    val customerPhone: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val vehicleType: VehicleType,
    val estimatedDistanceKm: Double,
    val estimatedFareVnd: Long,
    val notes: String = ""
)

data class SettleDebtRequest(
    val driverId: String,
    val amountVnd: Long
)

data class DispatchSyncSnapshot(
    val serverOnline: Boolean,
    val serverName: String,
    val drivers: List<Driver>,
    val trips: List<Trip>,
    val syncedAtEpochMs: Long = System.currentTimeMillis()
)
