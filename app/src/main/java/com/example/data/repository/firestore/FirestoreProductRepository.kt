package com.example.data.repository.firestore

import com.example.data.firebase.FirestoreProduct
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.repository.ProductRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreProductRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : ProductRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeProducts(): Flow<List<FirestoreProduct>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/products"
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreProduct::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    override suspend fun getProducts(): List<FirestoreProduct> {
        val uid = requireUserId()
        val path = "users/$uid/products"
        return try {
            val snapshot = db.collection("users").document(uid).collection("products").get().await()
            snapshot.toObjects(FirestoreProduct::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    override suspend fun getProductById(productId: String): FirestoreProduct? {
        val uid = requireUserId()
        val path = "users/$uid/products/$productId"
        return try {
            val doc = db.collection("users").document(uid).collection("products").document(productId).get().await()
            doc.toObject(FirestoreProduct::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    override suspend fun saveProduct(product: FirestoreProduct) {
        val uid = requireUserId()
        val docId = if (product.id.isNotBlank()) product.id else db.collection("users").document(uid).collection("products").document().id
        val path = "users/$uid/products/$docId"
        val docRef = db.collection("users").document(uid).collection("products").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "name" to product.name,
                "businessType" to product.businessType,
                "sellingPrice" to product.sellingPrice,
                "unitCost" to product.unitCost,
                "isActive" to product.isActive,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    override suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        val path = "users/$uid/products/$productId"
        try {
            db.collection("users").document(uid).collection("products").document(productId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
