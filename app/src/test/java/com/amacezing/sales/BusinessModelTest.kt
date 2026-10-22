package com.amacezing.sales

import com.amacezing.sales.data.firebase.FirestoreMappers
import com.amacezing.sales.model.Business
import com.amacezing.sales.model.ProfitMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessModelTest {

    @Test
    fun legacyBusinessIdsAreUnchanged() {
        assertEquals("ENGRAVING", Business.ENGRAVING_ID)
        assertEquals("SINTRA_BOARD", Business.SINTRA_BOARD_ID)
    }

    @Test
    fun defaultsKeepTheOriginalProfitRules() {
        val engraving = Business.DEFAULTS.first { it.id == Business.ENGRAVING_ID }
        val sintra = Business.DEFAULTS.first { it.id == Business.SINTRA_BOARD_ID }
        assertEquals(ProfitMethod.UNIT_COST, engraving.profitMethod)
        assertEquals(ProfitMethod.FIXED_MARGIN, sintra.profitMethod)
        assertEquals(58.3333, sintra.marginPercent, 0.0001)
        assertTrue(!sintra.useUnitCostIfAvailable)
    }

    @Test
    fun fallbackNameIsReadable() {
        assertEquals("Engraving", Business.fallbackName("ENGRAVING"))
        assertEquals("Sintra Board", Business.fallbackName("SINTRA_BOARD"))
        assertEquals("Invitation Printing", Business.fallbackName("INVITATION_PRINTING"))
    }

    @Test
    fun parseBusinessTypeNormalisesAndPreservesCustomIds() {
        assertEquals("ENGRAVING", FirestoreMappers.parseBusinessType(null))
        assertEquals("ENGRAVING", FirestoreMappers.parseBusinessType(" "))
        assertEquals("SINTRA_BOARD", FirestoreMappers.parseBusinessType("sintra_board"))
        assertEquals("SINTRA_BOARD", FirestoreMappers.parseBusinessType("Sintra Board"))
        assertEquals("INVITATION_PRINTING", FirestoreMappers.parseBusinessType("Invitation Printing"))
    }
}
