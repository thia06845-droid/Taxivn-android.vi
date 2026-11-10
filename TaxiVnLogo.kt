package com.taxivn.dispatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taxivn.dispatch.ui.theme.SlateInk
import com.taxivn.dispatch.ui.theme.TaxiAmber
import com.taxivn.dispatch.ui.theme.TaxiTealPrimary

@Composable
fun TaxiVnEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(TaxiTealPrimary),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height

            // Taxi roof sign
            drawRoundRect(
                color = TaxiAmber,
                topLeft = Offset(w * 0.28f, h * 0.08f),
                size = Size(w * 0.44f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )

            // Taxi cabin body
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.10f, h * 0.26f),
                size = Size(w * 0.80f, h * 0.50f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
            )

            // Windshield
            drawRoundRect(
                color = SlateInk,
                topLeft = Offset(w * 0.20f, h * 0.34f),
                size = Size(w * 0.60f, h * 0.17f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )

            // Headlights
            drawCircle(
                color = TaxiAmber,
                radius = w * 0.06f,
                center = Offset(w * 0.25f, h * 0.63f)
            )
            drawCircle(
                color = TaxiAmber,
                radius = w * 0.06f,
                center = Offset(w * 0.75f, h * 0.63f)
            )

            // Center GPS Grille
            drawRoundRect(
                color = TaxiTealPrimary,
                topLeft = Offset(w * 0.38f, h * 0.59f),
                size = Size(w * 0.24f, h * 0.08f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
        }
    }
}

@Composable
fun TaxiVnBrandLockup(
    showSubtitle: Boolean = true,
    darkText: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TaxiVnEmblem(size = 38.dp)
        Text(
            text = if (showSubtitle) "TaxiVN Điều Hành" else "TaxiVN",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = if (darkText) SlateInk else Color.White
        )
    }
}
