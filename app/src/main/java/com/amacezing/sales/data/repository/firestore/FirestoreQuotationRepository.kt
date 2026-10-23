package com.amacezing.sales.data.repository.firestore

import com.amacezing.sales.data.firebase.FirestoreQuotation
import com.amacezing.sales.data.firebase.OperationType
import com.amacezing.sales.data.firebase.handleFirestoreError
import com.amacezing.sales.data.repository.QuotationRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirestoreQuotationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : QuotationRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override suspend fun getQuotations(): List<FirestoreQuotation> {
        val uid = requireUserId()
        val path = "users/$uid/quotations"
        return try {
            db.collection("users").document(uid).collection("quotations").get().await()
                .toObjects(FirestoreQuotation::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    override suspend fun saveQuotation(quotation: FirestoreQuotation) {
        val uid = requireUserId()
        val path = "users/$uid/quotations/${quotation.id}"
        try {
            val payload = mapOf(
                "id" to quotation.id,
                "userId" to uid,
                "quotationNumber" to quotation.quotationNumber,
                "businessType" to quotation.businessType,
                "customerName" to quotation.customerName,
                "customerPhone" to quotation.customerPhone,
                "customerEmail" to quotation.customerEmail,
                "customerAddress" to quotation.customerAddress,
                "quotationDateEpochMillis" to quotation.quotationDateEpochMillis,
                "validityDays" to quotation.validityDays,
                "validUntilEpochMillis" to quotation.validUntilEpochMillis,
                "discountType" to quotation.discountType,
                "discountValue" to quotation.discountValue,
                "grossTotal" to quotation.grossTotal,
                "discountAmount" to quotation.discountAmount,
                "netTotal" to quotation.netTotal,
                "status" to quotation.status,
                "remarks" to quotation.remarks,
                "convertedTransactionId" to quotation.convertedTransactionId,
                "createdAtEpochMillis" to quotation.createdAtEpochMillis,
                "updatedAtEpochMillis" to quotation.updatedAtEpochMillis,
                "lineItems" to quotation.lineItems.map {
                    mapOf(
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "quantity" to it.quantity,
                        "unitPrice" to it.unitPrice,
                        "lineSubtotal" to it.lineSubtotal,
                        "notes" to it.notes
                    )
                }
            )
            db.collection("users").document(uid).collection("quotations").document(quotation.id)
                .set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    override suspend fun deleteQuotation(quotationId: String) {
        val uid = requireUserId()
        val path = "users/$uid/quotations/$quotationId"
        try {
            db.collection("users").document(uid).collection("quotations").document(quotationId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
