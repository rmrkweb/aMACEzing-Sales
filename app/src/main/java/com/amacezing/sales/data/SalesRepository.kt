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
    private val quotationDao = database.quotationDao()

    val allTransactions: Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    val allQuotations: Flow<List<com.amacezing.sales.model.QuotationWithItems>> =
        quotationDao.getAllQuotations()

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

    suspend fun generateQuotationNumber(customDateMillis: Long? = null): String {
        return withContext(Dispatchers.IO) {
            val dateMillis = customDateMillis ?: System.currentTimeMillis()
            val year = Instant.ofEpochMilli(dateMillis)
                .atZone(Formatters.MANILA_ZONE_ID)
                .year
            val count = quotationDao.countQuotations()
            var sequence = count + 1
            String.format("QT-%d-%04d", year, sequence)
        }
    }

    suspend fun saveQuotation(
        quotation: com.amacezing.sales.model.Quotation,
        lineItems: List<com.amacezing.sales.model.QuotationLineItem>
    ): Long = withContext(Dispatchers.IO) {
        val quotationId = quotationDao.insertQuotation(quotation)
        val itemsWithId = lineItems.map { it.copy(quotationId = quotationId) }
        quotationDao.insertLineItems(itemsWithId)
        quotationId
    }

    suspend fun deleteQuotation(id: Long) = withContext(Dispatchers.IO) {
        quotationDao.deleteQuotationById(id)
    }

    suspend fun updateQuotationStatus(id: Long, status: com.amacezing.sales.model.QuotationStatus) = withContext(Dispatchers.IO) {
        quotationDao.updateStatus(id, status)
    }

    suspend fun markQuotationConverted(id: Long, transactionId: String) = withContext(Dispatchers.IO) {
        quotationDao.markConverted(id, transactionId)
    }

    suspend fun getQuotationSnapshot(id: Long): com.amacezing.sales.model.QuotationWithItems? = withContext(Dispatchers.IO) {
        quotationDao.getQuotationSnapshot(id)
    }


    /** True when this installation has no real sales/customer data to protect from a cloud restore. */
    suspend fun isLocalSalesDataEmpty(): Boolean = withContext(Dispatchers.IO) {
        transactionDao.countTransactions() == 0 && customerDao.countCustomers() == 0
    }

    suspend fun getProductsSnapshot(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProductsSnapshot()
    }

    suspend fun getBusinessProfileSnapshot(): com.amacezing.sales.model.BusinessProfile? = withContext(Dispatchers.IO) {
        businessProfileDao.getProfileSnapshot()
    }

    /** Full snapshot (transaction + line items + payments) used when pushing one order to the cloud. */
    suspend fun getTransactionDetailsSnapshot(id: String): TransactionWithDetails? = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionById(id) ?: return@withContext null
        TransactionWithDetails(
            transaction = tx,
            lineItems = transactionDao.getLineItemsByTransactionId(id),
            payments = transactionDao.getPaymentsListForTransaction(id)
        )
    }

    /**
     * Merge products that share the same name + business (case-insensitive). The lowest id survives,
     * order lines are re-pointed to it. Returns the removed product ids (so the cloud copies can be deleted).
     */
    suspend fun dedupeProducts(): List<Long> = withContext(Dispatchers.IO) {
        val removed = mutableListOf<Long>()
        productDao.getAllProductsSnapshot()
            .groupBy { it.businessType to it.name.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                val keep = group.minByOrNull { it.id } ?: return@forEach
                group.filter { it.id != keep.id }.forEach { dup ->
                    transactionDao.repointLineItemProduct(dup.id, keep.id)
                    productDao.deleteProduct(dup)
                    removed += dup.id
                }
            }
        removed
    }

    /** Merge cloud records into Room without deleting local rows. Used only for first-install recovery. */
    suspend fun restoreCloudRecords(
        products: List<com.amacezing.sales.data.firebase.FirestoreProduct>,
        customers: List<com.amacezing.sales.data.firebase.FirestoreCustomer>,
        transactions: List<com.amacezing.sales.data.firebase.FirestoreTransaction>,
        cloudSettings: com.amacezing.sales.data.firebase.FirestoreSettings?,
        cloudProfile: com.amacezing.sales.data.firebase.FirestoreBusinessProfile? = null,
        restoredLogoPath: String? = null
    ) = withContext(Dispatchers.IO) {
        val mapper = com.amacezing.sales.data.firebase.FirestoreMappers
        val restoredCustomers = customers.map { mapper.run { it.toRoomCustomer() } }
        if (restoredCustomers.isNotEmpty()) customerDao.insertCustomers(restoredCustomers)

        val restoredProducts = products.map { mapper.run { it.toRoomProduct() } }
        if (restoredProducts.isNotEmpty()) productDao.insertProducts(restoredProducts)

        val productsByKey = (productDao.getAllProductsSnapshot() + restoredProducts)
            .associateBy { it.businessType to it.name.trim().lowercase() }

        val customerIdByName = restoredCustomers.associate { it.name.trim().lowercase() to it.id }
        val roomTransactions = transactions.map { remote ->
            val customerId = customerIdByName[remote.customerName.trim().lowercase()]
            mapper.run { remote.toRoomTransaction(customerId) }
        }
        if (roomTransactions.isNotEmpty()) transactionDao.insertTransactions(roomTransactions)

        // Order lines and payments live inside the cloud transaction document.
        transactions.zip(roomTransactions).forEach { (remote, room) ->
            val lines = mapper.run { remote.toRoomLineItems() }
                .ifEmpty { linesFromSummary(remote, room, productsByKey) }
            if (lines.isNotEmpty()) {
                transactionDao.deleteLineItemsByTransactionId(room.id)
                transactionDao.insertLineItems(lines.map { it.copy(transactionId = room.id) })
            }
            val pays = mapper.run { remote.toRoomPayments() }
            if (pays.isNotEmpty()) {
                transactionDao.deletePaymentsByTransactionId(room.id)
                transactionDao.insertPayments(pays.map { it.copy(transactionId = room.id) })
            }
        }

        if (cloudSettings != null) {
            settingsDao.insertOrUpdate(mapper.run { cloudSettings.toRoomSettings(id = 1) })
        }

        if (cloudProfile != null) {
            businessProfileDao.insertOrUpdate(
                mapper.run { cloudProfile.toRoomBusinessProfile() }.copy(logoUri = restoredLogoPath ?: "")
            )
        }
    }

    /**
     * Older cloud documents only stored a text summary ("Product (x2); Other (x1)").
     * Rebuild approximate order lines from it so quantities are not lost.
     */
    private fun linesFromSummary(
        remote: com.amacezing.sales.data.firebase.FirestoreTransaction,
        room: SalesTransaction,
        productsByKey: Map<Pair<BusinessType, String>, Product>
    ): List<TransactionLineItem> {
        val regex = Regex("^\\s*(.+?)\\s*\\(x(\\d+)\\)\\s*$")
        val parsed = remote.lineItemsSummary.split(";").mapNotNull { part ->
            regex.matchEntire(part)?.let { it.groupValues[1] to it.groupValues[2].toInt() }
        }
        if (parsed.isEmpty()) return emptyList()
        val totalQty = parsed.sumOf { it.second }.coerceAtLeast(1)
        return parsed.map { (name, qty) ->
            val product = productsByKey[room.businessType to name.trim().lowercase()]
            val unitPrice = product?.sellingPrice ?: (room.grossSales / totalQty)
            val unitCost = product?.unitCost ?: (room.totalProductCost / totalQty)
            TransactionLineItem(
                transactionId = room.id,
                productId = product?.id ?: 0L,
                productName = name,
                quantity = qty,
                unitSellingPrice = SalesCalculator.roundTo2Decimals(unitPrice),
                unitCost = SalesCalculator.roundTo2Decimals(unitCost),
                lineSubtotal = SalesCalculator.roundTo2Decimals(qty * unitPrice)
            )
        }
    }

    suspend fun seedInitialCatalogIfEmpty() {
        withContext(Dispatchers.IO) {
            AppDatabase.populateInitialData(database)
        }
    }
}
