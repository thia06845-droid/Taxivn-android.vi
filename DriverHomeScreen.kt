package com.taxivn.dispatch.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.taxivn.dispatch.data.model.DriverStatus
import com.taxivn.dispatch.data.model.Trip
import com.taxivn.dispatch.data.model.TripStatus
import com.taxivn.dispatch.ui.components.TaxiVnBrandLockup
import com.taxivn.dispatch.ui.theme.SlateBg
import com.taxivn.dispatch.ui.theme.SlateBorder
import com.taxivn.dispatch.ui.theme.SlateInk
import com.taxivn.dispatch.ui.theme.SlateMuted
import com.taxivn.dispatch.ui.theme.TaxiAmber
import com.taxivn.dispatch.ui.theme.TaxiEmerald
import com.taxivn.dispatch.ui.theme.TaxiTealPrimary
import com.taxivn.dispatch.ui.viewmodel.TaxiVnUiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    state: TaxiVnUiState,
    onToggleGps: (Boolean) -> Unit,
    onChangeStatus: (String, DriverStatus) -> Unit,
    onAdvanceTrip: (String, TripStatus, String?) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val driver = state.drivers.find { it.id == state.currentUser?.driverId } ?: state.drivers.first()
    val activeTrip = state.trips.find {
        it.assignedDriverId == driver.id &&
            (it.status == TripStatus.PICKING_UP || it.status == TripStatus.ARRIVED_PICKUP || it.status == TripStatus.CARRYING)
    }
    val pendingTrips = state.trips.filter { it.status == TripStatus.PENDING }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onToggleGps(true)
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    Scaffold(
        containerColor = SlateBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { TaxiVnBrandLockup(showSubtitle = false) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Đồng bộ")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Driver Identity & Status Card
            item {
                val isReady = driver.status == DriverStatus.READY || driver.status == DriverStatus.ON_TRIP
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = driver.fullName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateInk
                                )
                                Text(
                                    text = "${driver.code} · ${driver.licensePlate} · ${driver.vehicleModel}",
                                    fontSize = 12.sp,
                                    color = SlateMuted
                                )
                            }
                            Switch(
                                checked = isReady,
                                onCheckedChange = { checked ->
                                    val next = if (checked) DriverStatus.READY else DriverStatus.OFFLINE
                                    onChangeStatus(driver.id, next)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = TaxiEmerald)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Trạng thái: ${driver.status.labelVi}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (driver.status == DriverStatus.READY) TaxiEmerald else SlateInk
                            )
                            Text(
                                text = "Doanh thu: ${formatVnd(driver.todayRevenueVnd)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TaxiTealPrimary
                            )
                        }

                        if (driver.debtAmountVnd > 0L) {
                            Text(
                                text = "Công nợ hiện tại: ${formatVnd(driver.debtAmountVnd)}",
                                fontSize = 12.sp,
                                color = TaxiAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // GPS Telemetry Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GpsFixed,
                                    contentDescription = null,
                                    tint = if (state.gpsActive) TaxiEmerald else SlateMuted
                                )
                                Column {
                                    Text(
                                        text = if (state.gpsActive) "Định vị GPS đang phát trực tiếp" else "Định vị GPS chưa bật",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateInk
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.6f, %.6f · Sai số %.1fm · %.0f km/h",
                                            state.liveGps.latitude,
                                            state.liveGps.longitude,
                                            state.liveGps.accuracyMeters,
                                            state.liveGps.speedKmh
                                        ),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SlateMuted
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (state.gpsActive) {
                                    onToggleGps(false)
                                } else {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine) {
                                        onToggleGps(true)
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.gpsActive) SlateInk else TaxiTealPrimary
                            )
                        ) {
                            Text(if (state.gpsActive) "Tạm dừng phát GPS" else "Bật phát GPS tới Tổng đài")
                        }
                    }
                }
            }

            // Active Trip 3-Step Execution
            if (activeTrip != null) {
                item {
                    ActiveDriverTripCard(
                        trip = activeTrip,
                        onAdvanceStep = { next -> onAdvanceTrip(activeTrip.id, next, null) },
                        onCancel = { onAdvanceTrip(activeTrip.id, TripStatus.CANCELLED, "Khách hàng đổi kế hoạch") }
                    )
                }
            }

            // Available Pending Trips
            item {
                Text(
                    text = "Cuốc xe chờ nhận (${pendingTrips.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateInk
                )
            }

            items(pendingTrips, key = { it.id }) { trip ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${trip.code} · ${trip.vehicleType.labelVi}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TaxiTealPrimary
                            )
                            Text(
                                text = formatVnd(trip.estimatedFareVnd),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlateInk
                            )
                        }
                        Text(
                            text = "Điểm đón: ${trip.pickupAddress}",
                            fontSize = 13.sp,
                            color = SlateInk,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Điểm trả: ${trip.dropoffAddress}",
                            fontSize = 13.sp,
                            color = SlateMuted
                        )
                        Text(
                            text = "${trip.customerName} · ${trip.customerPhone} · ${trip.estimatedDistanceKm} km",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                        if (activeTrip == null && driver.status == DriverStatus.READY) {
                            Button(
                                onClick = { onAdvanceTrip(trip.id, TripStatus.PICKING_UP, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
                            ) {
                                Icon(Icons.Outlined.LocalTaxi, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Nhận cuốc ngay")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDriverTripCard(
    trip: Trip,
    onAdvanceStep: (TripStatus) -> Unit,
    onCancel: () -> Unit
) {
    val nextStep = when (trip.status) {
        TripStatus.PICKING_UP -> TripStatus.ARRIVED_PICKUP
        TripStatus.ARRIVED_PICKUP -> TripStatus.CARRYING
        TripStatus.CARRYING -> TripStatus.COMPLETED
        else -> null
    }
    val nextLabel = when (trip.status) {
        TripStatus.PICKING_UP -> "Xác nhận: 2. Đã đến - Chờ khách lên xe"
        TripStatus.ARRIVED_PICKUP -> "Xác nhận: 3. Đang chở khách tới điểm trả"
        TripStatus.CARRYING -> "Hoàn thành cuốc & Thu cước"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF0FDFA),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, TaxiTealPrimary, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chuyến đang thực hiện · ${trip.code}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaxiTealPrimary
                )
                Text(
                    text = formatVnd(trip.estimatedFareVnd),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateInk
                )
            }
            Text(
                text = trip.status.labelVi,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SlateInk
            )
            Text(text = "Đón: ${trip.pickupAddress}", fontSize = 13.sp, color = SlateInk)
            Text(text = "Trả: ${trip.dropoffAddress}", fontSize = 13.sp, color = SlateMuted)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = TaxiTealPrimary)
                Text(
                    text = "${trip.customerName} (${trip.customerPhone})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateInk
                )
            }

            if (nextStep != null) {
                Button(
                    onClick = { onAdvanceStep(nextStep) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
                ) {
                    Text(nextLabel, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Báo hủy chuyến")
            }
        }
    }
}

private fun formatVnd(amount: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${nf.format(amount)} đ"
}
