package com.example

import com.example.model.BusinessType
import com.example.model.DiscountType
import com.example.model.PaymentStatus
import com.example.util.LineItemInput
import com.example.util.SalesCalculator
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
            businessType = BusinessType.ENGRAVING,
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
            businessType = BusinessType.SINTRA_BOARD,
            items = items,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0.0,
            amountPaid = 3000.0,
            sintraMarginPercent = 58.3333,
            sintraUseCostCalculation = false
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
            businessType = BusinessType.ENGRAVING,
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
            businessType = BusinessType.ENGRAVING,
            items = items,
            amountPaid = 0.0,
            currentStatus = PaymentStatus.CANCELLED
        )

        assertEquals(PaymentStatus.CANCELLED, result.resolvedStatus)
    }
}
