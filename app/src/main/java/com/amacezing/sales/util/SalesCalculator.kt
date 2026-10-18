package com.amacezing.sales.util

import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import kotlin.math.roundToLong

data class LineItemInput(
    val quantity: Int,
    val unitSellingPrice: Double,
    val unitCost: Double
)

data class CalculationResult(
    val grossSales: Double,
    val discountAmount: Double,
    val netSales: Double,
    val totalProductCost: Double,
    val estimatedProfit: Double,
    val estimatedMarginPercent: Double,
    val outstandingBalance: Double,
    val resolvedStatus: PaymentStatus
)

object SalesCalculator {

    const val DEFAULT_SINTRA_MARGIN_PERCENT = 58.3333

    fun roundTo2Decimals(value: Double): Double {
        return (value * 100.0).roundToLong() / 100.0
    }

    fun calculate(
        businessType: BusinessType,
        items: List<LineItemInput>,
        discountType: DiscountType = DiscountType.PERCENTAGE,
        discountValue: Double = 0.0,
        amountPaid: Double = 0.0,
        currentStatus: PaymentStatus = PaymentStatus.PENDING,
        sintraMarginPercent: Double = DEFAULT_SINTRA_MARGIN_PERCENT,
        sintraUseCostCalculation: Boolean = false
    ): CalculationResult {
        val grossSales = roundTo2Decimals(items.sumOf { it.quantity * it.unitSellingPrice })
        val totalProductCost = roundTo2Decimals(items.sumOf { it.quantity * it.unitCost })

        val discountAmount = when (discountType) {
            DiscountType.PERCENTAGE -> {
                val clamped = discountValue.coerceIn(0.0, 100.0)
                roundTo2Decimals(grossSales * (clamped / 100.0))
            }
            DiscountType.FIXED -> {
                val clamped = discountValue.coerceAtLeast(0.0)
                minOf(clamped, grossSales)
            }
        }

        val netSales = roundTo2Decimals((grossSales - discountAmount).coerceAtLeast(0.0))

        val estimatedProfit = when (businessType) {
            BusinessType.ENGRAVING -> {
                roundTo2Decimals(netSales - totalProductCost)
            }
            BusinessType.SINTRA_BOARD -> {
                if (sintraUseCostCalculation && totalProductCost > 0.0) {
                    roundTo2Decimals(netSales - totalProductCost)
                } else {
                    val marginMultiplier = sintraMarginPercent / 100.0
                    roundTo2Decimals(netSales * marginMultiplier)
                }
            }
        }

        val estimatedMarginPercent = if (netSales > 0.0) {
            roundTo2Decimals((estimatedProfit / netSales) * 100.0)
        } else {
            0.0
        }

        val clampedPaid = amountPaid.coerceAtLeast(0.0)
        val outstandingBalance = roundTo2Decimals((netSales - clampedPaid).coerceAtLeast(0.0))

        val resolvedStatus = if (currentStatus == PaymentStatus.CANCELLED) {
            PaymentStatus.CANCELLED
        } else if (clampedPaid >= netSales && netSales > 0.0) {
            PaymentStatus.PAID
        } else if (clampedPaid > 0.0) {
            PaymentStatus.PARTIALLY_PAID
        } else {
            PaymentStatus.PENDING
        }

        return CalculationResult(
            grossSales = grossSales,
            discountAmount = discountAmount,
            netSales = netSales,
            totalProductCost = totalProductCost,
            estimatedProfit = estimatedProfit,
            estimatedMarginPercent = estimatedMarginPercent,
            outstandingBalance = outstandingBalance,
            resolvedStatus = resolvedStatus
        )
    }
}
