package com.taxivn.dispatch.ui.screens

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxivn.dispatch.data.model.Driver
import com.taxivn.dispatch.data.model.DriverStatus
import com.taxivn.dispatch.data.model.Trip
import com.taxivn.dispatch.data.model.TripStatus
import com.taxivn.dispatch.data.model.VehicleType
import com.taxivn.dispatch.ui.components.TaxiVnBrandLockup
import com.taxivn.dispatch.ui.theme.SlateBg
import com.taxivn.dispatch.ui.theme.SlateBorder
import com.taxivn.dispatch.ui.theme.SlateInk
import com.taxivn.dispatch.ui.theme.SlateMuted
import com.taxivn.dispatch.ui.theme.TaxiAmber
import com.taxivn.dispatch.ui.theme.TaxiCrimson
import com.taxivn.dispatch.ui.theme.TaxiEmerald
import com.taxivn.dispatch.ui.theme.TaxiTealPrimary
import com.taxivn.dispatch.ui.viewmodel.TaxiVnUiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherHomeScreen(
    state: TaxiVnUiState,
    onCreateTrip: (String, String, String, String, VehicleType, Double, Long, String) -> Unit,
    onAssignDriver: (String, String) -> Unit,
    onUpdateTripStatus: (String, TripStatus, String?) -> Unit,
    onChangeDriverStatus: (String, DriverStatus) -> Unit,
    onSettleDebt: (String, Long) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToAssign by remember { mutableStateOf<Trip?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    val readyDrivers = state.drivers.filter { it.status == DriverStatus.READY }
    val pendingTrips = state.trips.count { it.status == TripStatus.PENDING }
    val activeTrips = state.trips.count {
        it.status == TripStatus.PICKING_UP || it.status == TripStatus.ARRIVED_PICKUP || it.status == TripStatus.CARRYING
    }

    Scaffold(
        containerColor = SlateBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { TaxiVnBrandLockup(showSubtitle = true) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Làm mới")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Đăng xuất")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TaxiTealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tạo cuốc xe")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
        ) {
            // Operational KPI Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DispatchKpiBox("Chờ gán xe", pendingTrips.toString(), TaxiAmber, Modifier.weight(1f))
                DispatchKpiBox("Đang chạy", activeTrips.toString(), TaxiTealPrimary, Modifier.weight(1f))
                DispatchKpiBox("Xe sẵn sàng", readyDrivers.size.toString(), TaxiEmerald, Modifier.weight(1f))
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = TaxiTealPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Điều phối cuốc (${state.trips.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Đội xe GPS (${state.drivers.size})") }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.trips, key = { it.id }) { trip ->
                        DispatchTripCard(
                            trip = trip,
                            onAssignClick = { tripToAssign = trip },
                            onCancelClick = {
                                onUpdateTripStatus(trip.id, TripStatus.CANCELLED, "Tổng đài hủy cuốc")
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.drivers, key = { it.id }) { driver ->
                        FleetDriverCard(
                            driver = driver,
                            onToggleLock = {
                                val next = if (driver.status == DriverStatus.LOCKED) DriverStatus.READY else DriverStatus.LOCKED
                                onChangeDriverStatus(driver.id, next)
                            },
                            onSettleDebt = { onSettleDebt(driver.id, driver.debtAmountVnd) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTripDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { name, phone, pickup, dropoff, type, km, fare, notes ->
                onCreateTrip(name, phone, pickup, dropoff, type, km, fare, notes)
                showCreateDialog = false
            }
        )
    }

    tripToAssign?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToAssign = null },
            title = { Text("Gán tài xế cho ${trip.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (readyDrivers.isEmpty()) {
                        Text("Hiện chưa có tài xế ở trạng thái Sẵn Sàng.")
                    } else {
                        readyDrivers.forEach { d ->
                            OutlinedButton(
                                onClick = {
                                    onAssignDriver(trip.id, d.id)
                                    tripToAssign = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("${d.fullName} · ${d.licensePlate} (${d.vehicleType.labelVi})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { tripToAssign = null }) { Text("Đóng") }
            }
        )
    }
}

@Composable
private fun DispatchKpiBox(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = modifier.border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 11.sp, color = SlateMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable
private fun DispatchTripCard(
    trip: Trip,
    onAssignClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${trip.code} · ${trip.vehicleType.labelVi}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TaxiTealPrimary
                )
                Text(
                    text = formatVndDispatch(trip.estimatedFareVnd),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = SlateInk
                )
            }
            Text(text = trip.status.labelVi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateInk)
            Text(text = "Đón: ${trip.pickupAddress}", fontSize = 13.sp, color = SlateInk)
            Text(text = "Trả: ${trip.dropoffAddress}", fontSize = 13.sp, color = SlateMuted)
            Text(
                text = "Khách: ${trip.customerName} · ${trip.customerPhone}",
                fontSize = 12.sp,
                color = SlateMuted
            )
            if (!trip.assignedDriverName.isNullOrBlank()) {
                Text(
                    text = "Xe gán: ${trip.assignedDriverName} (${trip.assignedPlate})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaxiEmerald
                )
            }

            if (trip.status == TripStatus.PENDING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAssignClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
                    ) {
                        Icon(Icons.Outlined.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Gán tài xế")
                    }
                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hủy cuốc", color = TaxiCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetDriverCard(
    driver: Driver,
    onToggleLock: () -> Unit,
    onSettleDebt: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${driver.code} · ${driver.fullName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SlateInk
                )
                Text(
                    text = driver.status.labelVi,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (driver.status) {
                        DriverStatus.READY -> TaxiEmerald
                        DriverStatus.ON_TRIP -> TaxiTealPrimary
                        DriverStatus.LOCKED -> TaxiCrimson
                        DriverStatus.OFFLINE -> SlateMuted
                    }
                )
            }
            Text(
                text = "${driver.licensePlate} · ${driver.vehicleModel} · ${driver.vehicleType.labelVi}",
                fontSize = 12.sp,
                color = SlateMuted
            )
            Text(
                text = String.format(
                    Locale.US,
                    "GPS: %.5f, %.5f · %.0f km/h",
                    driver.gps.latitude,
                    driver.gps.longitude,
                    driver.gps.speedKmh
                ),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = SlateMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Doanh thu: ${formatVndDispatch(driver.todayRevenueVnd)} · Nợ: ${formatVndDispatch(driver.debtAmountVnd)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateInk
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (driver.debtAmountVnd > 0L) {
                    Button(
                        onClick = onSettleDebt,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
                    ) {
                        Text("Thanh toán nợ")
                    }
                }
                OutlinedButton(
                    onClick = onToggleLock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (driver.status == DriverStatus.LOCKED) "Mở khóa xe" else "Khóa tài xế")
                }
            }
        }
    }
}

@Composable
private fun CreateTripDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, VehicleType, Double, Long, String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var pickup by remember { mutableStateOf("") }
    var dropoff by remember { mutableStateOf("") }
    var fareText by remember { mutableStateOf("150000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tổng đài tạo cuốc xe mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Tên khách hàng") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Điểm đón khách") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dropoff,
                    onValueChange = { dropoff = it },
                    label = { Text("Điểm trả khách") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = fareText,
                    onValueChange = { fareText = it },
                    label = { Text("Cước dự kiến (VNĐ)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fare = fareText.toLongOrNull() ?: 150_000L
                    onSubmit(
                        customerName.ifBlank { "Khách tổng đài" },
                        customerPhone.ifBlank { "0900 000 000" },
                        pickup.ifBlank { "Trung tâm Hà Nội" },
                        dropoff.ifBlank { "Sân bay Nội Bài" },
                        VehicleType.FOUR_SEAT,
                        12.0,
                        fare,
                        "Tạo từ ứng dụng Android Điều hành"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
            ) {
                Text("Tạo cuốc")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private fun formatVndDispatch(amount: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${nf.format(amount)} đ"
}
