package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.repository.BusinessRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.data.repository.firestore.FirestoreBusinessRepository
import com.example.data.repository.firestore.FirestoreCustomerRepository
import com.example.data.repository.firestore.FirestoreProductRepository
import com.example.data.repository.firestore.FirestoreSettingsRepository
import com.example.data.repository.firestore.FirestoreTransactionRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreSalesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : BusinessRepository,
    ProductRepository,
    CustomerRepository,
    TransactionRepository,
    SettingsRepository {

    // Sub-repositories for clean modular access
    val businessRepo: BusinessRepository = FirestoreBusinessRepository(db, auth)
    val productRepo: ProductRepository = FirestoreProductRepository(db, auth)
    val customerRepo: CustomerRepository = FirestoreCustomerRepository(db, auth)
    val transactionRepo: TransactionRepository = FirestoreTransactionRepository(db, auth)
    val settingsRepo: SettingsRepository = FirestoreSettingsRepository(db, auth)

    // CRITICAL MANDATE: Always initialize with context.getString(R.string.firestore_database_id)
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    suspend fun saveUserProfile() {
        val uid = requireUserId()
        val user = auth.currentUser ?: return
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        try {
            val payload = mapOf(
                "uid" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    // --- Business Delegation ---
    override fun observeBusinesses(): Flow<List<FirestoreBusiness>> = businessRepo.observeBusinesses()
    override suspend fun getBusinesses(): List<FirestoreBusiness> = businessRepo.getBusinesses()
    override suspend fun getBusinessById(businessId: String): FirestoreBusiness? = businessRepo.getBusinessById(businessId)
    override suspend fun saveBusiness(business: FirestoreBusiness) = businessRepo.saveBusiness(business)
    override suspend fun deleteBusiness(businessId: String) = businessRepo.deleteBusiness(businessId)
    override suspend fun seedDefaultBusinessesIfEmpty() = businessRepo.seedDefaultBusinessesIfEmpty()

    // --- Product Delegation ---
    override fun observeProducts(): Flow<List<FirestoreProduct>> = productRepo.observeProducts()
    override suspend fun getProducts(): List<FirestoreProduct> = productRepo.getProducts()
    override suspend fun getProductById(productId: String): FirestoreProduct? = productRepo.getProductById(productId)
    override suspend fun saveProduct(product: FirestoreProduct) = productRepo.saveProduct(product)
    override suspend fun deleteProduct(productId: String) = productRepo.deleteProduct(productId)

    // --- Customer Delegation ---
    override fun observeCustomers(): Flow<List<FirestoreCustomer>> = customerRepo.observeCustomers()
    override suspend fun getCustomers(): List<FirestoreCustomer> = customerRepo.getCustomers()
    override suspend fun getCustomerById(customerId: String): FirestoreCustomer? = customerRepo.getCustomerById(customerId)
    override suspend fun saveCustomer(customer: FirestoreCustomer) = customerRepo.saveCustomer(customer)
    override suspend fun deleteCustomer(customerId: String) = customerRepo.deleteCustomer(customerId)

    // --- Transaction Delegation ---
    override fun observeTransactions(): Flow<List<FirestoreTransaction>> = transactionRepo.observeTransactions()
    override suspend fun getTransactions(): List<FirestoreTransaction> = transactionRepo.getTransactions()
    override suspend fun getTransactionById(transactionId: String): FirestoreTransaction? = transactionRepo.getTransactionById(transactionId)
    override suspend fun saveTransaction(tx: FirestoreTransaction) = transactionRepo.saveTransaction(tx)
    override suspend fun updateTransaction(tx: FirestoreTransaction) = transactionRepo.updateTransaction(tx)
    override suspend fun deleteTransaction(transactionId: String) = transactionRepo.deleteTransaction(transactionId)

    // --- Settings Delegation ---
    override fun observeSettings(): Flow<FirestoreSettings?> = settingsRepo.observeSettings()
    override suspend fun getSettings(): FirestoreSettings? = settingsRepo.getSettings()
    override suspend fun saveSettings(settings: FirestoreSettings) = settingsRepo.saveSettings(settings)
}
