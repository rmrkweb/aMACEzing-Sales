package com.amacezing.sales.data.repository.firestore

import com.amacezing.sales.data.firebase.FirestoreSettings
import com.amacezing.sales.data.firebase.OperationType
import com.amacezing.sales.data.firebase.handleFirestoreError
import com.amacezing.sales.data.repository.SettingsRepository
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

class FirestoreSettingsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : SettingsRepository {

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeSettings(): Flow<FirestoreSettings?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/settings/default"
        emitAll(
            db.collection("users").document(uid).collection("settings").document("default")
                .snapshots()
                .map { snapshot -> snapshot.toObject(FirestoreSettings::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    override suspend fun getSettings(): FirestoreSettings? {
        val uid = requireUserId()
        val path = "users/$uid/settings/default"
        return try {
            val doc = db.collection("users").document(uid).collection("settings").document("default").get().await()
            doc.toObject(FirestoreSettings::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    override suspend fun saveSettings(settings: FirestoreSettings) {
        val uid = requireUserId()
        val path = "users/$uid/settings/default"
        val docRef = db.collection("users").document(uid).collection("settings").document("default")
        try {
            val payload = mapOf(
                "id" to "default",
                "userId" to uid,
                "businessNameEngraving" to settings.businessNameEngraving,
                "businessNameSintra" to settings.businessNameSintra,
                "engravingOrderPrefix" to settings.engravingOrderPrefix,
                "sintraOrderPrefix" to settings.sintraOrderPrefix,
                "sintraDefaultMarginPercent" to settings.sintraDefaultMarginPercent,
                "sintraUseCostCalculation" to settings.sintraUseCostCalculation,
                "currencySymbol" to settings.currencySymbol,
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
