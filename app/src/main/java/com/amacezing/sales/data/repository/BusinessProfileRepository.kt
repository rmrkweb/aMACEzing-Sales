package com.amacezing.sales.data.repository

import com.amacezing.sales.data.firebase.FirestoreBusinessProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the active BusinessProfile in Cloud Firestore.
 */
interface BusinessProfileRepository {
    fun observeBusinessProfile(): Flow<FirestoreBusinessProfile?>
    suspend fun getBusinessProfile(): FirestoreBusinessProfile?
    suspend fun saveBusinessProfile(profile: FirestoreBusinessProfile)
}
