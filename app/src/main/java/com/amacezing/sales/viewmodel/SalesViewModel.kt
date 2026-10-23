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
import com.amacezing.sales.util.ExistingOrderRef
import com.amacezing.sales.util.ImportContext
import com.amacezing.sales.util.ImportEngine
import com.amacezing.sales.util.ImportPlan
import com.amacezing.sales.util.ProductRef
import com.amacezing.sales.util.TabularReader
import com.amacezing.sales.util.LineItemInput
import com.amacezing.sales.util.SalesCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

enum class DateRangeType(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Range")
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
    val totalProductsSold: Int = 0,
    val topCustomerName: String = "",
    val topCustomerSales: Double = 0.0
)

data class ChartBarData(
    val label: String,
    val value: Double,
    val secondaryValue: Double = 0.0
)

data class BusinessMetrics(
    val business: com.amacezing.sales.model.Business,
    val metrics: DashboardMetrics
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

    // Data import state (file upload -> validation -> confirm)
    val importFileName = MutableStateFlow<String?>(null)
    val importPlan = MutableStateFlow<ImportPlan?>(null)
    val importError = MutableStateFlow<String?>(null)
    val importIncludeDuplicates = MutableStateFlow(false)
    val isImporting = MutableStateFlow(false)

    // Raw Room flows
    val allTransactions: StateFlow<List<TransactionWithDetails>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businesses: StateFlow<List<com.amacezing.sales.model.Business>> = repository.allBusinesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Profit rule for a business id; unknown ids behave like unit-cost businesses. */
    fun profitRuleFor(businessId: String): com.amacezing.sales.model.ProfitRule =
        businesses.value.firstOrNull { it.id == businessId }?.toProfitRule()
            ?: com.amacezing.sales.model.ProfitRule()

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuotations: StateFlow<List<com.amacezing.sales.model.QuotationWithItems>> = repository.allQuotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .combine(MutableStateFlow(AppSettings())) { set, default -> set ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val businessProfile: StateFlow<com.amacezing.sales.model.BusinessProfile> = repository.businessProfileFlow
        .combine(MutableStateFlow(com.amacezing.sales.model.BusinessProfile())) { profile, default -> profile ?: default }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.amacezing.sales.model.BusinessProfile())

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
            repairConvertedQuotationCosts()
        }
    }

    /**
     * Orders converted from a quotation before costs were carried over have zero cost on every line, so
     * they showed 100% profit. Fills in each line's cost from the catalog (by product id, then by name
     * within the business) and recomputes profit. Idempotent: lines that already have a cost, or whose
     * product has no cost on file, are left untouched, so it does nothing once the data is correct.
     */
    private suspend fun repairConvertedQuotationCosts() {
        try {
            val catalog = repository.getProductsSnapshot()
            val transactions = repository.allTransactions.first()
            var repaired = 0
            for (item in transactions) {
                val tx = item.transaction
                if (tx.paymentStatus == PaymentStatus.CANCELLED || !tx.remarks.startsWith("Converted from quotation")) continue
                if (item.lineItems.none { it.unitCost <= 0.0 }) continue

                var changed = false
                val fixedLines = item.lineItems.map { line ->
                    if (line.unitCost > 0.0) return@map line
                    val product = catalog.firstOrNull { it.id == line.productId }
                        ?: catalog.firstOrNull {
                            it.businessType == tx.businessType && it.name.equals(line.productName.trim(), ignoreCase = true)
                        }
                    val cost = product?.unitCost ?: 0.0
                    if (cost > 0.0) {
                        changed = true
                        line.copy(unitCost = cost)
                    } else line
                }
                if (!changed) continue

                val calc = SalesCalculator.calculate(
                    profitRule = profitRuleFor(tx.businessType),
                    items = fixedLines.map { LineItemInput(it.quantity, it.unitSellingPrice, it.unitCost) },
                    discountType = tx.discountType,
                    discountValue = if (tx.discountType == DiscountType.PERCENTAGE) tx.discountPercentage else tx.discountAmount,
                    amountPaid = tx.amountPaid,
                    currentStatus = tx.paymentStatus
                )
                val updated = tx.copy(
                    estimatedProfit = calc.estimatedProfit,
                    totalProductCost = calc.totalProductCost
                )
                repository.updateTransaction(updated, fixedLines.map { it.copy(id = 0) })
                try {
                    pushTransactionToCloud(updated.id)
                } catch (_: Exception) {
                    // Offline: the next full sync uploads it.
                }
                repaired++
            }
            if (repaired > 0) {
                snackbarMessage.value = "Fixed profit on $repaired order${if (repaired == 1) "" else "s"} converted from quotations"
            }
        } catch (_: Exception) {
            // Never block startup on a repair.
        }
    }

    /** Restores cloud records (incl. order lines, payments and the logo), then removes duplicate products. */
    private suspend fun restoreFromRemote(
        products: List<com.amacezing.sales.data.firebase.FirestoreProduct>,
        customers: List<com.amacezing.sales.data.firebase.FirestoreCustomer>,
        transactions: List<com.amacezing.sales.data.firebase.FirestoreTransaction>,
        settings: com.amacezing.sales.data.firebase.FirestoreSettings?,
        profile: com.amacezing.sales.data.firebase.FirestoreBusinessProfile?,
        businesses: List<com.amacezing.sales.data.firebase.FirestoreBusiness> = emptyList(),
        quotations: List<com.amacezing.sales.data.firebase.FirestoreQuotation> = emptyList()
    ) {
        val logoPath = profile?.logoBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { com.amacezing.sales.util.LogoCodec.decodeBase64ToFile(getApplication(), it) }
        repository.restoreCloudRecords(products, customers, transactions, settings, profile, logoPath, businesses)
        repository.restoreQuotations(quotations)
        dedupeProductsEverywhere()
    }

    /** Merges same-name products locally and deletes the extra copies from Firestore too. */
    private suspend fun dedupeProductsEverywhere() {
        val removed = repository.dedupeProducts()
        removed.forEach { id ->
            try { firestoreRepo.deleteProduct(id.toString()) } catch (_: Exception) { /* offline: next sync */ }
        }
    }

    /** Pushes one order (with its lines and payments) to the cloud, built from the local database. */
    private suspend fun pushTransactionToCloud(transactionId: String) {
        val details = repository.getTransactionDetailsSnapshot(transactionId) ?: return
        val uid = firestoreRepo.requireUserId()
        val summary = details.lineItems.joinToString("; ") { "${it.productName} (x${it.quantity})" }
        firestoreRepo.saveTransaction(
            com.amacezing.sales.data.firebase.FirestoreMappers.run {
                details.transaction.toFirestore(uid, summary, details.lineItems, details.payments)
            }
        )
    }

    private suspend fun pushQuotationToCloud(id: Long) {
        val details = repository.getQuotationSnapshot(id) ?: return
        val uid = firestoreRepo.requireUserId()
        firestoreRepo.saveQuotation(
            com.amacezing.sales.data.firebase.FirestoreMappers.run { details.toFirestore(uid) }
        )
    }

    /** Offline-first: the local change always stands; a failed upload is retried by the next full sync. */
    private suspend fun tryPushQuotation(id: Long) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            pushQuotationToCloud(id)
            _syncStatus.value = SyncStatus.SYNCED
        } catch (_: Exception) {
            _syncStatus.value = SyncStatus.OFFLINE_SAVED
        }
    }

    /** Business profile as a cloud model, including the logo image. */
    private fun profileForCloud(
        profile: com.amacezing.sales.model.BusinessProfile,
        uid: String
    ): com.amacezing.sales.data.firebase.FirestoreBusinessProfile =
        com.amacezing.sales.data.firebase.FirestoreMappers.run { profile.toFirestore(uid) }
            .copy(logoBase64 = com.amacezing.sales.util.LogoCodec.encodeFileToBase64(profile.logoUri))

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
                val remoteBusinesses = try { firestoreRepo.getBusinesses() } catch (_: Exception) { emptyList() }
                val remoteQuotations = try { firestoreRepo.getQuotations() } catch (_: Exception) { emptyList() }

                // Do not mark this as restored unless Firestore actually returned data.
                if (remoteProducts.isEmpty() && remoteCustomers.isEmpty() && remoteTransactions.isEmpty() && remoteQuotations.isEmpty() && remoteSettings == null && remoteProfile == null) {
                    _syncStatus.value = SyncStatus.SYNCED
                    return@launch
                }

                restoreFromRemote(remoteProducts, remoteCustomers, remoteTransactions, remoteSettings, remoteProfile, remoteBusinesses, remoteQuotations)
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

    /**
     * Applies an inclusive custom range of local calendar dates and switches the period filter to CUSTOM.
     * The range is stored as [start of first day, end of last day] in the app's time zone, so every order
     * on the selected end date is included. Returns false (and changes nothing) if start is after end.
     */
    fun applyCustomDateRange(startDate: LocalDate, endDate: LocalDate): Boolean {
        if (startDate.isAfter(endDate)) return false
        val zone = Formatters.MANILA_ZONE_ID
        _customStartDateMillis.value = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        _customEndDateMillis.value = endDate.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
        _dateRangeType.value = DateRangeType.CUSTOM
        return true
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

        // Top customer by net sales (names compared case-insensitively)
        val topCustomer = activeOrders
            .groupBy { it.transaction.customerName.trim().lowercase() }
            .filterKeys { it.isNotEmpty() }
            .map { (_, orders) ->
                orders.first().transaction.customerName.trim() to orders.sumOf { it.transaction.netSales }
            }
            .maxByOrNull { it.second }

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
            totalProductsSold = totalProductsSold,
            topCustomerName = topCustomer?.first.orEmpty(),
            topCustomerSales = SalesCalculator.roundTo2Decimals(topCustomer?.second ?: 0.0)
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
    val businessComparison: StateFlow<List<BusinessMetrics>> = combine(filteredTransactions, businesses) { list, bizList ->
        val active = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }

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

        // Active businesses always appear; archived ones only while they still have orders.
        bizList.mapNotNull { business ->
            val items = active.filter { it.transaction.businessType == business.id }
            if (business.isActive || items.isNotEmpty()) BusinessMetrics(business, toMetrics(items)) else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Trend Chart Data
    val monthlyTrendData: StateFlow<List<ChartBarData>> = filteredTransactions.map { list ->
        val active = list.filter { it.transaction.paymentStatus != PaymentStatus.CANCELLED }
        if (active.isEmpty()) return@map emptyList<ChartBarData>()

        // Group by calendar month (year * 12 + month) so months sort chronologically.
        // (The list arrives newest-first, so grouping by label and taking the last 6 used to show
        // the six OLDEST months in reverse order.)
        val manila = java.util.TimeZone.getTimeZone("Asia/Manila")
        val cal = java.util.Calendar.getInstance(manila)
        fun monthKey(millis: Long): Int {
            cal.timeInMillis = millis
            return cal.get(java.util.Calendar.YEAR) * 12 + cal.get(java.util.Calendar.MONTH)
        }

        val byMonth = active.groupBy { monthKey(it.transaction.orderDateEpochMillis) }
        val lastKey = byMonth.keys.max()
        val firstKey = maxOf(byMonth.keys.min(), lastKey - 5)   // latest 6 months, gaps included

        (firstKey..lastKey).map { key ->
            val items = byMonth[key].orEmpty()
            cal.clear()
            cal.set(key / 12, key % 12, 1, 12, 0, 0)
            ChartBarData(
                label = Formatters.formatEpochMillisToMonthYear(cal.timeInMillis),
                value = SalesCalculator.roundTo2Decimals(items.sumOf { it.transaction.netSales }),
                secondaryValue = SalesCalculator.roundTo2Decimals(items.sumOf { it.transaction.estimatedProfit })
            )
        }
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
                profitRule = profitRuleFor(businessType),
                items = lineItems,
                discountType = discountType,
                discountValue = discountValue,
                amountPaid = initialPayment
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
                pushTransactionToCloud(newTransaction.id)
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
                profitRule = profitRuleFor(tx.businessType),
                items = lineItems,
                discountType = discountType,
                discountValue = discountValue,
                amountPaid = tx.amountPaid,
                currentStatus = statusOverride ?: tx.paymentStatus
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
                pushTransactionToCloud(updatedTx.id)
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
                    pushTransactionToCloud(updated.transaction.id)
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

    // Business actions
    /** Result of validating a business form; null means valid. */
    fun validateBusiness(
        name: String,
        prefix: String,
        editingId: String?
    ): String? {
        val trimmedName = name.trim()
        val cleanPrefix = prefix.trim().uppercase()
        val others = businesses.value.filter { it.id != editingId }
        return when {
            trimmedName.isEmpty() -> "Enter a business name."
            others.any { it.name.equals(trimmedName, ignoreCase = true) } -> "A business with this name already exists."
            cleanPrefix.length !in 2..6 || !cleanPrefix.all { it.isLetterOrDigit() } ->
                "Prefix must be 2 to 6 letters or digits."
            others.any { it.orderPrefix.equals(cleanPrefix, ignoreCase = true) } ->
                "This prefix is already used by another business."
            else -> null
        }
    }

    /** Builds a unique, stable id (e.g. "INVITATION_PRINTING") for a new business. */
    fun newBusinessId(name: String): String {
        val base = name.trim().uppercase()
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifEmpty { "BUSINESS" }
        val taken = businesses.value.map { it.id }.toSet()
        if (base !in taken) return base
        var n = 2
        while ("${base}_$n" in taken) n++
        return "${base}_$n"
    }

    suspend fun getBusinessUsage(id: String): SalesRepository.BusinessUsage = repository.getBusinessUsage(id)

    /** Deletes a business that nothing refers to, locally and in the cloud. */
    fun deleteBusiness(business: com.amacezing.sales.model.Business) {
        viewModelScope.launch {
            if (!repository.deleteBusinessIfUnused(business.id)) {
                snackbarMessage.value = "${business.name} is still used by existing records. Archive it instead."
                return@launch
            }
            try {
                firestoreRepo.deleteBusiness(business.id.lowercase())
            } catch (_: Exception) {
                // Offline: the local delete stands; a later full sync does not re-create it.
            }
            snackbarMessage.value = "${business.name} deleted"
        }
    }

    fun saveBusiness(business: com.amacezing.sales.model.Business) {
        viewModelScope.launch {
            repository.saveBusiness(business)
            try {
                _syncStatus.value = SyncStatus.SYNCING
                val uid = firestoreRepo.requireUserId()
                firestoreRepo.saveBusiness(
                    com.amacezing.sales.data.firebase.FirestoreMappers.run { business.toFirestore(uid) }
                )
                _syncStatus.value = SyncStatus.SYNCED
            } catch (_: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_SAVED
            }
            snackbarMessage.value = "${business.name} saved"
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
                        businessType = product.businessType,
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
                firestoreRepo.saveBusinessProfile(profileForCloud(newProfile, uid))
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
        viewModelScope.launch { performCloudSync() }
    }

    private val _lastSyncError = MutableStateFlow<String?>(null)
    /** Reason the most recent sync failed (shown in the logout dialog). */
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    /** Runs the writes in small parallel groups; sequential awaits made big syncs very slow. */
    private suspend fun <T> inParallel(items: List<T>, groupSize: Int = 20, block: suspend (T) -> Unit) {
        items.chunked(groupSize).forEach { group ->
            kotlinx.coroutines.coroutineScope {
                group.map { item -> async { block(item) } }.awaitAll()
            }
        }
    }

    /** Full cloud sync. Returns true on success. [silent] suppresses snackbars (used by the logout flow). */
    suspend fun performCloudSync(silent: Boolean = false): Boolean {
        var step = "start"
        _lastSyncError.value = null
        try {
            _syncStatus.value = SyncStatus.SYNCING
            val uid = firestoreRepo.requireUserId()

            // On a fresh install, restore first. Never upload empty local lists over the cloud backup.
            if (repository.isLocalSalesDataEmpty()) {
                step = "restore from cloud"
                val remoteProducts = firestoreRepo.getProducts()
                val remoteCustomers = firestoreRepo.getCustomers()
                val remoteTransactions = firestoreRepo.getTransactions()
                val remoteSettings = firestoreRepo.getSettings()
                val remoteProfile = firestoreRepo.getBusinessProfile()
                val remoteBusinesses = try { firestoreRepo.getBusinesses() } catch (_: Exception) { emptyList() }
                val remoteQuotations = try { firestoreRepo.getQuotations() } catch (_: Exception) { emptyList() }
                if (remoteProducts.isNotEmpty() || remoteCustomers.isNotEmpty() || remoteTransactions.isNotEmpty() || remoteQuotations.isNotEmpty() || remoteSettings != null || remoteProfile != null) {
                    restoreFromRemote(remoteProducts, remoteCustomers, remoteTransactions, remoteSettings, remoteProfile, remoteBusinesses, remoteQuotations)
                }
            }

            // Clean up duplicate products (local + cloud) before uploading anything.
            step = "remove duplicate products"
            dedupeProductsEverywhere()

            step = "users/$uid (profile)"
            firestoreRepo.saveUserProfile()
            step = "users/$uid/businesses"
            val mapper = com.amacezing.sales.data.firebase.FirestoreMappers

            // Businesses go first so every order/product they own has its business in the cloud.
            repository.getBusinessesSnapshot().forEach { b ->
                firestoreRepo.saveBusiness(mapper.run { b.toFirestore(uid) })
            }

            step = "users/$uid/products"
            inParallel(repository.getProductsSnapshot()) { p ->
                firestoreRepo.saveProduct(mapper.run { p.toFirestore(uid) })
            }

            step = "users/$uid/customers"
            inParallel(allCustomers.value) { c ->
                firestoreRepo.saveCustomer(mapper.run { c.toFirestore(uid) })
            }

            step = "users/$uid/transactions"
            inParallel(allTransactions.value.map { it.transaction.id }) { id ->
                pushTransactionToCloud(id)
            }

            step = "users/$uid/quotations"
            inParallel(repository.getAllQuotationsSnapshot().map { it.quotation.id }) { id ->
                pushQuotationToCloud(id)
            }

            step = "users/$uid/settings/default"
            firestoreRepo.saveSettings(mapper.run { settings.value.toFirestore(uid) })

            step = "users/$uid/business_profile"
            val currentProfile = repository.getBusinessProfileSnapshot()
            firestoreRepo.saveBusinessProfile(profileForCloud(currentProfile, uid))

            _syncStatus.value = SyncStatus.SYNCED
            if (!silent) snackbarMessage.value = "All sales records and business branding successfully synchronized with Cloud Firestore!"
            return true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.SYNC_ERROR
            val detail = "Failed at $step: ${e.localizedMessage ?: e.javaClass.simpleName}"
            _lastSyncError.value = detail
            if (!silent) snackbarMessage.value = "Cloud sync failed at $step: ${e.localizedMessage}"
            return false
        }
    }

    /** Used before logout: sync everything, but never hang forever when offline. */
    suspend fun syncBeforeLogout(): Boolean {
        val result = kotlinx.coroutines.withTimeoutOrNull(90_000L) { performCloudSync(silent = true) }
        if (result == null) _lastSyncError.value = "Timed out after 90 seconds. Check your internet connection."
        return result == true
    }

    /**
     * Logout cleanup: wipes all local data so the device is back to a fresh install
     * (cloud data is untouched and is restored on the next sign-in).
     */
    suspend fun wipeLocalData() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val ctx = getApplication<Application>()

        // Room: remove every row, then re-seed defaults exactly like a first install.
        database.clearAllTables()
        // Room's clearAllTables keeps the AUTOINCREMENT counters, so re-seeded products would get new ids
        // (5, 6, 7...) and duplicate the cloud copies on restore. Reset the counters like a fresh install.
        database.openHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence")
        AppDatabase.populateInitialData(database)

        // Branding logo copy, cached PDFs / downloaded updates.
        java.io.File(ctx.filesDir, "branding").deleteRecursively()
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }

        // Device-local security setting.
        com.amacezing.sales.security.SecurityPrefs.setBiometricLockEnabled(ctx, false)
        com.amacezing.sales.security.AppLock.unlock()

        // Back to the dashboard for the next sign-in.
        _screenStack.value = listOf(Screen.DASHBOARD)
        _currentScreen.value = Screen.DASHBOARD
    }

    // --- Data import (CSV / Excel upload) ---
    fun clearImport() {
        importFileName.value = null
        importPlan.value = null
        importError.value = null
        importIncludeDuplicates.value = false
    }

    /** Reads and validates an uploaded file; nothing is saved until [executeImport]. */
    fun loadImportFile(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            importError.value = null
            importPlan.value = null
            importIncludeDuplicates.value = false
            importFileName.value = fileName
            try {
                val catalog = repository.getProductsSnapshot()
                val existing = repository.allTransactions.first()
                val plan = withContext(Dispatchers.Default) {
                    val rows = TabularReader.read(fileName, bytes)
                    ImportEngine.validate(
                        rows,
                        ImportContext(
                            businesses = businesses.value,
                            products = catalog.map { ProductRef(it.id, it.businessType, it.name, it.unitCost) },
                            existingOrders = existing.map {
                                ExistingOrderRef(
                                    it.transaction.businessType,
                                    it.transaction.customerName,
                                    it.transaction.orderDateEpochMillis,
                                    it.transaction.netSales
                                )
                            }
                        )
                    )
                }
                importPlan.value = plan
            } catch (e: IllegalArgumentException) {
                importError.value = e.message ?: "This file can't be imported."
            } catch (e: Exception) {
                importError.value = "Couldn't read this file. Check that it is a valid CSV or Excel (.xlsx) file."
            }
        }
    }

    fun executeImport() {
        val plan = importPlan.value ?: return
        if (isImporting.value) return
        val includeDuplicates = importIncludeDuplicates.value
        val toImport = plan.orders.filter { includeDuplicates || !it.isDuplicate }
        if (toImport.isEmpty()) return

        viewModelScope.launch {
            isImporting.value = true
            try {
                val catalog = repository.getProductsSnapshot()
                var imported = 0
                for (order in toImport) {
                    val inputs = order.lines.map { line ->
                        val cost = line.unitCost
                            ?: catalog.firstOrNull { it.id == line.productId }?.unitCost
                            ?: 0.0
                        LineItemInput(line.quantity, line.unitPrice, cost)
                    }
                    val calc = SalesCalculator.calculate(
                        profitRule = profitRuleFor(order.businessId),
                        items = inputs,
                        discountType = DiscountType.PERCENTAGE,
                        discountValue = order.discountPercent,
                        amountPaid = order.amountPaid
                    )
                    val txId = UUID.randomUUID().toString()
                    val orderNumber = repository.generateOrderNumber(order.businessId, order.dateMillis)
                    val tx = SalesTransaction(
                        id = txId,
                        orderNumber = orderNumber,
                        businessType = order.businessId,
                        customerName = order.customer,
                        orderDateEpochMillis = order.dateMillis,
                        discountType = DiscountType.PERCENTAGE,
                        discountPercentage = order.discountPercent,
                        discountAmount = calc.discountAmount,
                        grossSales = calc.grossSales,
                        netSales = calc.netSales,
                        estimatedProfit = calc.estimatedProfit,
                        totalProductCost = calc.totalProductCost,
                        paymentStatus = calc.resolvedStatus,
                        amountPaid = order.amountPaid,
                        outstandingBalance = calc.outstandingBalance,
                        remarks = order.remarks
                    )
                    val lines = order.lines.mapIndexed { i, line ->
                        TransactionLineItem(
                            transactionId = txId,
                            productId = line.productId ?: 0L,
                            productName = line.productName,
                            quantity = line.quantity,
                            unitSellingPrice = line.unitPrice,
                            unitCost = inputs[i].unitCost,
                            lineSubtotal = SalesCalculator.roundTo2Decimals(line.quantity * line.unitPrice)
                        )
                    }
                    repository.saveTransaction(
                        transaction = tx,
                        lineItems = lines,
                        initialPaymentAmount = order.amountPaid,
                        paymentMethod = order.paymentMethod
                    )
                    imported++
                }
                // Upload the new orders (and any customers created for them); offline is fine.
                try { performCloudSync(silent = true) } catch (_: Exception) {}
                clearImport()
                snackbarMessage.value = "Imported $imported order${if (imported == 1) "" else "s"}."
                navigateTo(Screen.TRANSACTIONS)
            } catch (e: Exception) {
                importError.value = "Import stopped: ${e.message ?: "unexpected error"}. Orders already imported were kept."
            } finally {
                isImporting.value = false
            }
        }
    }

    // --- Quotations Management ---
    val showNewQuotationDialog = MutableStateFlow(false)
    val editingQuotation = MutableStateFlow<com.amacezing.sales.model.QuotationWithItems?>(null)
    val activePdfDialog = MutableStateFlow<GeneratedPdfState?>(null)

    fun createQuotation(
        businessType: BusinessType,
        customerName: String,
        customerPhone: String = "",
        customerEmail: String = "",
        customerAddress: String = "",
        quotationDateMillis: Long = System.currentTimeMillis(),
        validityDays: Int = 7,
        lineItems: List<com.amacezing.sales.model.QuotationLineItem>,
        discountType: DiscountType = DiscountType.PERCENTAGE,
        discountValue: Double = 0.0,
        remarks: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val quotationNumber = repository.generateQuotationNumber(quotationDateMillis)
            val grossTotal = lineItems.sumOf { it.lineSubtotal }
            val discountAmount = when (discountType) {
                DiscountType.PERCENTAGE -> (grossTotal * discountValue / 100.0).coerceAtLeast(0.0)
                DiscountType.FIXED -> discountValue.coerceAtMost(grossTotal).coerceAtLeast(0.0)
            }
            val netTotal = (grossTotal - discountAmount).coerceAtLeast(0.0)
            val validUntil = quotationDateMillis + (validityDays.toLong() * 24 * 60 * 60 * 1000)

            val quotation = com.amacezing.sales.model.Quotation(
                quotationNumber = quotationNumber,
                businessType = businessType,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerEmail = customerEmail.trim(),
                customerAddress = customerAddress.trim(),
                quotationDateEpochMillis = quotationDateMillis,
                validityDays = validityDays,
                validUntilEpochMillis = validUntil,
                discountType = discountType,
                discountValue = discountValue,
                grossTotal = grossTotal,
                discountAmount = discountAmount,
                netTotal = netTotal,
                status = com.amacezing.sales.model.QuotationStatus.DRAFT,
                remarks = remarks.trim()
            )

            val id = repository.saveQuotation(quotation, lineItems)
            tryPushQuotation(id)
            snackbarMessage.value = "Quotation $quotationNumber created successfully!"
            showNewQuotationDialog.value = false
            onComplete(id)
        }
    }

    fun updateQuotation(
        existing: com.amacezing.sales.model.Quotation,
        businessType: BusinessType,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        validityDays: Int,
        lineItems: List<com.amacezing.sales.model.QuotationLineItem>,
        discountType: DiscountType,
        discountValue: Double,
        remarks: String
    ) {
        viewModelScope.launch {
            val grossTotal = lineItems.sumOf { it.lineSubtotal }
            val discountAmount = when (discountType) {
                DiscountType.PERCENTAGE -> (grossTotal * discountValue / 100.0).coerceAtLeast(0.0)
                DiscountType.FIXED -> discountValue.coerceAtMost(grossTotal).coerceAtLeast(0.0)
            }
            val updated = existing.copy(
                businessType = businessType,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerEmail = customerEmail.trim(),
                customerAddress = customerAddress.trim(),
                validityDays = validityDays,
                validUntilEpochMillis = existing.quotationDateEpochMillis + validityDays.toLong() * 24 * 60 * 60 * 1000,
                discountType = discountType,
                discountValue = discountValue,
                grossTotal = grossTotal,
                discountAmount = discountAmount,
                netTotal = (grossTotal - discountAmount).coerceAtLeast(0.0),
                remarks = remarks.trim(),
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            repository.updateQuotation(updated, lineItems)
            tryPushQuotation(updated.id)
            snackbarMessage.value = "Quotation ${existing.quotationNumber} updated"
            editingQuotation.value = null
        }
    }

    fun deleteQuotation(id: Long) {
        viewModelScope.launch {
            val number = repository.getQuotationSnapshot(id)?.quotation?.quotationNumber
            repository.deleteQuotation(id)
            if (number != null) {
                try {
                    firestoreRepo.deleteQuotation(
                        com.amacezing.sales.data.firebase.FirestoreMappers.cloudQuotationId(number)
                    )
                } catch (_: Exception) {
                    // Offline: it stays in the cloud until deleted again while online.
                }
            }
            snackbarMessage.value = "Quotation deleted"
        }
    }

    fun updateQuotationStatus(id: Long, status: com.amacezing.sales.model.QuotationStatus) {
        viewModelScope.launch {
            repository.updateQuotationStatus(id, status)
            tryPushQuotation(id)
            snackbarMessage.value = "Quotation status updated to ${status.displayName}"
        }
    }

    fun convertQuotationToOrder(
        quotationWithItems: com.amacezing.sales.model.QuotationWithItems,
        initialPayment: Double = 0.0,
        paymentMethod: String = "Cash",
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val q = quotationWithItems.quotation
            val items = quotationWithItems.lineItems

            // Quotation lines don't store a cost, so take it from the catalog product (by id, then by
            // name within the business). Without it every converted order showed 100% profit.
            val catalog = repository.getProductsSnapshot()
            val convertedLineItems = items.map { item ->
                val product = catalog.firstOrNull { it.id == item.productId }
                    ?: catalog.firstOrNull {
                        it.businessType == q.businessType && it.name.equals(item.productName.trim(), ignoreCase = true)
                    }
                LineItemInput(
                    quantity = item.quantity,
                    unitSellingPrice = item.unitPrice,
                    unitCost = product?.unitCost ?: 0.0
                )
            }
            val productInfos = items.map { item ->
                (item.productId ?: 0L) to item.productName
            }

            createTransaction(
                businessType = q.businessType,
                customerName = q.customerName,
                orderDateMillis = System.currentTimeMillis(),
                lineItems = convertedLineItems,
                productInfos = productInfos,
                discountType = q.discountType,
                discountValue = q.discountValue,
                initialPayment = initialPayment,
                paymentMethod = paymentMethod,
                paymentNotes = "Converted from quotation ${q.quotationNumber}",
                remarks = "Converted from quotation ${q.quotationNumber}. ${q.remarks}".trim()
            )

            repository.markQuotationConverted(q.id, q.quotationNumber)
            tryPushQuotation(q.id)
            snackbarMessage.value = "Quotation ${q.quotationNumber} successfully converted to official Order!"
            onComplete(q.quotationNumber)
        }
    }
}

data class GeneratedPdfState(
    val file: java.io.File,
    val title: String,
    val documentNumber: String
)
