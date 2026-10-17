package com.example.data.repository.firestore

import com.example.data.firebase.FirestoreCustomer
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.repository.CustomerRepository
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

class FirestoreCustomerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : CustomerRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeCustomers(): Flow<List<FirestoreCustomer>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreCustomer::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    override suspend fun getCustomers(): List<FirestoreCustomer> {
        val uid = requireUserId()
        val path = "users/$uid/customers"
        return try {
            val snapshot = db.collection("users").document(uid).collection("customers").get().await()
            snapshot.toObjects(FirestoreCustomer::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    override suspend fun getCustomerById(customerId: String): FirestoreCustomer? {
        val uid = requireUserId()
        val path = "users/$uid/customers/$customerId"
        return try {
            val doc = db.collection("users").document(uid).collection("customers").document(customerId).get().await()
            doc.toObject(FirestoreCustomer::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    override suspend fun saveCustomer(customer: FirestoreCustomer) {
        val uid = requireUserId()
        val docId = if (customer.id.isNotBlank()) customer.id else db.collection("users").document(uid).collection("customers").document().id
        val path = "users/$uid/customers/$docId"
        val docRef = db.collection("users").document(uid).collection("customers").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "name" to customer.name,
                "phone" to customer.phone,
                "email" to customer.email,
                "notes" to customer.notes,
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

    override suspend fun deleteCustomer(customerId: String) {
        val uid = requireUserId()
        val path = "users/$uid/customers/$customerId"
        try {
            db.collection("users").document(uid).collection("customers").document(customerId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
