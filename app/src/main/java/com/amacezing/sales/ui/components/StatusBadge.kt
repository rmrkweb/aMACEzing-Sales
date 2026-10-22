package com.amacezing.sales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SintraBoardBlue
import com.amacezing.sales.ui.theme.StatusCancelled
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPartiallyPaid
import com.amacezing.sales.ui.theme.StatusPending

@Composable
fun PaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        PaymentStatus.PAID -> StatusPaid.copy(alpha = 0.15f) to StatusPaid
        PaymentStatus.PARTIALLY_PAID -> StatusPartiallyPaid.copy(alpha = 0.15f) to StatusPartiallyPaid
        PaymentStatus.PENDING -> StatusPending.copy(alpha = 0.18f) to StatusPending
        PaymentStatus.CANCELLED -> StatusCancelled.copy(alpha = 0.15f) to StatusCancelled
    }

    Box(
        modifier = modifier
            .testTag("status_badge_${status.name.lowercase()}")
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BusinessBadge(
    businessType: BusinessType,
    modifier: Modifier = Modifier
) {
    val businesses = LocalBusinesses.current
    val textColor = businesses.colorOf(businessType)
    val bgColor = textColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .testTag("business_badge_${businessType.lowercase()}")
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = businesses.nameOf(businessType),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
