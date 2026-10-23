package com.amacezing.sales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amacezing.sales.data.AppDatabase
import com.amacezing.sales.data.SalesRepository
import com.amacezing.sales.model.BusinessProfile
import com.amacezing.sales.model.Business
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentRecord
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Quotation
import com.amacezing.sales.model.QuotationLineItem
import com.amacezing.sales.model.QuotationStatus
import com.amacezing.sales.model.QuotationWithItems
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.pdf.PdfDocumentGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream

@Implements(android.graphics.pdf.PdfDocument::class)
class ShadowPdfDocument {
    private var nativeDoc: Long = 1L

    @Implementation
    fun __constructor__() {
        try {
            val field = android.graphics.pdf.PdfDocument::class.java.getDeclaredField("mNativeDocument")
            field.isAccessible = true
            field.setLong(this, 1L)
        } catch (_: Exception) {}
    }

    @Implementation
    fun startPage(pageInfo: android.graphics.pdf.PdfDocument.PageInfo): android.graphics.pdf.PdfDocument.Page {
        val constructor = android.graphics.pdf.PdfDocument.Page::class.java.getDeclaredConstructor(
            android.graphics.Canvas::class.java,
            android.graphics.pdf.PdfDocument.PageInfo::class.java
        )
        constructor.isAccessible = true
        val bmp = android.graphics.Bitmap.createBitmap(pageInfo.pageWidth, pageInfo.pageHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        return constructor.newInstance(canvas, pageInfo)
    }

    @Implementation
    fun finishPage(page: android.graphics.pdf.PdfDocument.Page) {}

    @Implementation
    fun writeTo(out: OutputStream) {
        out.write("%PDF-1.4\n%Mock PDF document for tests\n%%EOF".toByteArray())
    }

    @Implementation
    fun close() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocument::class])
class PdfAndQuotationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: SalesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SalesRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGenerateQuotationPdf_createsValidPdfFile() {
        val quotation = Quotation(
            id = 1,
            quotationNumber = "QT-2026-0001",
            businessType = Business.ENGRAVING_ID,
            customerName = "Acme Corporation",
            customerPhone = "0917-123-4567",
            customerEmail = "acme@example.com",
            customerAddress = "123 Business St, Manila",
            quotationDateEpochMillis = System.currentTimeMillis(),
            validityDays = 7,
            validUntilEpochMillis = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10.0,
            grossTotal = 1000.0,
            discountAmount = 100.0,
            netTotal = 900.0,
            status = QuotationStatus.SENT,
            remarks = " Rush order needed by Friday"
        )
        val lineItems = listOf(
            QuotationLineItem(
                id = 1,
                quotationId = 1,
                productName = "Custom Bamboo Ballpen with Engraving",
                quantity = 20,
                unitPrice = 50.0,
                lineSubtotal = 1000.0,
                notes = "Personalized names"
            )
        )
        val quotationWithItems = QuotationWithItems(quotation, lineItems)
        val profile = BusinessProfile(
            businessName = "aMACEzing Printing Services",
            tagline = "Precision Laser Engraving & Sintra Board Printing",
            ownerName = "Reymark Aguila",
            contactNumber = "0912-345-6789",
            contactEmail = "amacezing@example.com",
            address = "Batangas City, Philippines",
            paymentInstructions = "GCash: 0912-345-6789 / BDO: 1234-5678-90",
            quotationTerms = "Valid for 7 days. 50% downpayment required."
        )

        val pdfFile = PdfDocumentGenerator.generateQuotationPdf(
            context = context,
            quotationWithItems = quotationWithItems,
            profile = profile
        )

        assertNotNull(pdfFile)
        assertTrue(pdfFile.exists())
        assertTrue("PDF file size must be greater than zero", pdfFile.length() > 0)
        assertEquals("Quotation_QT-2026-0001.pdf", pdfFile.name)

