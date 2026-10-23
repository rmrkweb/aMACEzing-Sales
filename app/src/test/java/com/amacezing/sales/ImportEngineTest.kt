package com.amacezing.sales

import com.amacezing.sales.model.Business
import com.amacezing.sales.model.ProfitMethod
import com.amacezing.sales.util.ExistingOrderRef
import com.amacezing.sales.util.ImportContext
import com.amacezing.sales.util.ImportEngine
import com.amacezing.sales.util.ImportTemplate
import com.amacezing.sales.util.ProductRef
import com.amacezing.sales.util.SimpleXlsx
import com.amacezing.sales.util.TabularReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportEngineTest {

    private val invitation = Business(
        id = "INVITATION_PRINTING", name = "Invitation Printing", orderPrefix = "INV", colorHex = "#22C55E",
        profitMethod = ProfitMethod.UNIT_COST, marginPercent = 0.0, useUnitCostIfAvailable = false,
        isActive = true, sortOrder = 0
    )
    private val archived = invitation.copy(id = "OLD", name = "Old Shop", isActive = false)
    private val ctx = ImportContext(
        businesses = listOf(invitation, archived),
        products = listOf(ProductRef(5L, "INVITATION_PRINTING", "Wedding Invite", 12.0)),
        existingOrders = emptyList()
    )
    private val header = ImportTemplate.HEADERS

    private fun row(vararg v: String) = v.toList()

    @Test
    fun templateHeaderIsReadableByTheValidator() {
        val plan = ImportEngine.validate(listOf(header, row("", "2026-10-23", "Ana", "Invitation Printing", "Wedding Invite", "10", "50", "", "", "", "", "")), ctx)
        assertNull(plan.fatalProblem)
        assertEquals(1, plan.orders.size)
        assertEquals(500.0, plan.orders[0].net, 0.001)
        assertEquals(5L, plan.orders[0].lines[0].productId)
    }

    @Test
    fun missingRequiredColumnsAreReported() {
        val plan = ImportEngine.validate(listOf(listOf("Customer", "Product")), ctx)
        assertNotNull(plan.fatalProblem)
        assertTrue(plan.fatalProblem!!.contains("Order Date"))
        assertTrue(plan.fatalProblem!!.contains("Quantity"))
    }

    @Test
    fun sameOrderRefGroupsLinesAndAppliesDiscountAndPayment() {
        val rows = listOf(
            header,
            row("A1", "2026-10-23", "Ana", "Invitation Printing", "Wedding Invite", "100", "10", "", "20", "300", "GCash", "Rush"),
            row("A1", "2026-10-23", "ana", "invitation printing", "Envelope", "100", "2", "", "", "", "", "")
        )
        val plan = ImportEngine.validate(rows, ctx)
        assertTrue(plan.issues.isEmpty())
        assertEquals(1, plan.orders.size)
        val o = plan.orders[0]
        assertEquals(2, o.lines.size)
        assertEquals(1200.0, o.gross, 0.001)
        assertEquals(960.0, o.net, 0.001)
        assertEquals(300.0, o.amountPaid, 0.001)
        assertEquals("GCash", o.paymentMethod)
        assertEquals("Rush", o.remarks)
    }

    @Test
    fun conflictingRowsInOneOrderAreRejected() {
        val rows = listOf(
            header,
            row("A1", "2026-10-23", "Ana", "Invitation Printing", "X", "1", "10", "", "", "", "", ""),
            row("A1", "2026-10-23", "Ben", "Invitation Printing", "Y", "1", "10", "", "", "", "", "")
        )
        val plan = ImportEngine.validate(rows, ctx)
        assertTrue(plan.orders.isEmpty())
        assertEquals(1, plan.issues.size)
        assertEquals(3, plan.issues[0].row)
    }

    @Test
    fun badValuesProduceRowSpecificIssues() {
        val rows = listOf(
            header,
            row("", "not a date", "Ana", "Invitation Printing", "X", "1", "10", "", "", "", "", ""),
            row("", "2026-10-23", "", "Invitation Printing", "X", "1", "10", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Unknown Biz", "X", "1", "10", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Old Shop", "X", "1", "10", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Invitation Printing", "X", "0", "10", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Invitation Printing", "X", "1.5", "10", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Invitation Printing", "X", "1", "abc", "", "", "", "", ""),
            row("", "2026-10-23", "Ana", "Invitation Printing", "X", "1", "10", "", "150", "", "", ""),
            row("", "2026-10-23", "Ana", "Invitation Printing", "X", "1", "10", "", "", "999", "", "")
        )
        val plan = ImportEngine.validate(rows, ctx)
        assertTrue(plan.orders.isEmpty())
        assertEquals((2..10).toList(), plan.issues.map { it.row })
    }

    @Test
    fun currencySymbolsAndThousandsSeparatorsAreAccepted() {
        assertEquals(1234.5, ImportEngine.parseNumber("₱1,234.50")!!, 0.001)
        assertNull(ImportEngine.parseNumber("abc"))
        assertNull(ImportEngine.parseNumber(""))
    }

    @Test
    fun datesInSeveralFormatsAndExcelSerialsParse() {
        val iso = ImportEngine.parseDate("2026-10-23")
        assertNotNull(iso)
        assertEquals(iso, ImportEngine.parseDate("23/10/2026"))
        assertEquals(iso, ImportEngine.parseDate("23 Oct 2026"))
        // 2026-10-23 as an Excel serial number
        assertEquals(iso, ImportEngine.parseDate("46318"))
        assertNull(ImportEngine.parseDate("32/13/2026"))
    }

    @Test
    fun possibleDuplicatesAreFlagged() {
        val date = ImportEngine.parseDate("2026-10-23")!!
        val withExisting = ctx.copy(existingOrders = listOf(ExistingOrderRef("INVITATION_PRINTING", "ANA", date, 500.0)))
        val plan = ImportEngine.validate(
            listOf(header, row("", "2026-10-23", "Ana", "Invitation Printing", "Wedding Invite", "10", "50", "", "", "", "", "")),
            withExisting
        )
        assertTrue(plan.orders[0].isDuplicate)
        assertEquals(1, plan.duplicateCount)
    }

    @Test
    fun csvParserHandlesQuotesNewlinesBomAndSemicolons() {
        val csv = "\uFEFFOrder Date;Customer;Remarks\r\n2026-10-23;\"Cruz; Ana\";\"Line1\nLine2 \"\"quoted\"\"\"\r\n\r\n"
        val rows = TabularReader.parseCsv(csv)
        assertEquals(2, rows.size)
        assertEquals(listOf("Order Date", "Customer", "Remarks"), rows[0])
        assertEquals("Cruz; Ana", rows[1][1])
        assertEquals("Line1\nLine2 \"quoted\"", rows[1][2])
    }

    @Test
    fun xlsxWriteThenReadRoundTrips() {
        val data = listOf(header, row("R1", "2026-10-23", "Ana & Ben <Co>", "Invitation Printing", "Invite", "3", "10.5", "", "", "", "", "note"))
        val back = TabularReader.read("template.xlsx", SimpleXlsx.write(data))
        assertEquals(data[0], back[0])
        assertEquals("Ana & Ben <Co>", back[1][2])
        assertEquals("10.5", back[1][6])
    }

    @Test
    fun templateFilesAreReadable() {
        val csvRows = TabularReader.read("t.csv", ImportTemplate.csv().toByteArray())
        assertEquals(1, csvRows.size)
        assertEquals(ImportTemplate.HEADERS, csvRows[0])
        val xlsxRows = TabularReader.read("t.xlsx", ImportTemplate.xlsx())
        assertEquals(ImportTemplate.HEADERS, xlsxRows[0])
    }

    @Test
    fun invalidFilesGiveReadableErrors() {
        assertFalse(runCatching { TabularReader.read("a.xls", byteArrayOf(1, 2, 3)) }.exceptionOrNull()?.message.isNullOrBlank())
        assertFalse(runCatching { TabularReader.read("a.csv", ByteArray(0)) }.exceptionOrNull()?.message.isNullOrBlank())
        assertFalse(runCatching { TabularReader.read("a.xlsx", "not a zip".toByteArray()) }.exceptionOrNull()?.message.isNullOrBlank())
    }
}
