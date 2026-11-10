package com.taxivn.dispatch.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taxivn.dispatch.BuildConfig
import com.taxivn.dispatch.data.local.SessionPreferences
import com.taxivn.dispatch.data.model.CreateTripRequest
import com.taxivn.dispatch.data.model.Driver
import com.taxivn.dispatch.data.model.DriverStatus
import com.taxivn.dispatch.data.model.GpsCoordinate
import com.taxivn.dispatch.data.model.Trip
import com.taxivn.dispatch.data.model.TripStatus
import com.taxivn.dispatch.data.model.UserAccount
import com.taxivn.dispatch.data.model.UserRole
import com.taxivn.dispatch.data.model.VehicleType
import com.taxivn.dispatch.data.repository.TaxiVnRepository
import com.taxivn.dispatch.location.GpsLocationService
import com.taxivn.dispatch.location.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TaxiVnUiState(
    val isSplashDone: Boolean = false,
    val serverUrl: String = BuildConfig.DEFAULT_TAXIVN_SERVER_URL,
    val serverReachable: Boolean = true,
    val currentUser: UserAccount? = null,
    val isLoading: Boolean = false,
    val authError: String? = null,
    val drivers: List<Driver> = TaxiVnRepository.defaultSeedDrivers(),
    val trips: List<Trip> = TaxiVnRepository.defaultSeedTrips(),
    val liveGps: GpsCoordinate = GpsCoordinate(21.028511, 105.804817, 6.2f, 0f),
    val gpsActive: Boolean = false,
    val statusMessage: String? = null
)

class TaxiVnViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SessionPreferences(application)
    private val repository = TaxiVnRepository(BuildConfig.DEFAULT_TAXIVN_SERVER_URL)
    private val locationTracker = LocationTracker(application)

    private val _uiState = MutableStateFlow(TaxiVnUiState())
    val uiState: StateFlow<TaxiVnUiState> = _uiState.asStateFlow()

    private var gpsJob: Job? = null
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            val savedUrl = prefs.serverUrlFlow.first()
            repository.updateServerUrl(savedUrl)
            val reachable = repository.verifyServerReachability()
            _uiState.update {
                it.copy(
                    serverUrl = savedUrl,
                    serverReachable = reachable
                )
            }
            delay(1600L)
            _uiState.update { it.copy(isSplashDone = true) }
        }
    }

    fun updateServerUrl(newUrl: String) {
        viewModelScope.launch {
            prefs.saveServerUrl(newUrl)
            repository.updateServerUrl(newUrl)
            val reachable = repository.verifyServerReachability()
            _uiState.update {
                it.copy(
                    serverUrl = newUrl,
                    serverReachable = reachable,
                    statusMessage = if (reachable) "Đã kết nối máy chủ TaxiVN" else "Chế độ đồng bộ cục bộ (máy chủ không phản hồi API)"
                )
            }
        }
    }

    fun login(username: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            val result = repository.login(username.trim(), password, role)
            if (result.success && result.user != null) {
                prefs.saveSession(username.trim(), role.name, result.user.token)
                val snapshot = repository.syncSnapshot()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = result.user,
                        drivers = snapshot.drivers,
                        trips = snapshot.trips,
                        serverReachable = snapshot.serverOnline
                    )
                }
                startPeriodicSync()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authError = result.message ?: "Đăng nhập thất bại"
                    )
                }
            }
        }
    }

    fun logout() {
        stopGpsTracking()
        syncJob?.cancel()
        viewModelScope.launch {
            prefs.clearSession()
            _uiState.update {
                it.copy(
                    currentUser = null,
                    authError = null,
                    statusMessage = null
                )
            }
        }
    }

    fun refreshSnapshot() {
        viewModelScope.launch {
            val snapshot = repository.syncSnapshot()
            _uiState.update {
                it.copy(
                    drivers = snapshot.drivers,
                    trips = snapshot.trips,
                    serverReachable = snapshot.serverOnline
                )
            }
        }
    }

    fun startGpsTracking() {
        val ctx = getApplication<Application>()
        GpsLocationService.start(ctx)
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            _uiState.update { it.copy(gpsActive = true) }
            locationTracker.observeLocationUpdates().collect { coord ->
                _uiState.update { it.copy(liveGps = coord) }
                val driverId = _uiState.value.currentUser?.driverId
                if (driverId != null) {
                    val updated = repository.pushDriverGps(driverId, coord)
                    if (updated != null) {
                        replaceDriverInState(updated)
                    }
                }
            }
        }
    }

    fun stopGpsTracking() {
        gpsJob?.cancel()
        gpsJob = null
        GpsLocationService.stop(getApplication())
        _uiState.update { it.copy(gpsActive = false) }
    }

    fun setDriverStatus(driverId: String, status: DriverStatus) {
        viewModelScope.launch {
            val updated = repository.updateDriverStatus(driverId, status)
            if (updated != null) {
                replaceDriverInState(updated)
                _uiState.update { it.copy(statusMessage = "Trạng thái: ${status.labelVi}") }
            }
        }
    }

    fun createTrip(
        customerName: String,
        customerPhone: String,
        pickupAddress: String,
        dropoffAddress: String,
        vehicleType: VehicleType,
        distanceKm: Double,
        fareVnd: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val trip = repository.createTrip(
                CreateTripRequest(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    pickupAddress = pickupAddress,
                    dropoffAddress = dropoffAddress,
                    vehicleType = vehicleType,
                    estimatedDistanceKm = distanceKm,
                    estimatedFareVnd = fareVnd,
                    notes = notes
                )
            )
            _uiState.update {
                it.copy(
                    trips = listOf(trip) + it.trips.filterNot { existing -> existing.id == trip.id },
                    statusMessage = "Đã tạo cuốc xe ${trip.code}"
                )
            }
        }
    }

    fun assignDriverToTrip(tripId: String, driverId: String) {
        viewModelScope.launch {
            val updatedTrip = repository.assignTrip(tripId, driverId)
            if (updatedTrip != null) {
                refreshSnapshot()
                _uiState.update { it.copy(statusMessage = "Đã gán xe ${updatedTrip.assignedPlate} cho cuốc ${updatedTrip.code}") }
            }
        }
    }

    fun advanceTripStep(tripId: String, nextStatus: TripStatus, cancelReason: String? = null) {
        viewModelScope.launch {
            val updated = repository.updateTripStatus(tripId, nextStatus, cancelReason)
            if (updated != null) {
                refreshSnapshot()
                _uiState.update { it.copy(statusMessage = "Cập nhật chuyến ${updated.code}: ${nextStatus.labelVi}") }
            }
        }
    }

    fun settleDriverDebt(driverId: String, amountVnd: Long) {
        viewModelScope.launch {
            val updated = repository.settleDriverDebt(driverId, amountVnd)
            if (updated != null) {
                replaceDriverInState(updated)
                _uiState.update { it.copy(statusMessage = "Đã thanh toán công nợ cho ${updated.fullName}") }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun replaceDriverInState(driver: Driver) {
        _uiState.update { state ->
            state.copy(
                drivers = state.drivers.map { if (it.id == driver.id) driver else it }
            )
        }
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                val snapshot = repository.syncSnapshot()
                _uiState.update {
                    it.copy(
                        drivers = snapshot.drivers,
                        trips = snapshot.trips,
                        serverReachable = snapshot.serverOnline
                    )
                }
            }
        }
    }
}
