package com.amacezing.sales.data

import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.PaymentRecord
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.SalesCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class SalesRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val settingsDao = database.settingsDao()
    private val businessProfileDao = database.businessProfileDao()

    val allTransactions: Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()

    val businessProfileFlow: Flow<com.amacezing.sales.model.BusinessProfile?> =
        businessProfileDao.getProfileFlow()

    suspend fun getBusinessProfileSnapshot(): com.amacezing.sales.model.BusinessProfile {
        return withContext(Dispatchers.IO) {
            businessProfileDao.getProfileSnapshot() ?: com.amacezing.sales.model.BusinessProfile()
        }
    }

    suspend fun updateBusinessProfile(profile: com.amacezing.sales.model.BusinessProfile) {
        withContext(Dispatchers.IO) {
            businessProfileDao.insertOrUpdate(profile)
        }
    }

    suspend fun getSettingsSnapshot(): AppSettings {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingsSnapshot() ?: AppSettings()
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            settingsDao.insertOrUpdate(settings)
        }
    }

    suspend fun generateOrderNumber(businessType: BusinessType, customDateMillis: Long? = null): String {
        return withContext(Dispatchers.IO) {
            val settings = getSettingsSnapshot()
            val prefix = when (businessType) {
                BusinessType.ENGRAVING -> settings.engravingOrderPrefix
                BusinessType.SINTRA_BOARD -> settings.sintraOrderPrefix
            }
            val dateMillis = customDateMillis ?: System.currentTimeMillis()
            val year = Instant.ofEpochMilli(dateMillis)
                .atZone(Formatters.MANILA_ZONE_ID)
                .year

            val existingNumbers = transactionDao.getAllOrderNumbers().toSet()
            var sequence = 1
            var candidate: String
            do {
                candidate = String.format("%s-%d-%04d", prefix, year, sequence)
                sequence++
            } while (existingNumbers.contains(candidate))

            candidate
        }
    }

    suspend fun getOrCreateCustomer(name: String): Customer {
        return withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            val existing = customerDao.getCustomerByName(trimmed)
            if (existing != null) {
                existing
            } else {
                val newCustomer = Customer(name = trimmed)
                val newId = customerDao.insertCustomer(newCustomer)
                newCustomer.copy(id = newId)
            }
        }
    }

    suspend fun saveTransaction(
        transaction: SalesTransaction,
        lineItems: List<TransactionLineItem>,
        initialPaymentAmount: Double = 0.0,
        paymentMethod: String = "Cash",
        paymentReference: String = ""
    ) {
        withContext(Dispatchers.IO) {
            val customer = getOrCreateCustomer(transaction.customerName)
            val updatedTx = transaction.copy(
                customerId = customer.id,
                updatedAt = System.currentTimeMillis()
            )

            val initialPayment = if (initialPaymentAmount > 0.0) {
                PaymentRecord(
                    transactionId = updatedTx.id,
                    amount = initialPaymentAmount,
                    paymentDateEpochMillis = updatedTx.orderDateEpochMillis,
                    paymentMethod = paymentMethod,
                    referenceNumber = paymentReference,
                    notes = "Initial payment upon order creation"
                )
            } else null

            transactionDao.saveFullTransaction(updatedTx, lineItems, initialPayment)
        }
    }

    suspend fun updateTransaction(
        transaction: SalesTransaction,
        lineItems: List<TransactionLineItem>
    ) {
        withContext(Dispatchers.IO) {
            val customer = getOrCreateCustomer(transaction.customerName)
            // Re-calculate outstanding balance based on existing payments
            val existingPayments = transactionDao.getPaymentsListForTransaction(transaction.id)
            val totalPaid = SalesCalculator.roundTo2Decimals(existingPayments.sumOf { it.amount })
            val balance = SalesCalculator.roundTo2Decimals((transaction.netSales - totalPaid).coerceAtLeast(0.0))

            val status = if (transaction.paymentStatus == PaymentStatus.CANCELLED) {
                PaymentStatus.CANCELLED
            } else if (totalPaid >= transaction.netSales && transaction.netSales > 0.0) {
                PaymentStatus.PAID
            } else if (totalPaid > 0.0) {
                PaymentStatus.PARTIALLY_PAID
            } else {
                PaymentStatus.PENDING
            }

            val updatedTx = transaction.copy(
                customerId = customer.id,
                amountPaid = totalPaid,
                outstandingBalance = balance,
                paymentStatus = status,
                updatedAt = System.currentTimeMillis()
            )

            transactionDao.insertTransaction(updatedTx)
            transactionDao.deleteLineItemsByTransactionId(transaction.id)
            if (lineItems.isNotEmpty()) {
                val mapped = lineItems.map { it.copy(transactionId = transaction.id) }
                transactionDao.insertLineItems(mapped)
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
        }
    }

    suspend fun recordPayment(
        transactionId: String,
        amount: Double,
        method: String,
        reference: String,
        notes: String
    ) {
        withContext(Dispatchers.IO) {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@withContext
            val payment = PaymentRecord(
                transactionId = transactionId,
                amount = amount,
                paymentDateEpochMillis = System.currentTimeMillis(),
                paymentMethod = method,
                referenceNumber = reference,
                notes = notes
            )
            transactionDao.insertPayment(payment)

            val payments = transactionDao.getPaymentsListForTransaction(transactionId)
            val totalPaid = SalesCalculator.roundTo2Decimals(payments.sumOf { it.amount })
            val balance = SalesCalculator.roundTo2Decimals((tx.netSales - totalPaid).coerceAtLeast(0.0))

            val newStatus = if (tx.paymentStatus == PaymentStatus.CANCELLED) {
                PaymentStatus.CANCELLED
            } else if (totalPaid >= tx.netSales && tx.netSales > 0.0) {
                PaymentStatus.PAID
            } else if (totalPaid > 0.0) {
                PaymentStatus.PARTIALLY_PAID
            } else {
                PaymentStatus.PENDING
            }

            val updatedTx = tx.copy(
                amountPaid = totalPaid,
                outstandingBalance = balance,
                paymentStatus = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            transactionDao.updateTransaction(updatedTx)
        }
    }

    suspend fun saveProduct(product: Product): Long {
        return withContext(Dispatchers.IO) {
            productDao.insertProduct(product)
        }
    }

    suspend fun deleteProduct(product: Product) {
        withContext(Dispatchers.IO) {
            productDao.deleteProduct(product)
        }
    }

    suspend fun saveCustomer(customer: Customer): Long {
        return withContext(Dispatchers.IO) {
            customerDao.insertCustomer(customer)
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            customerDao.deleteCustomer(customer)
        }
    }


    /** True when this installation has no real sales/customer data to protect from a cloud restore. */
    suspend fun isLocalSalesDataEmpty(): Boolean = withContext(Dispatchers.IO) {
        transactionDao.countTransactions() == 0 && customerDao.countCustomers() == 0
    }

    /** Merge cloud records into Room without deleting local rows. Used only for first-install recovery. */
    suspend fun restoreCloudRecords(
        products: List<com.amacezing.sales.data.firebase.FirestoreProduct>,
        customers: List<com.amacezing.sales.data.firebase.FirestoreCustomer>,
        transactions: List<com.amacezing.sales.data.firebase.FirestoreTransaction>,
        cloudSettings: com.amacezing.sales.data.firebase.FirestoreSettings?,
        cloudProfile: com.amacezing.sales.data.firebase.FirestoreBusinessProfile? = null
    ) = withContext(Dispatchers.IO) {
        val mapper = com.amacezing.sales.data.firebase.FirestoreMappers
        val restoredCustomers = customers.map { mapper.run { it.toRoomCustomer() } }
        if (restoredCustomers.isNotEmpty()) customerDao.insertCustomers(restoredCustomers)

        val restoredProducts = products.map { mapper.run { it.toRoomProduct() } }
        if (restoredProducts.isNotEmpty()) productDao.insertProducts(restoredProducts)

        val customerIdByName = restoredCustomers.associate { it.name.trim().lowercase() to it.id }
        val restoredTransactions = transactions.map { remote ->
            val customerId = customerIdByName[remote.customerName.trim().lowercase()]
            mapper.run { remote.toRoomTransaction(customerId) }
        }
        if (restoredTransactions.isNotEmpty()) transactionDao.insertTransactions(restoredTransactions)

        if (cloudSettings != null) {
            settingsDao.insertOrUpdate(mapper.run { cloudSettings.toRoomSettings(id = 1) })
        }

        if (cloudProfile != null) {
            businessProfileDao.insertOrUpdate(mapper.run { cloudProfile.toRoomBusinessProfile() })
        }
    }

    suspend fun seedInitialCatalogIfEmpty() {
        withContext(Dispatchers.IO) {
            AppDatabase.populateInitialData(database)
        }
    }
}
