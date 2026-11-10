package com.taxivn.dispatch.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxivn.dispatch.data.model.UserRole
import com.taxivn.dispatch.ui.components.TaxiVnEmblem
import com.taxivn.dispatch.ui.theme.SlateBg
import com.taxivn.dispatch.ui.theme.SlateBorder
import com.taxivn.dispatch.ui.theme.SlateInk
import com.taxivn.dispatch.ui.theme.SlateMuted
import com.taxivn.dispatch.ui.theme.TaxiCrimson
import com.taxivn.dispatch.ui.theme.TaxiTealPrimary

@Composable
fun LoginScreen(
    serverUrl: String,
    serverReachable: Boolean,
    isLoading: Boolean,
    authError: String?,
    onUpdateServerUrl: (String) -> Unit,
    onLogin: (String, String, UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.DRIVER) }
    var username by remember { mutableStateOf("tx101") }
    var password by remember { mutableStateOf("taxivn2026") }
    var showServerEditor by remember { mutableStateOf(false) }
    var serverInput by remember(serverUrl) { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TaxiVnEmblem(size = 58.dp)
            Column {
                Text(
                    text = "TaxiVN",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateInk
                )
                Text(
                    text = "Đăng nhập hệ thống điều hành",
                    fontSize = 14.sp,
                    color = SlateMuted
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Phân hệ đăng nhập",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateMuted
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val roles = listOf(UserRole.DRIVER, UserRole.DISPATCHER)
                    roles.forEachIndexed { index, role ->
                        SegmentedButton(
                            selected = selectedRole == role,
                            onClick = {
                                selectedRole = role
                                username = if (role == UserRole.DRIVER) "tx101" else "dieuhanh01"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size)
                        ) {
                            Text(
                                text = if (role == UserRole.DRIVER) "Tài xế" else "Điều hành",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (selectedRole == UserRole.DRIVER) "Mã tài xế / SĐT" else "Tài khoản điều hành") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!authError.isNullOrBlank()) {
                    Text(
                        text = authError,
                        color = TaxiCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { onLogin(username, password, selectedRole) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TaxiTealPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (selectedRole == UserRole.DRIVER) "Vào ca Tài xế" else "Vào Trung tâm Điều hành",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = TaxiTealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (serverReachable) "Máy chủ TaxiVN trực tuyến" else "Chế độ đồng bộ ngoại tuyến",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateInk
                        )
                    }
                    TextButton(onClick = { showServerEditor = !showServerEditor }) {
                        Text(if (showServerEditor) "Đóng" else "Cấu hình")
                    }
                }

                Text(
                    text = serverUrl,
                    fontSize = 12.sp,
                    color = SlateMuted
                )

                if (showServerEditor) {
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        label = { Text("Địa chỉ máy chủ TaxiVN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            onUpdateServerUrl(serverInput)
                            showServerEditor = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateInk),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lưu & Kiểm tra kết nối")
                    }
                }
            }
        }
    }
}
