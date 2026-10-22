package com.amacezing.sales

import com.amacezing.sales.model.Business
import com.amacezing.sales.model.ProfitMethod
import com.amacezing.sales.model.ProfitRule
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.util.LineItemInput
import com.amacezing.sales.util.SalesCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesCalculatorTest {

    @Test
    fun testEngravingCalculations() {
        val items = listOf(
            LineItemInput(quantity = 10, unitSellingPrice = 70.0, unitCost = 35.0),  // 700 gross, 350 cost
            LineItemInput(quantity = 20, unitSellingPrice = 40.0, unitCost = 20.0)   // 800 gross, 400 cost
        )

        val result = SalesCalculator.calculate(
            profitRule = Business.DEFAULTS.first { it.id == Business.ENGRAVING_ID }.toProfitRule(),
            items = items,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 5.0, // 5% discount
            amountPaid = 0.0
        )

        assertEquals(1500.0, result.grossSales, 0.01)
        assertEquals(75.0, result.discountAmount, 0.01)
        assertEquals(1425.0, result.netSales, 0.01)
        assertEquals(750.0, result.totalProductCost, 0.01)
        assertEquals(675.0, result.estimatedProfit, 0.01)
        assertEquals(1425.0, result.outstandingBalance, 0.01)
        assertEquals(PaymentStatus.PENDING, result.resolvedStatus)
    }

    @Test
    fun testSintraBoardMarginCalculations() {
        val items = listOf(
            LineItemInput(quantity = 10, unitSellingPrice = 150.0, unitCost = 62.5), // A4: 1500 gross
            LineItemInput(quantity = 5, unitSellingPrice = 300.0, unitCost = 125.0)  // A3: 1500 gross
        )

        val result = SalesCalculator.calculate(
            profitRule = Business.DEFAULTS.first { it.id == Business.SINTRA_BOARD_ID }.toProfitRule(),
            items = items,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0.0,
            amountPaid = 3000.0
        )

        assertEquals(3000.0, result.grossSales, 0.01)
        assertEquals(0.0, result.discountAmount, 0.01)
        assertEquals(3000.0, result.netSales, 0.01)
        // 3000 * 58.3333% = 1750.00
        assertEquals(1750.0, result.estimatedProfit, 0.05)
        assertEquals(0.0, result.outstandingBalance, 0.01)
        assertEquals(PaymentStatus.PAID, result.resolvedStatus)
    }

    @Test
    fun testPartialPaymentAndBalance() {
        val items = listOf(
            LineItemInput(quantity = 1, unitSellingPrice = 1000.0, unitCost = 500.0)
        )

        val result = SalesCalculator.calculate(
            profitRule = Business.DEFAULTS.first { it.id == Business.ENGRAVING_ID }.toProfitRule(),
            items = items,
            discountType = DiscountType.FIXED,
            discountValue = 100.0,
            amountPaid = 400.0
        )

        assertEquals(1000.0, result.grossSales, 0.01)
        assertEquals(100.0, result.discountAmount, 0.01)
        assertEquals(900.0, result.netSales, 0.01)
        assertEquals(500.0, result.outstandingBalance, 0.01)
        assertEquals(PaymentStatus.PARTIALLY_PAID, result.resolvedStatus)
    }

    @Test
    fun testCancelledStatusPreserved() {
        val items = listOf(
            LineItemInput(quantity = 2, unitSellingPrice = 200.0, unitCost = 100.0)
        )

        val result = SalesCalculator.calculate(
            profitRule = Business.DEFAULTS.first { it.id == Business.ENGRAVING_ID }.toProfitRule(),
            items = items,
            amountPaid = 0.0,
            currentStatus = PaymentStatus.CANCELLED
        )

        assertEquals(PaymentStatus.CANCELLED, result.resolvedStatus)
    }

    @Test
    fun testFixedMarginIgnoresCostByDefault() {
        val rule = ProfitRule(ProfitMethod.FIXED_MARGIN, 40.0, useUnitCostIfAvailable = false)
        assertEquals(400.0, SalesCalculator.calculateProfit(rule, netSales = 1000.0, totalProductCost = 900.0), 0.001)
    }

    @Test
    fun testFixedMarginUsesCostWhenOptedInAndAvailable() {
        val rule = ProfitRule(ProfitMethod.FIXED_MARGIN, 40.0, useUnitCostIfAvailable = true)
        assertEquals(300.0, SalesCalculator.calculateProfit(rule, netSales = 1000.0, totalProductCost = 700.0), 0.001)
        // No cost recorded: falls back to the margin, exactly like the former Sintra rule.
        assertEquals(400.0, SalesCalculator.calculateProfit(rule, netSales = 1000.0, totalProductCost = 0.0), 0.001)
    }

    @Test
    fun testNewBusinessUsesItsOwnRule() {
        val invitation = ProfitRule(ProfitMethod.FIXED_MARGIN, 25.0)
        val result = SalesCalculator.calculate(
            profitRule = invitation,
            items = listOf(LineItemInput(quantity = 100, unitSellingPrice = 20.0, unitCost = 5.0))
        )
        assertEquals(2000.0, result.netSales, 0.001)
        assertEquals(500.0, result.estimatedProfit, 0.001)
    }
}
