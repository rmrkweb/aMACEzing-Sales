package com.amacezing.sales

import com.amacezing.sales.util.ProductImportEngine
import com.amacezing.sales.util.ProductImportTemplate
import com.amacezing.sales.util.ProductInputRules
import com.amacezing.sales.util.ProductRef
import com.amacezing.sales.util.TabularReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductImportEngineTest {

    private val header = ProductImportTemplate.HEADERS
    private fun row(vararg v: String) = v.toList()
    private fun validate(vararg rows: List<String>, existing: List<ProductRef> = emptyList(), business: String = "ENGRAVING") =
        ProductImportEngine.validate(listOf(header) + rows.toList(), business, existing)

    @Test
    fun templateHeaderIsReadableAndAValidRowParses() {
        val plan = validate(row("Guest Book", "350", "120", "Yes"))
        assertNull(plan.fatalProblem)
        assertEquals(1, plan.rows.size)
        val p = plan.rows[0]
        assertEquals("Guest Book", p.name)
        assertEquals(350.0, p.sellingPrice, 0.001)
        assertEquals(120.0, p.unitCost, 0.001)
        assertTrue(p.isActive)
        assertNull(p.existingId)
    }

    @Test
    fun missingRequiredColumnsAreReported() {
        val plan = ProductImportEngine.validate(listOf(listOf("Unit Cost")), "ENGRAVING", emptyList())
        assertNotNull(plan.fatalProblem)
        assertTrue(plan.fatalProblem!!.contains("Product Name"))
        assertTrue(plan.fatalProblem!!.contains("Selling Price"))
    }

    @Test
    fun usesTheSameRulesAndMessagesAsTheProductDialog() {
        val plan = validate(
            row("", "10", "", ""),          // blank name
            row("Mug", "abc", "", ""),      // unreadable price
            row("Cup", "-1", "", ""),       // negative price
            row("Pen", "", "", "")          // blank price
        )
        assertTrue(plan.rows.isEmpty())
        assertEquals(ProductInputRules.NAME_REQUIRED, plan.issues.first { it.row == 2 }.message)
        listOf(3, 4, 5).forEach { r ->
            assertEquals(ProductInputRules.SELLING_PRICE_INVALID, plan.issues.first { it.row == r }.message)
        }
    }

    @Test
    fun blankCostIsZeroAndPesoFormattingIsAccepted() {
        val plan = validate(row("Frame", "₱1,200.50", "", ""))
        assertEquals(1200.5, plan.rows[0].sellingPrice, 0.001)
        assertEquals(0.0, plan.rows[0].unitCost, 0.001)
    }

    @Test
    fun unreadableOrNegativeCostIsReportedInsteadOfBecomingZero() {
        val plan = validate(row("A", "10", "abc", ""), row("B", "10", "-5", ""))
        assertTrue(plan.rows.isEmpty())
        assertEquals(2, plan.invalidRowCount)
    }

    @Test
    fun activeColumnAcceptsYesNoAndRejectsOtherText() {
        val plan = validate(
            row("A", "1", "", "No"), row("B", "1", "", ""), row("C", "1", "", "TRUE"), row("D", "1", "", "maybe")
        )
        assertEquals(listOf(false, true, true), plan.rows.map { it.isActive })
        assertEquals(1, plan.issues.size)
        assertEquals(5, plan.issues[0].row)
    }

    @Test
    fun repeatedNamesInTheFileAreFlaggedCaseInsensitively() {
        val plan = validate(row("Guest Book", "1", "", ""), row("guest book ", "2", "", ""))
        assertEquals(1, plan.rows.size)
        assertEquals(1, plan.issues.size)
        assertEquals(3, plan.issues[0].row)
    }

    @Test
    fun existingProductsAreMatchedWithinTheChosenBusinessOnly() {
        val existing = listOf(
            ProductRef(7L, "ENGRAVING", "Guest Book", 10.0),
            ProductRef(8L, "SINTRA_BOARD", "Frame", 5.0)
        )
        val plan = validate(row("guest book", "99", "", ""), row("Frame", "20", "", ""), existing = existing)
        assertEquals(7L, plan.rows[0].existingId)
        assertNull(plan.rows[1].existingId)
        assertEquals(1, plan.newCount)
        assertEquals(1, plan.existingCount)
        assertEquals(1, plan.rowsToSave(updateExisting = false).size)
        assertEquals(2, plan.rowsToSave(updateExisting = true).size)
    }

    @Test
    fun blankRowsAreIgnoredAndTemplateFilesAreReadable() {
        val plan = validate(row("", "", "", ""), row("Pen", "5", "", ""))
        assertEquals(1, plan.rows.size)
        assertTrue(plan.issues.isEmpty())
        assertEquals(ProductImportTemplate.HEADERS, TabularReader.read("t.csv", ProductImportTemplate.csv().toByteArray())[0])
        assertEquals(ProductImportTemplate.HEADERS, TabularReader.read("t.xlsx", ProductImportTemplate.xlsx())[0])
        assertFalse(ProductInputRules.validSellingPrice(-0.01) != null)
    }
}
