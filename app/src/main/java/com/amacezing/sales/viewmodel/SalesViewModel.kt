package com.amacezing.sales.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amacezing.sales.data.AppDatabase
import com.amacezing.sales.data.SalesRepository
import com.amacezing.sales.model.AppSettings
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentRecord
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.model.TransactionLineItem
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.ui.navigation.Screen
import com.amacezing.sales.util.CsvHelper
import com.amacezing.sales.util.Formatters
import com.amacezing.sales.util.ImportPreviewSummary
import com.amacezing.sales.util.ImportValidationRow
import com.amacezing.sales.util.LineItemInput
import com.amacezing.sales.util.SalesCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class DateRangeType(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

data class DashboardMetrics(
    val grossSales: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val netSales: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val totalCollected: Double = 0.0,
    val outstandingReceivables: Double = 0.0,
    val ordersCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val totalProductsSold: Int = 0
)

data class ChartBarData(
    val label: String,
    val value: Double,
    val secondaryValue: Double = 0.0
)

data class ProductSaleStat(
    val productName: String,
    val businessType: BusinessType,
    val totalQuantity: Int,
    val totalSales: Double
)

enum class SyncStatus(val label: String) {
    SYNCED("Cloud Synced"),
    SYNCING("Syncing to Cloud..."),
    OFFLINE_SAVED("Saved Locally (Offline)"),
    SYNC_ERROR("Sync Error (Saved Locally)")
}

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = SalesRepository(database)
    val firestoreRepo = com.amacezing.sales.data.firebase.FirestoreSalesRepository(application)

    val businessRepo: com.amacezing.sales.data.repository.BusinessRepository = firestoreRepo.businessRepo
    val productRepo: com.amacezing.sales.data.repository.ProductRepository = firestoreRepo.productRepo
    val customerRepo: com.amacezing.sales.data.repository.CustomerRepository = firestoreRepo.customerRepo
    val transactionRepo: com.amacezing.sales.data.repository.TransactionRepository = firestoreRepo.transactionRepo
    val settingsRepo: com.amacezing.sales.data.repository.SettingsRepository = firestoreRepo.settingsRepo
    val businessProfileRepo: com.amacezing.sales.data.repository.BusinessProfileRepository = firestoreRepo.businessProfileRepo

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Current Screen
    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenStack = MutableStateFlow(listOf(Screen.DASHBOARD))

    // Business Filter: null = All Businesses, or specific
    private val _selectedBusiness = MutableStateFlow<BusinessType?>(null)
    val selectedBusiness: StateFlow<BusinessType?> = _selectedBusiness.asStateFlow()

    // Date Range
    private val _dateRangeType = MutableStateFlow(DateRangeType.ALL_TIME)
    val dateRangeType: StateFlow<DateRangeType> = _dateRangeType.asStateFlow()

    private val _customStartDateMillis = MutableStateFlow<Long?>(null)
    val customStartDateMillis: StateFlow<Long?> = _customStartDateMillis.asStateFlow()

    private val _customEndDateMillis = MutableStateFlow<Long?>(null)
    val customEndDateMillis: StateFlow<Long?> = _customEndDateMillis.asStateFlow()

    // Search & Status filters for Transactions screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<PaymentStatus?>(null)
    val statusFilter: StateFlow<PaymentStatus?> = _statusFilter.asStateFlow()

    // Modals and dialog states
    val showNewTransactionDialog = MutableStateFlow(false)
    val editingTransaction = MutableStateFlow<TransactionWithDetails?>(null)
    val viewingTransaction = MutableStateFlow<TransactionWithDetails?>(null)
    val recordingPaymentTransaction = MutableStateFlow<TransactionWithDetails?>(null)

    val showCustomerDialog = MutableStateFlow(false)
    val editingCustomer = MutableStateFlow<Customer?>(null)
    val viewingCustomerHistory = MutableStateFlow<Customer?>(null)

    val showProductDialog = MutableStateFlow(false)
    val editingProduct = MutableStateFlow<Product?>(null)

    val itemToDelete = MutableStateFlow<Any?>(null)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Import/Export state
    val importText = MutableStateFlow("")
    val importBusinessType = MutableStateFlow(BusinessType.ENGRAVING)
    val importSheetName = MutableStateFlow("Engraving 2026")
    val importPreview = MutableStateFlow<ImportPreviewSummary?>(null)
    val isImporting = MutableStateFlow(false)

    // Raw Room flows
    val allTransactions: StateFlow<List<TransactionWithDetails>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .combine(MutableStateFlow(AppSettings())) { set, default -> set ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val businessProfile: StateFlow<com.amacezing.sales.model.BusinessProfile> = repository.businessProfileFlow
        .combine(MutableStateFlow(com.amacezing.sales.model.BusinessProfile())) { profile, default -> profile ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.amacezing.sales.model.BusinessProfile())

    init {
        viewModelScope.launch {
            repository.seedInitialCatalogIfEmpty()
        }
    }

    /** Restore the user's Firestore backup on a fresh install before normal sync can upload empty Room data. */
    fun restoreFromCloudIfLocalEmpty() {
        viewModelScope.launch {
            try {
                if (!repository.isLocalSalesDataEmpty()) return@launch
                _syncStatus.value = SyncStatus.SYNCING
                val remoteProducts = firestoreRepo.getProducts()
                val remoteCustomers = firestoreRepo.getCustomers()
                val remoteTransactions = firestoreRepo.getTransactions()
                val remoteSettings = firestoreRepo.getSettings()
                val remoteProfile = firestoreRepo.getBusinessProfile()

                // Do not mark this as restored unless Firestore actually returned data.
                if (remoteProducts.isEmpty() && remoteCustomers.isEmpty() && remoteTransactions.isEmpty() && remoteSettings == null && remoteProfile == null) {
                    _syncStatus.value = SyncStatus.SYNCED
                    return@launch
                }

                repository.restoreCloudRecords(remoteProducts, remoteCustomers, remoteTransactions, remoteSettings, remoteProfile)
                _syncStatus.value = SyncStatus.SYNCED
                snackbarMessage.value = "Cloud backup and business profile restored to this device."
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.SYNC_ERROR
                snackbarMessage.value = "Cloud restore failed: ${e.localizedMessage ?: "Please check your connection and try Sync again."}"
            }
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenStack.value = _screenStack.value + screen
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        val stack = _screenStack.value
        return if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            _screenStack.value = newStack
            _currentScreen.value = newStack.last()
            true
        } else {
            false
        }
    }

    fun setBusinessFilter(business: BusinessType?) {
        _selectedBusiness.value = business
    }

    fun setDateRangeType(range: DateRangeType) {
        _dateRangeType.value = range
    }

    fun setCustomDateRange(startMillis: Long?, endMillis: Long?) {
        _customStartDateMillis.value = startMillis
        _customEndDateMillis.value = endMillis
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: PaymentStatus?) {
        _statusFilter.value = status
    }

    fun dismissSnackbar() {
        snackbarMessage.value = null
    }

    // Helper: is date inside selected range?
    private fun isDateInRange(epochMillis: Long, rangeType: DateRangeType, startCustom: Long?, endCustom: Long?): Boolean {
        if (rangeType == DateRangeType.ALL_TIME) return true
        val date = Instant.ofEpochMilli(epochMillis).atZone(Formatters.MANILA_ZONE_ID).toLocalDate()
        val today = LocalDate.now(Formatters.MANILA_ZONE_ID)

        return when (rangeType) {
            DateRangeType.ALL_TIME -> true
            DateRangeType.TODAY -> date.isEqual(today)
            DateRangeType.THIS_WEEK -> {
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
            }
            DateRangeType.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                !date.isBefore(startOfMonth) && !date.isAfter(endOfMonth)
            }
            DateRangeType.THIS_YEAR -> {
                val startOfYear = today.withDayOfYear(1)
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                !date.isBefore(startOfYear) && !date.isAfter(endOfYear)
            }
            DateRangeType.CUSTOM -> {
                val sDate = startCustom?.let { Instant.ofEpochMilli(it).atZone(Formatters.MANILA_ZONE_ID).toLocalDate() }
                val eDate = endCustom?.let { Instant.ofEpochMilli(it).atZone(Formatters.MANILA_ZONE_ID).toLocalDate() }
                val afterStart = sDate == null || !date.isBefore(sDate)
                val beforeEnd = eDate == null || !date.isAfter(eDate)
                afterStart && beforeEnd
            }
        }
    }

    // Filtered transactions for Dashboard and Reports
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        allTransactions,
        selectedBusiness,
        dateRangeType,
        customStartDateMillis,
        customEndDateMillis
    ) { list, business, range, customStart, customEnd ->
        list.filter { item ->
            val matchBusiness = business == null || item.transaction.businessType == business
            val matchDate = isDateInRange(item.transaction.orderDateEpochMillis, range, customStart, customEnd)
            matchBusiness && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered transactions for the Sales Transactions Screen (including search and status filters)
    val transactionsScreenList: StateFlow<List<TransactionWithDetails>> = combine(
        filteredTransactions,
        searchQuery,
        statusFilter
    ) { list, query, status ->
        list.filter { item ->
            val tx = item.transaction
            val matchStatus = status == null || tx.paymentStatus == status
            val matchQuery = query.isBlank() ||
                tx.orderNumber.contains(query, ignoreCase = true) ||
                tx.customerName.contains(query, ignoreCase = true) ||
                tx.remarks.contains(query, ignoreCase = true) ||
                item.lineItems.any { it.productName.contains(query, ignoreCase = true) }
            matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = filteredTransactions.combine(settings) { list, _ ->
        // Exclude CANCELLED transactions by default from sales and profit totals as requested
        val activeOrders = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }

        val gross = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.grossSales })
        val discounts = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.discountAmount })
        val net = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.netSales })
        val profit = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.estimatedProfit })
        val margin = if (net > 0.0) SalesCalculator.roundTo2Decimals((profit / net) * 100.0) else 0.0

        // Collected and Receivables
        val collected = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.amountPaid })
        val receivables = SalesCalculator.roundTo2Decimals(activeOrders.sumOf { it.transaction.outstandingBalance })
        val ordersCount = activeOrders.size
        val avgOrderValue = if (ordersCount > 0) SalesCalculator.roundTo2Decimals(net / ordersCount) else 0.0
        val totalProductsSold = activeOrders.sumOf { it.totalItemsCount }

        DashboardMetrics(
            grossSales = gross,
            totalDiscounts = discounts,
            netSales = net,
            estimatedProfit = profit,
            profitMarginPercent = margin,
            totalCollected = collected,
            outstandingReceivables = receivables,
            ordersCount = ordersCount,
            averageOrderValue = avgOrderValue,
            totalProductsSold = totalProductsSold
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Outstanding Payments List (Orders with outstandingBalance > 0 and not cancelled)
    val outstandingTransactions: StateFlow<List<TransactionWithDetails>> = filteredTransactions.combine(MutableStateFlow(Unit)) { list, _ ->
        list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED && it.transaction.outstandingBalance > 0.0 }
            .sortedByDescending { it.transaction.outstandingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Sales Breakdown
    val productSalesStats: StateFlow<List<ProductSaleStat>> = filteredTransactions.combine(MutableStateFlow(Unit)) { list, _ ->
        val active = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }
        val map = mutableMapOf<String, Pair<BusinessType, Pair<Int, Double>>>()

        for (tx in active) {
            for (line in tx.lineItems) {
                val current = map[line.productName] ?: Triple(tx.transaction.businessType, 0, 0.0).let { Pair(it.first, Pair(it.second, it.third)) }
                val newQty = current.second.first + line.quantity
                val newSales = current.second.second + line.lineSubtotal
                map[line.productName] = Pair(current.first, Pair(newQty, newSales))
            }
        }

        map.map { (name, data) ->
            ProductSaleStat(
                productName = name,
                businessType = data.first,
                totalQuantity = data.second.first,
                totalSales = SalesCalculator.roundTo2Decimals(data.second.second)
            )
        }.sortedByDescending { it.totalSales }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Business Comparison Stats
    val businessComparison: StateFlow<Pair<DashboardMetrics, DashboardMetrics>> = filteredTransactions.combine(MutableStateFlow(Unit)) { list, _ ->
        val active = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }
        val engList = active.filter { it.transaction.businessType == BusinessType.ENGRAVING }
        val sntList = active.filter { it.transaction.businessType == BusinessType.SINTRA_BOARD }

        fun toMetrics(items: List<TransactionWithDetails>): DashboardMetrics {
            val g = items.sumOf { it.transaction.grossSales }
            val d = items.sumOf { it.transaction.discountAmount }
            val n = items.sumOf { it.transaction.netSales }
            val p = items.sumOf { it.transaction.estimatedProfit }
            val m = if (n > 0.0) (p / n) * 100.0 else 0.0
            val c = items.sumOf { it.transaction.amountPaid }
            val r = items.sumOf { it.transaction.outstandingBalance }
            val count = items.size
            val aov = if (count > 0) n / count else 0.0
            val sold = items.sumOf { it.totalItemsCount }
            return DashboardMetrics(
                grossSales = SalesCalculator.roundTo2Decimals(g),
                totalDiscounts = SalesCalculator.roundTo2Decimals(d),
                netSales = SalesCalculator.roundTo2Decimals(n),
                estimatedProfit = SalesCalculator.roundTo2Decimals(p),
                profitMarginPercent = SalesCalculator.roundTo2Decimals(m),
                totalCollected = SalesCalculator.roundTo2Decimals(c),
                outstandingReceivables = SalesCalculator.roundTo2Decimals(r),
                ordersCount = count,
                averageOrderValue = SalesCalculator.roundTo2Decimals(aov),
                totalProductsSold = sold
            )
        }

        Pair(toMetrics(engList), toMetrics(sntList))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(DashboardMetrics(), DashboardMetrics()))

    // Monthly Trend Chart Data
    val monthlyTrendData: StateFlow<List<ChartBarData>> = filteredTransactions.combine(MutableStateFlow(Unit)) { list, _ ->
        val active = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }
        val grouped = active.groupBy { Formatters.formatEpochMillisToMonthYear(it.transaction.orderDateEpochMillis) }

        grouped.map { (monthStr, items) ->
            val sales = items.sumOf { it.transaction.netSales }
            val profit = items.sumOf { it.transaction.estimatedProfit }
            ChartBarData(
                label = monthStr,
                value = SalesCalculator.roundTo2Decimals(sales),
                secondaryValue = SalesCalculator.roundTo2Decimals(profit)
            )
        }.takeLast(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun createTransaction(
        businessType: BusinessType,
        customerName: String,
        orderDateMillis: Long,
        lineItems: List<LineItemInput>,
        productInfos: List<Pair<Long, String>>, // (id, name)
        discountType: DiscountType,
        discountValue: Double,
        initialPayment: Double,
        paymentMethod: String,
        paymentNotes: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val orderNumber = repository.generateOrderNumber(businessType, orderDateMillis)
            val txId = UUID.randomUUID().toString()

            val calcResult = SalesCalculator.calculate(
                businessType = businessType,
                items = lineItems,
                discountType = discountType,
                discountValue = discountValue,
                amountPaid = initialPayment,
                sintraMarginPercent = currentSettings.sintraDefaultMarginPercent,
                sintraUseCostCalculation = currentSettings.sintraUseCostCalculation
            )

            val newTransaction = SalesTransaction(
                id = txId,
                orderNumber = orderNumber,
                businessType = businessType,
                customerName = customerName.trim(),
                orderDateEpochMillis = orderDateMillis,
                discountType = discountType,
                discountPercentage = if (discountType == DiscountType.PERCENTAGE) discountValue else 0.0,
                discountAmount = calcResult.discountAmount,
                grossSales = calcResult.grossSales,
                netSales = calcResult.netSales,
                estimatedProfit = calcResult.estimatedProfit,
                totalProductCost = calcResult.totalProductCost,
                paymentStatus = calcResult.resolvedStatus,
                amountPaid = initialPayment,
                outstandingBalance = calcResult.outstandingBalance,
                paymentNotes = paymentNotes,
                remarks = remarks
            )

            val mappedLines = lineItems.mapIndexed { idx, item ->
                val prodInfo = productInfos.getOrNull(idx) ?: Pair(0L, "Custom Product")
                TransactionLineItem(
                    transactionId = txId,
                    productId = prodInfo.first,
                    productName = prodInfo.second,
                    quantity = item.quantity,
                    unitSellingPrice = item.unitSellingPrice,
                    unitCost = item.unitCost,
                    lineSubtotal = SalesCalculator.roundTo2Decimals(item.quantity * item.unitSellingPrice)
                )
            }

            repository.saveTransaction(
                transaction = newTransaction,
                lineItems = mappedLines,
                initialPaymentAmount = initialPayment,
                paymentMethod = paymentMethod
            )

            // Cloud sync (offline-first)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val summary = mappedLines.joinToString("; ") { "${it.productName} (x${it.quantity})" }
                firestoreRepo.saveTransaction(
                    com.amacezing.sales.data.firebase.FirestoreTransaction(
                        id = newTransaction.id,
                        userId = firestoreRepo.requireUserId(),
                        orderNumber = newTransaction.orderNumber,
                        businessType = newTransaction.businessType.name,
                        customerName = newTransaction.customerName,
                        orderDateEpochMillis = newTransaction.orderDateEpochMillis,
                        discountType = newTransaction.discountType.name,
                        discountPercentage = newTransaction.discountPercentage,
                        discountAmount = newTransaction.discountAmount,
                        grossSales = newTransaction.grossSales,
                        netSales = newTransaction.netSales,
                        totalProductCost = newTransaction.totalProductCost,
                        estimatedProfit = newTransaction.estimatedProfit,
                        paymentStatus = newTransaction.paymentStatus.name,
                        amountPaid = newTransaction.amountPaid,
                        outstandingBalance = newTransaction.outstandingBalance,
                        paymentNotes = newTransaction.paymentNotes,
                        remarks = newTransaction.remarks,
                        lineItemsSummary = summary
                    )
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }

            snackbarMessage.value = "Order $orderNumber created successfully!"
            showNewTransactionDialog.value = false
        }
    }

    fun updateTransaction(
        tx: SalesTransaction,
        lineItems: List<LineItemInput>,
        productInfos: List<Pair<Long, String>>,
        discountType: DiscountType,
        discountValue: Double,
        remarks: String,
        statusOverride: PaymentStatus? = null
    ) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val calcResult = SalesCalculator.calculate(
                businessType = tx.businessType,
                items = lineItems,
                discountType = discountType,
                discountValue = discountValue,
                amountPaid = tx.amountPaid,
                currentStatus = statusOverride ?: tx.paymentStatus,
                sintraMarginPercent = currentSettings.sintraDefaultMarginPercent,
                sintraUseCostCalculation = currentSettings.sintraUseCostCalculation
            )

            val updatedTx = tx.copy(
                discountType = discountType,
                discountPercentage = if (discountType == DiscountType.PERCENTAGE) discountValue else 0.0,
                discountAmount = calcResult.discountAmount,
                grossSales = calcResult.grossSales,
                netSales = calcResult.netSales,
                estimatedProfit = calcResult.estimatedProfit,
                totalProductCost = calcResult.totalProductCost,
                paymentStatus = statusOverride ?: calcResult.resolvedStatus,
                outstandingBalance = calcResult.outstandingBalance,
                remarks = remarks
            )

            val mappedLines = lineItems.mapIndexed { idx, item ->
                val prodInfo = productInfos.getOrNull(idx) ?: Pair(0L, "Product")
                TransactionLineItem(
                    transactionId = tx.id,
                    productId = prodInfo.first,
                    productName = prodInfo.second,
                    quantity = item.quantity,
                    unitSellingPrice = item.unitSellingPrice,
                    unitCost = item.unitCost,
                    lineSubtotal = SalesCalculator.roundTo2Decimals(item.quantity * item.unitSellingPrice)
                )
            }

            repository.updateTransaction(updatedTx, mappedLines)

            // Cloud sync
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val summary = mappedLines.joinToString("; ") { "${it.productName} (x${it.quantity})" }
                firestoreRepo.saveTransaction(
                    com.amacezing.sales.data.firebase.FirestoreTransaction(
                        id = updatedTx.id,
                        userId = firestoreRepo.requireUserId(),
                        orderNumber = updatedTx.orderNumber,
                        businessType = updatedTx.businessType.name,
                        customerName = updatedTx.customerName,
                        orderDateEpochMillis = updatedTx.orderDateEpochMillis,
                        discountType = updatedTx.discountType.name,
                        discountPercentage = updatedTx.discountPercentage,
                        discountAmount = updatedTx.discountAmount,
                        grossSales = updatedTx.grossSales,
                        netSales = updatedTx.netSales,
                        totalProductCost = updatedTx.totalProductCost,
                        estimatedProfit = updatedTx.estimatedProfit,
                        paymentStatus = updatedTx.paymentStatus.name,
                        amountPaid = updatedTx.amountPaid,
                        outstandingBalance = updatedTx.outstandingBalance,
                        paymentNotes = updatedTx.paymentNotes,
                        remarks = updatedTx.remarks,
                        lineItemsSummary = summary
                    )
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }

            snackbarMessage.value = "Order ${tx.orderNumber} updated!"
            editingTransaction.value = null
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            try {
                firestoreRepo.deleteTransaction(id)
            } catch (_: Exception) {
                // Offline
            }
            snackbarMessage.value = "Transaction deleted"
            itemToDelete.value = null
            viewingTransaction.value = null
        }
    }

    fun recordPayment(
        transactionId: String,
        amount: Double,
        paymentMethod: String,
        reference: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordPayment(transactionId, amount, paymentMethod, reference, notes)
            val updated = allTransactions.value.find { it.transaction.id == transactionId }
            if (updated != null) {
                try {
                    val tx = updated.transaction
                    firestoreRepo.saveTransaction(
                        com.amacezing.sales.data.firebase.FirestoreTransaction(
                            id = tx.id,
                            userId = firestoreRepo.requireUserId(),
                            orderNumber = tx.orderNumber,
                            businessType = tx.businessType.name,
                            customerName = tx.customerName,
                            orderDateEpochMillis = tx.orderDateEpochMillis,
                            grossSales = tx.grossSales,
                            netSales = tx.netSales,
                            estimatedProfit = tx.estimatedProfit,
                            paymentStatus = tx.paymentStatus.name,
                            amountPaid = tx.amountPaid,
                            outstandingBalance = tx.outstandingBalance,
                            paymentNotes = tx.paymentNotes,
                            remarks = tx.remarks
                        )
                    )
                    _syncStatus.value = SyncStatus.SYNCED
                } catch (_: Exception) {
                    _syncStatus.value = SyncStatus.OFFLINE_SAVED
                }
            }
            snackbarMessage.value = "Payment recorded: ₱${Formatters.formatNumber(amount)}"
            recordingPaymentTransaction.value = null
        }
    }

    // Customer actions
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                firestoreRepo.saveCustomer(
                    com.amacezing.sales.data.firebase.FirestoreCustomer(
                        id = if (customer.id > 0) customer.id.toString() else id.toString(),
                        userId = firestoreRepo.requireUserId(),
                        name = customer.name,
                        phone = customer.phone,
                        email = customer.email,
                        notes = customer.notes
                    )
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }
            showCustomerDialog.value = false
            editingCustomer.value = null
            snackbarMessage.value = "Customer saved"
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            try {
                firestoreRepo.deleteCustomer(customer.id.toString())
            } catch (_: Exception) {
                // Offline
            }
            snackbarMessage.value = "Customer removed"
            itemToDelete.value = null
        }
    }

    // Product actions
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            val id = repository.saveProduct(product)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                firestoreRepo.saveProduct(
                    com.amacezing.sales.data.firebase.FirestoreProduct(
                        id = if (product.id > 0) product.id.toString() else id.toString(),
                        userId = firestoreRepo.requireUserId(),
                        name = product.name,
                        businessType = product.businessType.name,
                        sellingPrice = product.sellingPrice,
                        unitCost = product.unitCost,
                        isActive = product.isActive
                    )
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }
            showProductDialog.value = false
            editingProduct.value = null
            snackbarMessage.value = "Product saved"
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            try {
                firestoreRepo.deleteProduct(product.id.toString())
            } catch (_: Exception) {
                // Offline
            }
            snackbarMessage.value = "Product removed"
            itemToDelete.value = null
        }
    }

    // Settings actions
    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                firestoreRepo.saveSettings(
                    com.amacezing.sales.data.firebase.FirestoreSettings(
                        id = "default",
                        userId = firestoreRepo.requireUserId(),
                        businessNameEngraving = newSettings.businessNameEngraving,
                        businessNameSintra = newSettings.businessNameSintra,
                        engravingOrderPrefix = newSettings.engravingOrderPrefix,
                        sintraOrderPrefix = newSettings.sintraOrderPrefix,
                        sintraDefaultMarginPercent = newSettings.sintraDefaultMarginPercent,
                        sintraUseCostCalculation = newSettings.sintraUseCostCalculation,
                        currencySymbol = newSettings.currencySymbol,
                        isProfitMasked = newSettings.isProfitMasked
                    )
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }
            snackbarMessage.value = "Settings updated successfully"
        }
    }

    // Business Profile action
    fun updateBusinessProfile(newProfile: com.amacezing.sales.model.BusinessProfile, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateBusinessProfile(newProfile)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val uid = firestoreRepo.requireUserId()
                val firestoreModel = com.amacezing.sales.data.firebase.FirestoreMappers.run { newProfile.toFirestore(uid) }
                firestoreRepo.saveBusinessProfile(firestoreModel)
                _syncStatus.value = SyncStatus.SYNCED
                snackbarMessage.value = "Business branding profile updated and synced!"
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
                snackbarMessage.value = "Profile saved locally (Offline mode)"
                onComplete?.invoke(true)
            }
        }
    }

    // Bulk Synchronize all data to Cloud Firestore
    fun syncAllToCloud() {
        viewModelScope.launch {
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val uid = firestoreRepo.requireUserId()

                // On a fresh install, restore first. Never upload empty local lists over the cloud backup.
                if (repository.isLocalSalesDataEmpty()) {
                    val remoteProducts = firestoreRepo.getProducts()
                    val remoteCustomers = firestoreRepo.getCustomers()
                    val remoteTransactions = firestoreRepo.getTransactions()
                    val remoteSettings = firestoreRepo.getSettings()
                    val remoteProfile = firestoreRepo.getBusinessProfile()
                    if (remoteProducts.isNotEmpty() || remoteCustomers.isNotEmpty() || remoteTransactions.isNotEmpty() || remoteSettings != null || remoteProfile != null) {
                        repository.restoreCloudRecords(remoteProducts, remoteCustomers, remoteTransactions, remoteSettings, remoteProfile)
                    }
                }

                firestoreRepo.saveUserProfile()
                firestoreRepo.seedDefaultBusinessesIfEmpty()

                // Sync products using FirestoreMappers
                for (p in allProducts.value) {
                    firestoreRepo.saveProduct(com.amacezing.sales.data.firebase.FirestoreMappers.run { p.toFirestore(uid) })
                }

                // Sync customers using FirestoreMappers
                for (c in allCustomers.value) {
                    firestoreRepo.saveCustomer(com.amacezing.sales.data.firebase.FirestoreMappers.run { c.toFirestore(uid) })
                }

                // Sync transactions using FirestoreMappers
                for (item in allTransactions.value) {
                    val tx = item.transaction
                    val summary = item.lineItems.joinToString("; ") { "${it.productName} (x${it.quantity})" }
                    firestoreRepo.saveTransaction(com.amacezing.sales.data.firebase.FirestoreMappers.run { tx.toFirestore(uid, summary) })
                }

                // Sync settings using FirestoreMappers
                firestoreRepo.saveSettings(com.amacezing.sales.data.firebase.FirestoreMappers.run { settings.value.toFirestore(uid) })

                // Sync business profile using FirestoreMappers
                firestoreRepo.saveBusinessProfile(com.amacezing.sales.data.firebase.FirestoreMappers.run { businessProfile.value.toFirestore(uid) })

                _syncStatus.value = SyncStatus.SYNCED
                snackbarMessage.value = "All sales records and business branding successfully synchronized with Cloud Firestore!"
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.SYNC_ERROR
                snackbarMessage.value = "Cloud sync failed (offline or network issue): ${e.localizedMessage}"
            }
        }
    }

    // Excel Migration
    fun loadSampleMigration(csvContent: String, businessType: BusinessType, sheetName: String) {
        importText.value = csvContent
        importBusinessType.value = businessType
        importSheetName.value = sheetName
        previewImport()
    }

    fun previewImport() {
        val content = importText.value
        val biz = importBusinessType.value
        val sheet = importSheetName.value
        val existing = allTransactions.value
        val margin = settings.value.sintraDefaultMarginPercent

        val summary = CsvHelper.parseAndValidateExcelCsv(
            content = content,
            businessType = biz,
            sheetName = sheet,
            existingTransactions = existing,
            sintraMarginPercent = margin
        )
        importPreview.value = summary
    }

    fun executeImport() {
        val preview = importPreview.value ?: return
        val validRows = preview.rows.filter { it.isValid }
        if (validRows.isEmpty()) return

        viewModelScope.launch {
            isImporting.value = true
            val biz = importBusinessType.value
            val sheetName = importSheetName.value
            val currentSettings = settings.value

            val p1Name = if (biz == BusinessType.ENGRAVING) "Bamboo Coaster" else "Sintra Board A4 Print"
            val p1Selling = if (biz == BusinessType.ENGRAVING) 70.0 else 150.0
            val p1Cost = if (biz == BusinessType.ENGRAVING) 35.0 else 62.50

            val p2Name = if (biz == BusinessType.ENGRAVING) "Bamboo Ballpen" else "Sintra Board A3 Print"
            val p2Selling = if (biz == BusinessType.ENGRAVING) 40.0 else 300.0
            val p2Cost = if (biz == BusinessType.ENGRAVING) 20.0 else 125.00

            var importedCount = 0

            for (row in validRows) {
                val txId = UUID.randomUUID().toString()
                val orderNumber = repository.generateOrderNumber(biz, row.parsedDateMillis)

                // Preserve original Excel remarks, parsing payment indicators if present
                val remarks = row.rawRemarks
                val isPaidRemark = remarks.contains("paid", ignoreCase = true) || remarks.contains("received", ignoreCase = true)
                val isDepositRemark = remarks.contains("deposit", ignoreCase = true) || remarks.contains("50%", ignoreCase = true)

                val amountPaid = if (isDepositRemark) {
                    SalesCalculator.roundTo2Decimals(row.calculatedNet * 0.5)
                } else if (isPaidRemark) {
                    row.calculatedNet
                } else {
                    0.0
                }

                val balance = SalesCalculator.roundTo2Decimals((row.calculatedNet - amountPaid).coerceAtLeast(0.0))
                val status = if (amountPaid >= row.calculatedNet && row.calculatedNet > 0.0) {
                    PaymentStatus.PAID
                } else if (amountPaid > 0.0) {
                    PaymentStatus.PARTIALLY_PAID
                } else {
                    PaymentStatus.PENDING
                }

                val tx = SalesTransaction(
                    id = txId,
                    orderNumber = orderNumber,
                    businessType = biz,
                    customerName = row.rawCustomer,
                    orderDateEpochMillis = row.parsedDateMillis,
                    discountType = DiscountType.PERCENTAGE,
                    discountPercentage = row.discountPercent,
                    discountAmount = SalesCalculator.roundTo2Decimals(row.calculatedGross - row.calculatedNet),
                    grossSales = row.calculatedGross,
                    netSales = row.calculatedNet,
                    estimatedProfit = row.calculatedProfit,
                    totalProductCost = SalesCalculator.roundTo2Decimals(
                        (row.product1Qty * p1Cost) + (row.product2Qty * p2Cost)
                    ),
                    paymentStatus = status,
                    amountPaid = amountPaid,
                    outstandingBalance = balance,
                    remarks = "[Imported from $sheetName] $remarks"
                )

                val lineItems = mutableListOf<TransactionLineItem>()
                if (row.product1Qty > 0) {
                    lineItems.add(
                        TransactionLineItem(
                            transactionId = txId,
                            productId = 1L,
                            productName = p1Name,
                            quantity = row.product1Qty,
                            unitSellingPrice = p1Selling,
                            unitCost = p1Cost,
                            lineSubtotal = SalesCalculator.roundTo2Decimals(row.product1Qty * p1Selling)
                        )
                    )
                }
                if (row.product2Qty > 0) {
                    lineItems.add(
                        TransactionLineItem(
                            transactionId = txId,
                            productId = 2L,
                            productName = p2Name,
                            quantity = row.product2Qty,
                            unitSellingPrice = p2Selling,
                            unitCost = p2Cost,
                            lineSubtotal = SalesCalculator.roundTo2Decimals(row.product2Qty * p2Selling)
                        )
                    )
                }

                repository.saveTransaction(
                    transaction = tx,
                    lineItems = lineItems,
                    initialPaymentAmount = amountPaid,
                    paymentMethod = if (remarks.contains("gcash", ignoreCase = true)) "GCash" else if (remarks.contains("check", ignoreCase = true)) "Check" else "Cash"
                )
                importedCount++
            }

            isImporting.value = false
            importPreview.value = null
            importText.value = ""
            snackbarMessage.value = "Successfully imported $importedCount records from $sheetName!"
            navigateTo(Screen.TRANSACTIONS)
        }
    }
}
