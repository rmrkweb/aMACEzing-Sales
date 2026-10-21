package com.amacezing.sales.data.repository.firestore

import com.amacezing.sales.data.firebase.FirestoreTransaction
import com.amacezing.sales.data.firebase.OperationType
import com.amacezing.sales.data.firebase.handleFirestoreError
import com.amacezing.sales.data.repository.TransactionRepository
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

class FirestoreTransactionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : TransactionRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeTransactions(): Flow<List<FirestoreTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreTransaction::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    override suspend fun getTransactions(): List<FirestoreTransaction> {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        return try {
            val snapshot = db.collection("users").document(uid).collection("transactions").get().await()
            snapshot.toObjects(FirestoreTransaction::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    override suspend fun getTransactionById(transactionId: String): FirestoreTransaction? {
        val uid = requireUserId()
        val path = "users/$uid/transactions/$transactionId"
        return try {
            val doc = db.collection("users").document(uid).collection("transactions").document(transactionId).get().await()
            doc.toObject(FirestoreTransaction::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    override suspend fun saveTransaction(tx: FirestoreTransaction) {
        val uid = requireUserId()
        val path = "users/$uid/transactions/${tx.id}"
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        try {
            val payload = mapOf(
                "id" to tx.id,
                "userId" to uid,
                "orderNumber" to tx.orderNumber,
                "businessType" to tx.businessType,
                "customerName" to tx.customerName,
                "orderDateEpochMillis" to tx.orderDateEpochMillis,
                "discountType" to tx.discountType,
                "discountPercentage" to tx.discountPercentage,
                "discountAmount" to tx.discountAmount,
                "grossSales" to tx.grossSales,
                "netSales" to tx.netSales,
                "totalProductCost" to tx.totalProductCost,
                "estimatedProfit" to tx.estimatedProfit,
                "paymentStatus" to tx.paymentStatus,
                "amountPaid" to tx.amountPaid,
                "outstandingBalance" to tx.outstandingBalance,
                "paymentNotes" to tx.paymentNotes,
                "remarks" to tx.remarks,
                "lineItemsSummary" to tx.lineItemsSummary,
                "lineItems" to tx.lineItems.map {
                    mapOf(
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "quantity" to it.quantity,
                        "unitSellingPrice" to it.unitSellingPrice,
                        "unitCost" to it.unitCost,
                        "lineSubtotal" to it.lineSubtotal
                    )
                },
                "payments" to tx.payments.map {
                    mapOf(
                        "amount" to it.amount,
                        "paymentDateEpochMillis" to it.paymentDateEpochMillis,
                        "paymentMethod" to it.paymentMethod,
                        "referenceNumber" to it.referenceNumber,
                        "notes" to it.notes
                    )
                },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    override suspend fun updateTransaction(tx: FirestoreTransaction) {
        saveTransaction(tx)
    }

    override suspend fun deleteTransaction(transactionId: String) {
        val uid = requireUserId()
        val path = "users/$uid/transactions/$transactionId"
        try {
            db.collection("users").document(uid).collection("transactions").document(transactionId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
