package com.amacezing.sales

import com.amacezing.sales.util.CoffeeTip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CoffeeTipCacheTest {
    private val window = CoffeeTip.CACHE_MAX_MINUTES * 60_000L

    @Test
    fun cacheKeyIsStableInsideAFiveMinuteWindowAndChangesAfterIt() {
        assertEquals(5L, CoffeeTip.CACHE_MAX_MINUTES)
        assertEquals(CoffeeTip.cacheKey(window * 10), CoffeeTip.cacheKey(window * 10 + window - 1))
        assertNotEquals(CoffeeTip.cacheKey(window * 10), CoffeeTip.cacheKey(window * 11))
    }
}
