package com.taxivn.dispatch.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxivn.dispatch.ui.components.TaxiVnEmblem
import com.taxivn.dispatch.ui.theme.TaxiAmber
import com.taxivn.dispatch.ui.theme.TaxiTealDark
import com.taxivn.dispatch.ui.theme.TaxiTealPrimary

@Composable
fun SplashScreen(serverReachable: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "splash_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TaxiTealPrimary, TaxiTealDark, Color(0xFF0F172A))
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            TaxiVnEmblem(size = 104.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "TaxiVN",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hệ Thống Điều Hành Taxi Chính Thức",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.84f)
            )
            Spacer(modifier = Modifier.height(36.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = TaxiAmber,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (serverReachable) "Đang đồng bộ máy chủ TaxiVN..." else "Đang khởi tạo chế độ đồng bộ an toàn...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.68f)
            )
        }
    }
}
