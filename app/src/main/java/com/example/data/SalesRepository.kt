package com.example.data

import com.example.model.AppSettings
import com.example.model.BusinessType
import com.example.model.Customer
import com.example.model.PaymentRecord
import com.example.model.PaymentStatus
import com.example.model.Product
import com.example.model.SalesTransaction
import com.example.model.TransactionLineItem
import com.example.model.TransactionWithDetails
import com.example.util.Formatters
import com.example.util.SalesCalculator
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

    val allTransactions: Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()

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

    suspend fun seedInitialCatalogIfEmpty() {
        withContext(Dispatchers.IO) {
            AppDatabase.populateInitialData(database)
        }
    }
}
