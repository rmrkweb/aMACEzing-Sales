package com.example.data.repository

import com.example.data.firebase.FirestoreBusiness
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Business profile records in Firestore.
 * Supports Engraving and Sintra Board businesses.
 */
interface BusinessRepository {
    fun observeBusinesses(): Flow<List<FirestoreBusiness>>
    suspend fun getBusinesses(): List<FirestoreBusiness>
    suspend fun getBusinessById(businessId: String): FirestoreBusiness?
    suspend fun saveBusiness(business: FirestoreBusiness)
    suspend fun deleteBusiness(businessId: String)
    suspend fun seedDefaultBusinessesIfEmpty()
}
