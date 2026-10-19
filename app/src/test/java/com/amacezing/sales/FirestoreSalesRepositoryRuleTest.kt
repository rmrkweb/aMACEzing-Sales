package com.amacezing.sales

import com.amacezing.sales.base.FirestoreEmulatorTestBase
import com.amacezing.sales.data.firebase.FirestoreCustomer
import com.amacezing.sales.data.firebase.FirestoreProduct
import com.amacezing.sales.data.firebase.FirestoreSalesRepository
import com.amacezing.sales.data.firebase.FirestoreSettings
import com.amacezing.sales.data.firebase.FirestoreTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreSalesRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authenticatedUser_canSaveProfileAndProduct() = runBlocking {
    val uid = signInTestUser("testuser@example.com")
    val repo = FirestoreSalesRepository(firestore, auth)

    withTimeout(10000L) {
      repo.saveUserProfile()
    }

    val product = FirestoreProduct(
      id = "prod_1",
      userId = uid,
      name = "Bamboo Coaster",
      businessType = "ENGRAVING",
      sellingPrice = 70.0,
      unitCost = 35.0,
      isActive = true
    )

    withTimeout(10000L) {
      repo.productRepo.saveProduct(product)
    }

    val products = withTimeout(10000L) {
      repo.productRepo.observeProducts().first { it.isNotEmpty() }
    }

    assertEquals(1, products.size)
    assertEquals("Bamboo Coaster", products[0].name)
    assertEquals(70.0, products[0].sellingPrice, 0.001)
  }

  @Test
  fun authenticatedUser_canCreateAndReadTransaction() = runBlocking {
    val uid = signInTestUser("salesuser@example.com")
    val repo = FirestoreSalesRepository(firestore, auth)

    val tx = FirestoreTransaction(
      id = "tx_999",
      userId = uid,
      orderNumber = "ENG-2026-0001",
      businessType = "ENGRAVING",
      customerName = "Juan Dela Cruz",
      orderDateEpochMillis = System.currentTimeMillis(),
      discountType = "PERCENTAGE",
      discountPercentage = 5.0,
      discountAmount = 75.0,
      grossSales = 1500.0,
      netSales = 1425.0,
      totalProductCost = 750.0,
      estimatedProfit = 675.0,
      paymentStatus = "PARTIALLY_PAID",
      amountPaid = 500.0,
      outstandingBalance = 925.0,
      paymentNotes = "Downpayment via GCash",
      remarks = "Rush order",
      lineItemsSummary = "Bamboo Coaster (x20)"
    )

    withTimeout(10000L) {
      repo.transactionRepo.saveTransaction(tx)
    }

    val txList = withTimeout(10000L) {
      repo.transactionRepo.observeTransactions().first { it.isNotEmpty() }
    }

    assertEquals(1, txList.size)
    assertEquals("ENG-2026-0001", txList[0].orderNumber)
    assertEquals(1425.0, txList[0].netSales, 0.001)
  }

  @Test
  fun authenticatedUser_canSeedBusinessesAndManageSettings() = runBlocking {
    val uid = signInTestUser("bizuser@example.com")
    val repo = FirestoreSalesRepository(firestore, auth)

    withTimeout(10000L) {
      repo.businessRepo.seedDefaultBusinessesIfEmpty()
    }

    val businesses = withTimeout(10000L) {
      repo.businessRepo.observeBusinesses().first { it.size >= 2 }
    }

    assertEquals(2, businesses.size)
    val engravingBiz = businesses.find { it.type == "ENGRAVING" }
    assertNotNull(engravingBiz)
    assertEquals("ENG", engravingBiz?.orderPrefix)

    val settings = FirestoreSettings(
      id = "default",
      userId = uid,
      businessNameEngraving = "aMACEzing Engraving",
      businessNameSintra = "aMACEzing Sintra Prints",
      engravingOrderPrefix = "ENG",
      sintraOrderPrefix = "SNT",
      sintraDefaultMarginPercent = 58.3333,
      sintraUseCostCalculation = false,
      currencySymbol = "₱"
    )

    withTimeout(10000L) {
      repo.settingsRepo.saveSettings(settings)
    }

    val loadedSettings = withTimeout(10000L) {
      repo.settingsRepo.observeSettings().first { it != null }
    }

    assertNotNull(loadedSettings)
    assertEquals("aMACEzing Engraving", loadedSettings?.businessNameEngraving)
  }

  @Test
  fun authenticatedUser_canCreateAndReadCustomer() = runBlocking {
    val uid = signInTestUser("custuser@example.com")
    val repo = FirestoreSalesRepository(firestore, auth)

    val customer = FirestoreCustomer(
      id = "cust_42",
      userId = uid,
      name = "Maria Santos",
      phone = "09171234567",
      email = "maria@example.com",
      notes = "VIP Corporate Client"
    )

    withTimeout(10000L) {
      repo.customerRepo.saveCustomer(customer)
    }

    val customers = withTimeout(10000L) {
      repo.customerRepo.observeCustomers().first { it.isNotEmpty() }
    }

    assertEquals(1, customers.size)
    assertEquals("Maria Santos", customers[0].name)
    assertEquals("09171234567", customers[0].phone)
  }

  @Test
  fun unauthenticatedUser_failsAccess() = runBlocking {
    auth.signOut()
    val repo = FirestoreSalesRepository(firestore, auth)

    try {
      repo.requireUserId()
      fail("Expected IllegalStateException for unauthenticated access")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("User must be signed in") == true)
    }
  }
}
