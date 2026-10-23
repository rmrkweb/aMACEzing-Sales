package com.amacezing.sales

import com.amacezing.sales.ui.components.QuotationItemState
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotationItemStateTest {
    @Test
    fun clearedQuantityIsZeroNotOne() {
        val item = QuotationItemState(quantityText = "", unitPriceText = "40")
        assertEquals(0, item.quantity)
        assertEquals(0.0, item.subtotal, 0.001)
    }

    @Test
    fun typedValuesDriveSubtotal() {
        val item = QuotationItemState(quantityText = "200", unitPriceText = "40.5")
        assertEquals(200, item.quantity)
        assertEquals(8100.0, item.subtotal, 0.001)
    }

    @Test
    fun blankPriceCountsAsZero() {
        assertEquals(0.0, QuotationItemState(unitPriceText = "").unitPrice, 0.0)
    }
}