        // Verify PDF Magic Bytes (%PDF-)
        val header = ByteArray(5)
        FileInputStream(pdfFile).use { it.read(header) }
        assertEquals("%PDF-", String(header))
    }

    @Test
    fun testGenerateReceiptPdf_createsValidPdfFile() {
        val tx = SalesTransaction(
            id = "tx-123",
            orderNumber = "ENG-2026-0001",
            businessType = Business.ENGRAVING_ID,
            customerName = "Juan Dela Cruz",
            orderDateEpochMillis = System.currentTimeMillis(),
            discountType = DiscountType.PERCENTAGE,
            discountPercentage = 0.0,
            discountAmount = 0.0,
            grossSales = 1500.0,
            netSales = 1500.0,
            estimatedProfit = 600.0,
            totalProductCost = 900.0,
            paymentStatus = PaymentStatus.PAID,
            amountPaid = 1500.0,
            outstandingBalance = 0.0,
            paymentNotes = "Paid via GCash",
            remarks = "Completed and picked up"
        )
        val items = listOf(
            TransactionLineItem(
                id = 1,
                transactionId = "tx-123",
                productId = 1L,
                productName = "Bamboo Coaster (Set of 4)",
                quantity = 5,
                unitSellingPrice = 300.0,
                unitCost = 180.0,
                lineSubtotal = 1500.0
            )
        )
        val payments = listOf(
            PaymentRecord(
                id = 1,
                transactionId = "tx-123",
                amount = 1500.0,
                paymentDateEpochMillis = System.currentTimeMillis(),
                paymentMethod = "GCash",
                notes = "Ref: 123456789"
            )
        )
        val txDetails = TransactionWithDetails(tx, items, payments)
        val profile = BusinessProfile(
            businessName = "aMACEzing Printing Services",
            ownerName = "Reymark Aguila",
            contactNumber = "0912-345-6789",
            receiptFooter = "Thank you for your business!"
        )

        val receiptFile = PdfDocumentGenerator.generateReceiptPdf(
            context = context,
            transactionWithDetails = txDetails,
            profile = profile
        )

        assertNotNull(receiptFile)
        assertTrue(receiptFile.exists())
        assertTrue("Receipt file size must be greater than zero", receiptFile.length() > 0)
        assertEquals("Receipt_ENG-2026-0001.pdf", receiptFile.name)

        val header = ByteArray(5)
        FileInputStream(receiptFile).use { it.read(header) }
        assertEquals("%PDF-", String(header))
    }

    @Test
    fun testQuotationRoomPersistence_savesAndUpdatesStatus() = runBlocking {
        val quotation = Quotation(
            quotationNumber = "QT-2026-0002",
            businessType = Business.SINTRA_BOARD_ID,
            customerName = "Maria Santos",
            customerPhone = "0918-999-8888",
            grossTotal = 500.0,
            discountAmount = 0.0,
            netTotal = 500.0,
            status = QuotationStatus.DRAFT
        )
        val lineItems = listOf(
            QuotationLineItem(
                quotationId = 0,
                productName = "Sintra Board A4 3mm Matte",
                quantity = 2,
                unitPrice = 250.0,
                lineSubtotal = 500.0
            )
        )

        val savedId = repository.saveQuotation(quotation, lineItems)
        assertTrue(savedId > 0)

        val all = repository.allQuotations.first()
        assertEquals(1, all.size)
        assertEquals("QT-2026-0002", all[0].quotation.quotationNumber)
        assertEquals("Maria Santos", all[0].quotation.customerName)
        assertEquals(1, all[0].lineItems.size)
        assertEquals("Sintra Board A4 3mm Matte", all[0].lineItems[0].productName)

        // Update status to ACCEPTED
        repository.updateQuotationStatus(savedId, QuotationStatus.ACCEPTED)
        val updated = repository.getQuotationSnapshot(savedId)
        assertNotNull(updated)
        assertEquals(QuotationStatus.ACCEPTED, updated?.quotation?.status)

        // Delete quotation
        repository.deleteQuotation(savedId)
        val afterDelete = repository.allQuotations.first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testQuotationEdit_replacesDetailsAndLines_keepsNumberAndStatus() = runBlocking {
        val id = repository.saveQuotation(
            Quotation(
                quotationNumber = "QT-2026-0009",
                businessType = Business.SINTRA_BOARD_ID,
                customerName = "Old Name",
                grossTotal = 500.0,
                netTotal = 500.0,
                status = QuotationStatus.SENT
            ),
            listOf(
                QuotationLineItem(quotationId = 0, productName = "Item A", quantity = 2, unitPrice = 250.0, lineSubtotal = 500.0),
                QuotationLineItem(quotationId = 0, productName = "Item B", quantity = 1, unitPrice = 10.0, lineSubtotal = 10.0)
            )
        )
        val original = repository.getQuotationSnapshot(id)!!

        repository.updateQuotation(
            original.quotation.copy(customerName = "New Name", grossTotal = 300.0, netTotal = 300.0),
            listOf(QuotationLineItem(quotationId = 0, productName = "Item C", quantity = 3, unitPrice = 100.0, lineSubtotal = 300.0))
        )

        val edited = repository.getQuotationSnapshot(id)!!
        assertEquals("New Name", edited.quotation.customerName)
        assertEquals("QT-2026-0009", edited.quotation.quotationNumber)
        assertEquals(QuotationStatus.SENT, edited.quotation.status)
        assertEquals(1, edited.lineItems.size)
        assertEquals("Item C", edited.lineItems[0].productName)
        assertEquals(300.0, edited.quotation.netTotal, 0.001)
    }
}
