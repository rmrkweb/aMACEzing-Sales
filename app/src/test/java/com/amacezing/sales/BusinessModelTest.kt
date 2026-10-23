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

class ConvertedQuotationProfitTest {
    @Test
    fun zeroCostGivesFullProfitWhichIsTheBugWeFixed() {
        val rule = Business.DEFAULTS.first { it.id == Business.ENGRAVING_ID }.toProfitRule()
        val zero = com.amacezing.sales.util.SalesCalculator.calculateProfit(rule, 9200.0, 0.0)
        val withCost = com.amacezing.sales.util.SalesCalculator.calculateProfit(rule, 9200.0, 200 * 20.0 + 50 * 35.0)
        assertEquals(9200.0, zero, 0.001)
        assertEquals(3450.0, withCost, 0.001)
    }
}

class QuotationCloudMappingTest {
    @Test
    fun quotationRoundTripKeepsEverything() {
        val original = com.amacezing.sales.model.QuotationWithItems(
            quotation = com.amacezing.sales.model.Quotation(
                id = 7,
                quotationNumber = "QT-2026-0004",
                businessType = "INVITATION_PRINTING",
                customerName = "Ana Cruz",
                customerPhone = "0917",
                quotationDateEpochMillis = 1_700_000_000_000L,
                validityDays = 10,
                validUntilEpochMillis = 1_700_864_000_000L,
                discountType = com.amacezing.sales.model.DiscountType.FIXED,
                discountValue = 50.0,
                grossTotal = 500.0,
                discountAmount = 50.0,
                netTotal = 450.0,
                status = com.amacezing.sales.model.QuotationStatus.ACCEPTED,
                remarks = "Rush",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L
            ),
            lineItems = listOf(
                com.amacezing.sales.model.QuotationLineItem(
                    id = 3, quotationId = 7, productId = null, productName = "Invitation",
                    quantity = 50, unitPrice = 10.0, lineSubtotal = 500.0, notes = "Gold foil"
                )
            )
        )
        val cloud = FirestoreMappers.run { original.toFirestore("uid-1") }
        assertEquals("QT-2026-0004", cloud.id)
        val (room, lines) = FirestoreMappers.run { cloud.toRoomQuotation() }
        assertEquals(original.quotation.copy(id = 0), room)
        assertEquals(1, lines.size)
        assertEquals(original.lineItems[0].copy(id = 0, quotationId = 0), lines[0])
    }

    @Test
    fun unknownStatusFallsBackToDraft() {
        val cloud = com.amacezing.sales.data.firebase.FirestoreQuotation(
            id = "QT-2026-0001", quotationNumber = "QT-2026-0001", businessType = "ENGRAVING",
            customerName = "X", status = "SOMETHING_NEW"
        )
        val (room, _) = FirestoreMappers.run { cloud.toRoomQuotation() }
        assertEquals(com.amacezing.sales.model.QuotationStatus.DRAFT, room.status)
    }
}
