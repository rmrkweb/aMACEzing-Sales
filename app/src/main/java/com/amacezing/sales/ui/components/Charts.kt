
package com.amacezing.sales.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SecondaryAmber
import com.amacezing.sales.ui.theme.SintraBoardBlue
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.ui.theme.StatusPending
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.formatCurrency
import com.amacezing.sales.viewmodel.ChartBarData
import com.amacezing.sales.viewmodel.DashboardMetrics
import com.amacezing.sales.viewmodel.ProductSaleStat

@Composable
fun SalesAndProfitTrendChart(
    data: List<ChartBarData>,
    modifier: Modifier = Modifier,
    isProfitMasked: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sales_trend_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sales & Profit Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF00B8F0), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Net Sales",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFA78BFA), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Profit",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transaction data for the selected period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = (
                    data.maxOfOrNull {
                        maxOf(it.value, it.secondaryValue)
                    } ?: 1.0
                ).coerceAtLeast(100.0)

                // Separate colors for the chart bars
                val salesColor = Color(0xFF00B8F0)
                val profitColor = Color(0xFFA78BFA)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 30f
                    val groupWidth = canvasWidth / data.size
                    val barWidth = (groupWidth * 0.3f).coerceAtMost(28f)

                    // Draw baseline
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, canvasHeight),
                        end = Offset(canvasWidth, canvasHeight),
                        strokeWidth = 2f
                    )

                    data.forEachIndexed { index, item ->
                        val groupCenterX =
                            (index * groupWidth) + (groupWidth / 2f)

                        // Net Sales bar - Cyan
                        val salesHeight =
                            ((item.value / maxVal) * (canvasHeight - 10f)).toFloat()
                        val salesLeft = groupCenterX - barWidth - 2f
                        val salesTop = canvasHeight - salesHeight

                        drawRoundRect(
                            color = salesColor,
                            topLeft = Offset(salesLeft, salesTop),
                            size = Size(barWidth, salesHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Profit bar - Violet
                        val profitHeight =
                            ((item.secondaryValue / maxVal) * (canvasHeight - 10f)).toFloat()
                        val profitLeft = groupCenterX + 2f
                        val profitTop = canvasHeight - profitHeight

                        drawRoundRect(
                            color = profitColor,
                            topLeft = Offset(profitLeft, profitTop),
                            size = Size(barWidth, profitHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                // Month labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    data.forEach { item ->
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessComparisonCard(
    engravingMetrics: DashboardMetrics,
    sintraMetrics: DashboardMetrics,
    modifier: Modifier = Modifier,
    isProfitMasked: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("business_comparison_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Business Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            val totalNet = engravingMetrics.netSales + sintraMetrics.netSales
            val engSalesPercent =
                if (totalNet > 0.0) {
                    engravingMetrics.netSales / totalNet
                } else {
                    0.5
                }

            // Proportion bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Engraving (${Formatters.formatPercent(engSalesPercent * 100)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EngravingBrown
                    )
                    Text(
                        text = "Sintra Board (${Formatters.formatPercent((1.0 - engSalesPercent) * 100)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SintraBoardBlue
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                engSalesPercent.toFloat().coerceIn(0.05f, 0.95f)
                            )
                            .height(10.dp)
                            .background(EngravingBrown)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(SintraBoardBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-column side-by-side stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Engraving column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            EngravingBrown.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        "Engraving",
                        fontWeight = FontWeight.Bold,
                        color = EngravingBrown,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Net: ${Formatters.formatCurrency(engravingMetrics.netSales)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Profit: ${engravingMetrics.estimatedProfit.formatCurrency(isProfitMasked)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Margin: ${if (isProfitMasked) "••••" else Formatters.formatPercent(engravingMetrics.profitMarginPercent)}",
                        fontSize = 11.sp,
                        color = SecondaryAmber
                    )
                    Text(
                        "Orders: ${engravingMetrics.ordersCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sintra Board column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            SintraBoardBlue.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        "Sintra Board",
                        fontWeight = FontWeight.Bold,
                        color = SintraBoardBlue,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Net: ${Formatters.formatCurrency(sintraMetrics.netSales)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Profit: ${sintraMetrics.estimatedProfit.formatCurrency(isProfitMasked)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Margin: ${if (isProfitMasked) "••••" else Formatters.formatPercent(sintraMetrics.profitMarginPercent)}",
                        fontSize = 11.sp,
                        color = SecondaryAmber
                    )
                    Text(
                        "Orders: ${sintraMetrics.ordersCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProductSalesBreakdownCard(
    stats: List<ProductSaleStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_sales_breakdown_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sales by Product",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "No product sales recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxSales =
                    (stats.maxOfOrNull { it.totalSales } ?: 1.0)
                        .coerceAtLeast(1.0)

                stats.take(5).forEach { stat ->
                    val progress = (stat.totalSales / maxSales).toFloat()
                    val barColor =
                        if (stat.businessType == BusinessType.ENGRAVING) {
                            EngravingBrown
                        } else {
                            SintraBoardBlue
                        }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stat.productName} (x${stat.totalQuantity})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.formatCurrency(stat.totalSales),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = barColor,
                            trackColor = barColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaidVsUnpaidCard(
    totalCollected: Double,
    outstandingReceivables: Double,
    modifier: Modifier = Modifier
) {
    val totalBillable = totalCollected + outstandingReceivables
    val collectionRate =
        if (totalBillable > 0.0) {
            (totalCollected / totalBillable) * 100.0
        } else {
            100.0
        }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("paid_vs_unpaid_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Collection Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Formatters.formatPercent(collectionRate)} Collected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (collectionRate >= 80.0) StatusPaid else StatusPending
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (collectionRate / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = StatusPaid,
                trackColor = StatusPending.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StatusPaid, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            "Collected Cash",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Formatters.formatCurrency(totalCollected),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StatusPending, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            "Outstanding Receivables",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Formatters.formatCurrency(outstandingReceivables),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StatusPending
                        )
                    }
                }
            }
        }
    }
}
