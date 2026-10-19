package com.amacezing.sales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.amacezing.sales.data.firebase.FirestoreSalesRepository
import com.amacezing.sales.model.Customer
import com.amacezing.sales.model.Product
import com.amacezing.sales.model.SalesTransaction
import com.amacezing.sales.ui.auth.AuthScreen
import com.amacezing.sales.ui.auth.signOut
import com.amacezing.sales.ui.components.ConfirmDeleteDialog
import com.amacezing.sales.ui.components.CustomerDialog
import com.amacezing.sales.ui.components.PaymentDialog
import com.amacezing.sales.ui.components.ProductDialog
import com.amacezing.sales.ui.components.TransactionDetailDialog
import com.amacezing.sales.ui.components.TransactionDialog
import com.amacezing.sales.ui.navigation.Screen
import com.amacezing.sales.ui.screens.CustomersScreen
import com.amacezing.sales.ui.screens.DashboardScreen
import com.amacezing.sales.ui.screens.ImportExportScreen
import com.amacezing.sales.ui.screens.ProductsScreen
import com.amacezing.sales.ui.screens.ProfileScreen
import com.amacezing.sales.ui.screens.ReportsScreen
import com.amacezing.sales.ui.screens.SettingsScreen
import com.amacezing.sales.ui.screens.TransactionsScreen
import com.amacezing.sales.ui.theme.MyApplicationTheme
import com.amacezing.sales.ui.theme.StatusPaid
import com.amacezing.sales.viewmodel.SalesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SalesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        com.amacezing.sales.util.FirebaseInitHelper.initFirebaseIfEmpty(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: SalesViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        com.amacezing.sales.update.AutoUpdatePrompt()
        MainAppScaffold(
            viewModel = viewModel,
            currentUser = currentUser!!,
            onSignOut = {
                signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = { currentUser = null },
                    scope = coroutineScope
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: SalesViewModel,
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val allProducts by viewModel.allProducts.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Dialog state collections
    val showNewTxDialog by viewModel.showNewTransactionDialog.collectAsState()
    val editingTx by viewModel.editingTransaction.collectAsState()
    val viewingTx by viewModel.viewingTransaction.collectAsState()
    val recordingPaymentTx by viewModel.recordingPaymentTransaction.collectAsState()
    val showCustDialog by viewModel.showCustomerDialog.collectAsState()
    val editingCust by viewModel.editingCustomer.collectAsState()
    val showProdDialog by viewModel.showProductDialog.collectAsState()
    val editingProd by viewModel.editingProduct.collectAsState()
    val itemToDelete by viewModel.itemToDelete.collectAsState()

    // Hardware / Gesture back handler
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        viewModel.handleBack()
    }

    // Save profile to Cloud Firestore on session start
    LaunchedEffect(currentUser.uid) {
        try {
            val firestoreRepo = FirestoreSalesRepository(context)
            firestoreRepo.saveUserProfile()
        } catch (_: Exception) {
            // Profile sync can be retried later if offline.
        }
        // Restore cloud backup before the user manually syncs an empty fresh installation.
        viewModel.restoreFromCloudIfLocalEmpty()
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.amacezing.sales.R.drawable.amacezing_logo),
                            contentDescription = "aMACEzing Printing Services logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "SALES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Connected",
                            tint = StatusPaid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.IMPORT_EXPORT) },
                        modifier = Modifier.testTag("top_nav_import_export")
                    ) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = "Excel Migration",
                            tint = if (currentScreen == Screen.IMPORT_EXPORT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.PROFILE) },
                        modifier = Modifier.testTag("top_nav_profile")
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = "Business Profile & Branding",
                            tint = if (currentScreen == Screen.PROFILE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                        modifier = Modifier.testTag("top_nav_settings")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentScreen == Screen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("top_nav_sign_out")
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val primaryScreens = listOf(
                    Screen.DASHBOARD,
                    Screen.TRANSACTIONS,
                    Screen.CUSTOMERS,
                    Screen.PRODUCTS,
                    Screen.REPORTS
                )

                primaryScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title, fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.testTag("bottom_nav_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    Screen.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                    Screen.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                    Screen.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                    Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
                    Screen.IMPORT_EXPORT -> ImportExportScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    Screen.PROFILE -> ProfileScreen(viewModel = viewModel, currentUser = currentUser)
                }
            }
        }
    }

    // Modal Dialogs
    if (showNewTxDialog) {
        TransactionDialog(
            initialTransaction = null,
            availableProducts = allProducts,
            existingCustomers = allCustomers,
            sintraMarginPercent = settings.sintraDefaultMarginPercent,
            sintraUseCostCalculation = settings.sintraUseCostCalculation,
            isProfitMasked = settings.isProfitMasked,
            onSaveNew = { biz, cust, date, items, pInfo, dType, dVal, initPaid, pMethod, pNotes, rem ->
                viewModel.createTransaction(
                    businessType = biz,
                    customerName = cust,
                    orderDateMillis = date,
                    lineItems = items,
                    productInfos = pInfo,
                    discountType = dType,
                    discountValue = dVal,
                    initialPayment = initPaid,
                    paymentMethod = pMethod,
                    paymentNotes = pNotes,
                    remarks = rem
                )
            },
            onSaveEdit = { _, _, _, _, _, _, _ -> },
            onDismiss = { viewModel.showNewTransactionDialog.value = false }
        )
    }

    editingTx?.let { txToEdit ->
        TransactionDialog(
            initialTransaction = txToEdit,
            isDuplicateMode = false,
            availableProducts = allProducts,
            existingCustomers = allCustomers,
            sintraMarginPercent = settings.sintraDefaultMarginPercent,
            sintraUseCostCalculation = settings.sintraUseCostCalculation,
            isProfitMasked = settings.isProfitMasked,
            onSaveNew = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onSaveEdit = { original, items, pInfo, dType, dVal, rem, stOverride ->
                viewModel.updateTransaction(
                    tx = original.transaction,
                    lineItems = items,
                    productInfos = pInfo,
                    discountType = dType,
                    discountValue = dVal,
                    remarks = rem,
                    statusOverride = stOverride
                )
            },
            onDismiss = { viewModel.editingTransaction.value = null }
        )
    }

    viewingTx?.let { txToView ->
        TransactionDetailDialog(
            transactionWithDetails = txToView,
            isProfitMasked = settings.isProfitMasked,
            onDismiss = { viewModel.viewingTransaction.value = null },
            onEdit = {
                viewModel.viewingTransaction.value = null
                viewModel.editingTransaction.value = txToView
            },
            onDuplicate = {
                viewModel.viewingTransaction.value = null
                viewModel.createTransaction(
                    businessType = txToView.transaction.businessType,
                    customerName = txToView.transaction.customerName,
                    orderDateMillis = System.currentTimeMillis(),
                    lineItems = txToView.lineItems.map { com.amacezing.sales.util.LineItemInput(it.quantity, it.unitSellingPrice, it.unitCost) },
                    productInfos = txToView.lineItems.map { Pair(it.productId, it.productName) },
                    discountType = txToView.transaction.discountType,
                    discountValue = txToView.transaction.discountPercentage,
                    initialPayment = 0.0,
                    paymentMethod = "Cash",
                    paymentNotes = "",
                    remarks = "Duplicated from ${txToView.transaction.orderNumber}"
                )
            },
            onRecordPayment = {
                viewModel.viewingTransaction.value = null
                viewModel.recordingPaymentTransaction.value = txToView
            },
            onDelete = {
                viewModel.viewingTransaction.value = null
                viewModel.itemToDelete.value = txToView.transaction
            }
        )
    }

    recordingPaymentTx?.let { txForPayment ->
        PaymentDialog(
            transactionWithDetails = txForPayment,
            onRecordPayment = { amt, method, ref, notes ->
                viewModel.recordPayment(txForPayment.transaction.id, amt, method, ref, notes)
            },
            onDismiss = { viewModel.recordingPaymentTransaction.value = null }
        )
    }

    if (showCustDialog) {
        CustomerDialog(
            initialCustomer = editingCust,
            onSave = { viewModel.saveCustomer(it) },
            onDismiss = {
                viewModel.showCustomerDialog.value = false
                viewModel.editingCustomer.value = null
            }
        )
    }

    if (showProdDialog) {
        ProductDialog(
            initialProduct = editingProd,
            onSave = { viewModel.saveProduct(it) },
            onDismiss = {
                viewModel.showProductDialog.value = false
                viewModel.editingProduct.value = null
            }
        )
    }

    itemToDelete?.let { item ->
        when (item) {
            is SalesTransaction -> {
                ConfirmDeleteDialog(
                    title = "Delete Order ${item.orderNumber}",
                    message = "Are you sure you want to permanently delete order ${item.orderNumber} for ${item.customerName}? This will remove all associated line items and payment records.",
                    onConfirm = { viewModel.deleteTransaction(item.id) },
                    onDismiss = { viewModel.itemToDelete.value = null }
                )
            }
            is Product -> {
                ConfirmDeleteDialog(
                    title = "Delete Product",
                    message = "Are you sure you want to remove '${item.name}' from the catalog? Past historical transactions will remain unaffected.",
                    onConfirm = { viewModel.deleteProduct(item) },
                    onDismiss = { viewModel.itemToDelete.value = null }
                )
            }
            is Customer -> {
                ConfirmDeleteDialog(
                    title = "Delete Customer",
                    message = "Are you sure you want to remove '${item.name}' from the directory?",
                    onConfirm = { viewModel.deleteCustomer(item) },
                    onDismiss = { viewModel.itemToDelete.value = null }
                )
            }
        }
    }
}
