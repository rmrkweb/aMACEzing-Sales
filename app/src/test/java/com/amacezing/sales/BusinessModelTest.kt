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

class BusinessCloudMappingTest {
    private val mapper = FirestoreMappers

    @Test
    fun roundTripKeepsEveryField() {
        val original = Business(
            id = "INVITATION_PRINTING", name = "Invitation Printing", orderPrefix = "INV",
            colorHex = "#22C55E", profitMethod = ProfitMethod.FIXED_MARGIN, marginPercent = 42.5,
            useUnitCostIfAvailable = true, isActive = false, sortOrder = 3
        )
        val cloud = mapper.run { original.toFirestore("uid-1") }
        assertEquals("invitation_printing", cloud.id)
        assertEquals("INVITATION_PRINTING", cloud.type)
        assertEquals(original, mapper.run { cloud.toRoomBusiness(fallbackSortOrder = 9) })
    }

    @Test
    fun legacyCloudDocumentsFallBackToOriginalDefaults() {
        val legacy = com.amacezing.sales.data.firebase.FirestoreBusiness(
            id = "sintra_board", userId = "uid-1", name = "Sintra Board Printing Business",
            type = "SINTRA_BOARD", orderPrefix = "snt"
        )
        val room = mapper.run { legacy.toRoomBusiness(fallbackSortOrder = 5) }
        assertEquals("SINTRA_BOARD", room.id)
        assertEquals("SNT", room.orderPrefix)
        assertEquals(ProfitMethod.FIXED_MARGIN, room.profitMethod)
        assertEquals(58.3333, room.marginPercent, 0.0001)
        assertEquals(1, room.sortOrder)
        assertTrue(room.isActive)
    }
}
