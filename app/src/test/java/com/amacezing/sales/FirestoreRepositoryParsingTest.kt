package com.amacezing.sales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amacezing.sales.data.AppDatabase
import com.amacezing.sales.data.SalesRepository
import com.amacezing.sales.data.firebase.FirestoreCustomer
import com.amacezing.sales.data.firebase.FirestoreMappers.toFirestore
import com.amacezing.sales.data.firebase.FirestoreMappers.toRoomCustomer
import com.amacezing.sales.data.firebase.FirestoreMappers.toRoomProduct
import com.amacezing.sales.data.firebase.FirestoreMappers.toRoomSettings
import com.amacezing.sales.data.firebase.FirestoreMappers.toRoomTransaction
import com.amacezing.sales.data.firebase.FirestoreProduct
import com.amacezing.sales.data.firebase.FirestoreSettings
import com.amacezing.sales.data.firebase.FirestoreTransaction
import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
import com.amacezing.sales.util.LineItemInput
import com.amacezing.sales.util.SalesCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestoreRepositoryParsingTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SalesRepository
    private val testUserId = "user_test_123"

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SalesRepository(database)
        AppDatabase.populateInitialData(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // PRODUCT PARSING & EDGE CASE TESTS
    // =========================================================================

    @Test
    fun testProduct_validRoundtripConversion() = runBlocking {
        val roomProduct = Product(
            id = 42L,
            name = "Wooden Keychain",
            businessType = BusinessType.ENGRAVING,
            sellingPrice = 85.0,
            unitCost = 40.0,
            isActive = true
        )

        val firestoreModel = roomProduct.toFirestore(testUserId)
        assertEquals("42", firestoreModel.id)
        assertEquals(testUserId, firestoreModel.userId)
        assertEquals("Wooden Keychain", firestoreModel.name)
        assertEquals("ENGRAVING", firestoreModel.businessType)
        assertEquals(85.0, firestoreModel.sellingPrice, 0.001)
        assertEquals(40.0, firestoreModel.unitCost, 0.001)
        assertTrue(firestoreModel.isActive)

        val parsedRoomProduct = firestoreModel.toRoomProduct()
        assertEquals(roomProduct.id, parsedRoomProduct.id)
        assertEquals(roomProduct.name, parsedRoomProduct.name)
        assertEquals(roomProduct.businessType, parsedRoomProduct.businessType)
        assertEquals(roomProduct.sellingPrice, parsedRoomProduct.sellingPrice, 0.001)
        assertEquals(roomProduct.unitCost, parsedRoomProduct.unitCost, 0.001)
        assertEquals(roomProduct.isActive, parsedRoomProduct.isActive)
    }

    @Test
    fun testProduct_handlesNegativePricesAndNaNGracefully() {
        val corruptedFirestoreProduct = FirestoreProduct(
            id = "101",
            userId = testUserId,
            name = "Defective Coaster",
            businessType = "ENGRAVING",
            sellingPrice = -50.0,
            unitCost = Double.NaN,
            isActive = false
        )

        val parsed = corruptedFirestoreProduct.toRoomProduct()
        assertEquals(101L, parsed.id)
        assertEquals("Defective Coaster", parsed.name)
        assertEquals(0.0, parsed.sellingPrice, 0.001)
        assertEquals(0.0, parsed.unitCost, 0.001)
        assertFalse(parsed.isActive)
    }

    @Test
    fun testProduct_nonNumericDocumentId_fallsBackToZeroWithoutCrashing() {
        val firestoreProductWithDocId = FirestoreProduct(
            id = "doc_engraving_987xyz",
            userId = testUserId,
            name = "Acrylic Trophy",
            businessType = "ENGRAVING",
            sellingPrice = 350.0,
            unitCost = 150.0
        )

        val parsed = firestoreProductWithDocId.toRoomProduct()
        assertEquals(0L, parsed.id)
        assertEquals("Acrylic Trophy", parsed.name)
    }

    @Test
    fun testProduct_caseInsensitiveBusinessTypeParsing() {
        val lowercaseSintra = FirestoreProduct(
            id = "5",
            userId = testUserId,
            name = "3mm Sintra Board A4",
            businessType = "sintra_board"
        )
        assertEquals(BusinessType.SINTRA_BOARD, lowercaseSintra.toRoomProduct().businessType)

        val unknownType = FirestoreProduct(
            id = "6",
            userId = testUserId,
            name = "Laser Cut Acrylic",
            businessType = "UNKNOWN_CUSTOM_SERVICE"
        )
        assertEquals(BusinessType.ENGRAVING, unknownType.toRoomProduct().businessType)
    }

    @Test
    fun testProduct_blankName_defaultsToUnnamed() {
        val emptyNameProduct = FirestoreProduct(
            id = "7",
            userId = testUserId,
            name = "   "
        )
        assertEquals("Unnamed Product", emptyNameProduct.toRoomProduct().name)
    }

    // =========================================================================
    // CUSTOMER PARSING & ROOM INTEGRITY TESTS
    // =========================================================================

    @Test
    fun testCustomer_validRoundtripConversion() {
        val roomCustomer = Customer(
            id = 15L,
            name = "Juan Dela Cruz",
            phone = "+63 917 123 4567",
            email = "juan@example.com",
            notes = "Prefers rush engraving"
        )

        val firestoreModel = roomCustomer.toFirestore(testUserId)
        assertEquals("15", firestoreModel.id)
        assertEquals(testUserId, firestoreModel.userId)
        assertEquals("Juan Dela Cruz", firestoreModel.name)
        assertEquals("+63 917 123 4567", firestoreModel.phone)
        assertEquals("juan@example.com", firestoreModel.email)
        assertEquals("Prefers rush engraving", firestoreModel.notes)

        val parsedCustomer = firestoreModel.toRoomCustomer()
        assertEquals(roomCustomer.id, parsedCustomer.id)
        assertEquals(roomCustomer.name, parsedCustomer.name)
        assertEquals(roomCustomer.phone, parsedCustomer.phone)
        assertEquals(roomCustomer.email, parsedCustomer.email)
        assertEquals(roomCustomer.notes, parsedCustomer.notes)
    }

    @Test
    fun testCustomer_blankName_defaultsToWalkIn() {
        val blankCustomer = FirestoreCustomer(
            id = "doc_cust_001",
            userId = testUserId,
            name = "   ",
            phone = " 09181234567 "
        )

        val parsed = blankCustomer.toRoomCustomer()
        assertEquals(0L, parsed.id)
        assertEquals("Walk-in Customer", parsed.name)
        assertEquals("09181234567", parsed.phone)
    }

    @Test
    fun testCustomer_roomPersistenceAndSyncIntegrity() = runBlocking {
        // Save via repository
        val created = repository.getOrCreateCustomer("  Reymark Aguila  ")
        assertTrue(created.id > 0)
        assertEquals("Reymark Aguila", created.name)

        // Convert to Firestore model
        val firestoreCustomer = created.toFirestore(testUserId)
        assertEquals(created.id.toString(), firestoreCustomer.id)
        assertEquals("Reymark Aguila", firestoreCustomer.name)

        // Verify retrieval from Room DAO matches
        val customersInDb = repository.allCustomers.first()
        assertTrue(customersInDb.any { it.name == "Reymark Aguila" })
    }

    // =========================================================================
    // TRANSACTION PARSING & EDGE CASE TESTS
    // =========================================================================

    @Test
    fun testTransaction_validRoundtripConversion() {
        val txId = UUID.randomUUID().toString()
        val originalTx = SalesTransaction(
            id = txId,
            orderNumber = "ENG-2026-0001",
            businessType = BusinessType.ENGRAVING,
            customerId = 10L,
            customerName = "Apex Corporate",
            orderDateEpochMillis = 1776384000000L,
            discountType = DiscountType.PERCENTAGE,
            discountPercentage = 10.0,
            discountAmount = 150.0,
            grossSales = 1500.0,
            netSales = 1350.0,
            totalProductCost = 700.0,
            estimatedProfit = 650.0,
            paymentStatus = PaymentStatus.PARTIALLY_PAID,
            amountPaid = 500.0,
            outstandingBalance = 850.0,
            paymentNotes = "Deposit via GCash",
            remarks = "Deliver by Friday",
            createdAt = 1776384000000L,
            updatedAt = 1776384000000L
        )

        val firestoreModel = originalTx.toFirestore(testUserId, "Wooden Keychain (x20)")
        assertEquals(txId, firestoreModel.id)
        assertEquals(testUserId, firestoreModel.userId)
        assertEquals("ENG-2026-0001", firestoreModel.orderNumber)
        assertEquals("ENGRAVING", firestoreModel.businessType)
        assertEquals(1350.0, firestoreModel.netSales, 0.001)
        assertEquals("PARTIALLY_PAID", firestoreModel.paymentStatus)
        assertEquals("Wooden Keychain (x20)", firestoreModel.lineItemsSummary)

        val parsedBack = firestoreModel.toRoomTransaction(customerId = 10L)
        assertEquals(originalTx.id, parsedBack.id)
        assertEquals(originalTx.orderNumber, parsedBack.orderNumber)
        assertEquals(originalTx.businessType, parsedBack.businessType)
        assertEquals(originalTx.customerName, parsedBack.customerName)
        assertEquals(originalTx.grossSales, parsedBack.grossSales, 0.001)
        assertEquals(originalTx.netSales, parsedBack.netSales, 0.001)
        assertEquals(originalTx.estimatedProfit, parsedBack.estimatedProfit, 0.001)
        assertEquals(PaymentStatus.PARTIALLY_PAID, parsedBack.paymentStatus)
        assertEquals(850.0, parsedBack.outstandingBalance, 0.001)
    }

    @Test
    fun testTransaction_paymentStatusReconciliation() {
        // Case 1: Fully Paid (amountPaid >= netSales)
        val fullyPaidFirestore = FirestoreTransaction(
            id = "tx_paid_1",
            netSales = 1000.0,
            amountPaid = 1000.0,
            paymentStatus = "PENDING" // Incorrect status in payload
        )
        val parsedPaid = fullyPaidFirestore.toRoomTransaction()
        assertEquals(PaymentStatus.PAID, parsedPaid.paymentStatus)
        assertEquals(0.0, parsedPaid.outstandingBalance, 0.001)

        // Case 2: Overpayment (amountPaid > netSales)
        val overpaidFirestore = FirestoreTransaction(
            id = "tx_overpaid",
            netSales = 1000.0,
            amountPaid = 1200.0
        )
        val parsedOverpaid = overpaidFirestore.toRoomTransaction()
        assertEquals(PaymentStatus.PAID, parsedOverpaid.paymentStatus)
        assertEquals(0.0, parsedOverpaid.outstandingBalance, 0.001)

        // Case 3: Partial payment
        val partialFirestore = FirestoreTransaction(
            id = "tx_partial",
            netSales = 1000.0,
            amountPaid = 400.0
        )
        val parsedPartial = partialFirestore.toRoomTransaction()
        assertEquals(PaymentStatus.PARTIALLY_PAID, parsedPartial.paymentStatus)
        assertEquals(600.0, parsedPartial.outstandingBalance, 0.001)

        // Case 4: Zero payment
        val zeroFirestore = FirestoreTransaction(
            id = "tx_zero",
            netSales = 1000.0,
            amountPaid = 0.0,
            paymentStatus = "PENDING"
        )
        val parsedZero = zeroFirestore.toRoomTransaction()
        assertEquals(PaymentStatus.PENDING, parsedZero.paymentStatus)
        assertEquals(1000.0, parsedZero.outstandingBalance, 0.001)

        // Case 5: CANCELLED status preserved
        val cancelledFirestore = FirestoreTransaction(
            id = "tx_cancelled",
            netSales = 1000.0,
            amountPaid = 500.0,
            paymentStatus = "CANCELLED"
        )
        val parsedCancelled = cancelledFirestore.toRoomTransaction()
        assertEquals(PaymentStatus.CANCELLED, parsedCancelled.paymentStatus)
    }

    @Test
    fun testTransaction_corruptedTimestampsAndDiscounts() {
        val corrupted = FirestoreTransaction(
            id = "",
            orderDateEpochMillis = -100L,
            discountPercentage = 250.0, // Invalid > 100%
            discountAmount = -50.0,
            grossSales = -200.0
        )

        val parsed = corrupted.toRoomTransaction()
        // Must generate UUID
        assertTrue(parsed.id.isNotBlank())
        // Must use valid positive timestamp
        assertTrue(parsed.orderDateEpochMillis > 0L)
        // Discount percentage clamped to 100.0
        assertEquals(100.0, parsed.discountPercentage, 0.001)
        // Discount amount non-negative
        assertEquals(0.0, parsed.discountAmount, 0.001)
        // Gross sales non-negative
        assertEquals(0.0, parsed.grossSales, 0.001)
    }

    // =========================================================================
    // SETTINGS PARSING & REPOSITORY INTEGRITY TESTS
    // =========================================================================

    @Test
    fun testSettings_validRoundtripConversion() {
        val roomSettings = AppSettings(
            id = 1,
            businessNameEngraving = "Mace Engraving Studio",
            businessNameSintra = "Mace Sintra Prints",
            engravingOrderPrefix = "MACE-ENG",
            sintraOrderPrefix = "MACE-SNT",
            sintraDefaultMarginPercent = 60.0,
            sintraUseCostCalculation = false,
            currencySymbol = "₱"
        )

        val firestoreModel = roomSettings.toFirestore(testUserId)
        assertEquals("default", firestoreModel.id)
        assertEquals(testUserId, firestoreModel.userId)
        assertEquals("MACE-ENG", firestoreModel.engravingOrderPrefix)
        assertEquals(60.0, firestoreModel.sintraDefaultMarginPercent, 0.001)

        val parsedSettings = firestoreModel.toRoomSettings()
        assertEquals("Mace Engraving Studio", parsedSettings.businessNameEngraving)
        assertEquals("MACE-ENG", parsedSettings.engravingOrderPrefix)
        assertEquals("MACE-SNT", parsedSettings.sintraOrderPrefix)
        assertEquals(60.0, parsedSettings.sintraDefaultMarginPercent, 0.001)
        assertEquals("₱", parsedSettings.currencySymbol)
    }

    @Test
    fun testSettings_outOfBoundsMarginAndEmptyPrefixes() {
        val invalidFirestoreSettings = FirestoreSettings(
            id = "default",
            userId = testUserId,
            engravingOrderPrefix = "",
            sintraOrderPrefix = "   ",
            sintraDefaultMarginPercent = 150.0, // Invalid > 100%
            currencySymbol = ""
        )

        val parsed = invalidFirestoreSettings.toRoomSettings()
        assertEquals("ENG", parsed.engravingOrderPrefix)
        assertEquals("SNT", parsed.sintraOrderPrefix)
        assertEquals(100.0, parsed.sintraDefaultMarginPercent, 0.001)
        assertEquals("₱", parsed.currencySymbol)
    }

    // =========================================================================
    // COMPLETE ROOM + FIRESTORE SALES WORKFLOW TESTS
    // =========================================================================

    @Test
    fun testCompleteWorkflow_engravingOrderCreationAndFirestoreMapping() = runBlocking {
        // 1. Calculate sales for Engraving
        val lineItems = listOf(
            LineItemInput(quantity = 10, unitSellingPrice = 70.0, unitCost = 35.0)
        )
        val calcResult = SalesCalculator.calculate(
            businessType = BusinessType.ENGRAVING,
            items = lineItems,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0.0,
            amountPaid = 700.0
        )

        // 2. Generate order number
        val orderNum = repository.generateOrderNumber(BusinessType.ENGRAVING)
        assertTrue(orderNum.startsWith("ENG-"))

        // 3. Build Room transaction
        val tx = SalesTransaction(
            id = UUID.randomUUID().toString(),
            orderNumber = orderNum,
            businessType = BusinessType.ENGRAVING,
            customerName = "Acme Corp",
            orderDateEpochMillis = System.currentTimeMillis(),
            grossSales = calcResult.grossSales,
            netSales = calcResult.netSales,
            totalProductCost = calcResult.totalProductCost,
            estimatedProfit = calcResult.estimatedProfit,
            paymentStatus = calcResult.resolvedStatus,
            amountPaid = 700.0,
            outstandingBalance = calcResult.outstandingBalance
        )

        val lineItemEntities = listOf(
            TransactionLineItem(
                transactionId = tx.id,
                productId = 1L,
                productName = "Bamboo Coaster",
                quantity = 10,
                unitSellingPrice = 70.0,
                unitCost = 35.0,
                lineSubtotal = 700.0
            )
        )

        // 4. Save into Room
        repository.saveTransaction(tx, lineItemEntities, initialPaymentAmount = 700.0)

        // 5. Retrieve from Room
        val savedList = repository.allTransactions.first()
        val saved = savedList.find { it.transaction.orderNumber == orderNum }
        assertNotNull(saved)

        // 6. Map to Firestore
        val summary = saved!!.lineItems.joinToString("; ") { "${it.productName} (x${it.quantity})" }
        val firestoreTx = saved.transaction.toFirestore(testUserId, summary)

        assertEquals("Bamboo Coaster (x10)", firestoreTx.lineItemsSummary)
        assertEquals(700.0, firestoreTx.grossSales, 0.01)
        assertEquals(350.0, firestoreTx.estimatedProfit, 0.01)
        assertEquals("PAID", firestoreTx.paymentStatus)
        assertEquals(0.0, firestoreTx.outstandingBalance, 0.01)

        // 7. Parse back from Firestore and verify
        val restored = firestoreTx.toRoomTransaction(saved.transaction.customerId)
        assertEquals(saved.transaction.orderNumber, restored.orderNumber)
        assertEquals(saved.transaction.netSales, restored.netSales, 0.01)
        assertEquals(PaymentStatus.PAID, restored.paymentStatus)
    }

    @Test
    fun testCompleteWorkflow_sintraBoardMarginModelAndFirestoreMapping() = runBlocking {
        // Sintra Board with 58.3333% margin calculation
        val lineItems = listOf(
            LineItemInput(quantity = 4, unitSellingPrice = 150.0, unitCost = 62.50)
        )
        val calcResult = SalesCalculator.calculate(
            businessType = BusinessType.SINTRA_BOARD,
            items = lineItems,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 0.0,
            amountPaid = 300.0, // Partial payment (300 out of 600)
            sintraMarginPercent = 58.3333,
            sintraUseCostCalculation = false
        )

        assertEquals(600.0, calcResult.grossSales, 0.01)
        assertEquals(350.0, calcResult.estimatedProfit, 0.05)
        assertEquals(300.0, calcResult.outstandingBalance, 0.01)
        assertEquals(PaymentStatus.PARTIALLY_PAID, calcResult.resolvedStatus)

        val tx = SalesTransaction(
            id = UUID.randomUUID().toString(),
            orderNumber = "SNT-2026-0005",
            businessType = BusinessType.SINTRA_BOARD,
            customerName = "Signage Hub",
            orderDateEpochMillis = System.currentTimeMillis(),
            grossSales = calcResult.grossSales,
            netSales = calcResult.netSales,
            totalProductCost = calcResult.totalProductCost,
            estimatedProfit = calcResult.estimatedProfit,
            paymentStatus = calcResult.resolvedStatus,
            amountPaid = 300.0,
            outstandingBalance = calcResult.outstandingBalance
        )

        val firestoreTx = tx.toFirestore(testUserId, "Sintra Board A4 Print (x4)")
        assertEquals("SINTRA_BOARD", firestoreTx.businessType)
        assertEquals("PARTIALLY_PAID", firestoreTx.paymentStatus)
        assertEquals(300.0, firestoreTx.outstandingBalance, 0.01)

        val parsedRoomTx = firestoreTx.toRoomTransaction()
        assertEquals(BusinessType.SINTRA_BOARD, parsedRoomTx.businessType)
        assertEquals(PaymentStatus.PARTIALLY_PAID, parsedRoomTx.paymentStatus)
        assertEquals(300.0, parsedRoomTx.outstandingBalance, 0.01)
    }
}
