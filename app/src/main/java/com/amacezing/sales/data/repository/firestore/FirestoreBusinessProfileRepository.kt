package com.amacezing.sales.data.repository.firestore

import com.amacezing.sales.data.firebase.FirestoreBusinessProfile
import com.amacezing.sales.data.firebase.OperationType
import com.amacezing.sales.data.firebase.handleFirestoreError
import com.amacezing.sales.data.repository.BusinessProfileRepository
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

class FirestoreBusinessProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : BusinessProfileRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeBusinessProfile(): Flow<FirestoreBusinessProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/business_profile/active"
        emitAll(
            db.collection("users").document(uid).collection("business_profile").document("active")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(FirestoreBusinessProfile::class.java) else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    override suspend fun getBusinessProfile(): FirestoreBusinessProfile? {
        val uid = requireUserId()
        val path = "users/$uid/business_profile/active"
        return try {
            val doc = db.collection("users").document(uid).collection("business_profile").document("active").get().await()
            if (doc.exists()) doc.toObject(FirestoreBusinessProfile::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    override suspend fun saveBusinessProfile(profile: FirestoreBusinessProfile) {
        val uid = requireUserId()
        val docId = if (profile.id.isNotBlank()) profile.id else "active"
        val path = "users/$uid/business_profile/$docId"
        val docRef = db.collection("users").document(uid).collection("business_profile").document(docId)
        try {
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "businessName" to profile.businessName,
                "tagline" to profile.tagline,
                "ownerName" to profile.ownerName,
                "contactNumber" to profile.contactNumber,
                "contactEmail" to profile.contactEmail,
                "address" to profile.address,
                "facebookPage" to profile.facebookPage,
                "website" to profile.website,
                "tin" to profile.tin,
                "documentFooter" to profile.documentFooter,
                "logoUri" to profile.logoUri,
                "currency" to profile.currency,
                "quotationValidityDays" to profile.quotationValidityDays,
                "paymentInstructions" to profile.paymentInstructions,
                "quotationTerms" to profile.quotationTerms,
                "receiptFooter" to profile.receiptFooter,
                "showLogoOnDocuments" to profile.showLogoOnDocuments,
                "showContactOnDocuments" to profile.showContactOnDocuments,
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
}
