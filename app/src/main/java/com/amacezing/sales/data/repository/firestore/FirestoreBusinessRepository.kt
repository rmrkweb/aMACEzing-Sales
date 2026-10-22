package com.amacezing.sales.data.repository.firestore

import com.amacezing.sales.data.firebase.FirestoreBusiness
import com.amacezing.sales.data.firebase.OperationType
import com.amacezing.sales.data.firebase.handleFirestoreError
import com.amacezing.sales.data.repository.BusinessRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreBusinessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : BusinessRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeBusinesses(): Flow<List<FirestoreBusiness>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/businesses"
        emitAll(
            db.collection("users").document(uid).collection("businesses")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreBusiness::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    override suspend fun getBusinesses(): List<FirestoreBusiness> {
        val uid = requireUserId()
        val path = "users/$uid/businesses"
        return try {
            val snapshot = db.collection("users").document(uid).collection("businesses").get().await()
            snapshot.toObjects(FirestoreBusiness::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    override suspend fun getBusinessById(businessId: String): FirestoreBusiness? {
        val uid = requireUserId()
        val path = "users/$uid/businesses/$businessId"
        return try {
            val doc = db.collection("users").document(uid).collection("businesses").document(businessId).get().await()
            doc.toObject(FirestoreBusiness::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    override suspend fun saveBusiness(business: FirestoreBusiness) {
        val uid = requireUserId()
        val docId = if (business.id.isNotBlank()) business.id else business.type.lowercase()
        val path = "users/$uid/businesses/$docId"
        val docRef = db.collection("users").document(uid).collection("businesses").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "name" to business.name,
                "type" to business.type,
                "orderPrefix" to business.orderPrefix,
                "colorHex" to business.colorHex,
                "profitMethod" to business.profitMethod,
                "marginPercent" to business.marginPercent,
                "useUnitCostIfAvailable" to business.useUnitCostIfAvailable,
                "isActive" to business.isActive,
                "sortOrder" to business.sortOrder,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    override suspend fun deleteBusiness(businessId: String) {
        val uid = requireUserId()
        val path = "users/$uid/businesses/$businessId"
        try {
            db.collection("users").document(uid).collection("businesses").document(businessId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    override suspend fun seedDefaultBusinessesIfEmpty() {
        val existing = getBusinesses()
        if (existing.isEmpty()) {
            val uid = requireUserId()
            saveBusiness(
                FirestoreBusiness(
                    id = "engraving",
                    userId = uid,
                    name = "Engraving Business",
                    type = "ENGRAVING",
                    orderPrefix = "ENG"
                )
            )
            saveBusiness(
                FirestoreBusiness(
                    id = "sintra_board",
                    userId = uid,
                    name = "Sintra Board Printing Business",
                    type = "SINTRA_BOARD",
                    orderPrefix = "SNT"
                )
            )
        }
    }
}
