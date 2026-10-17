package com.example.data.repository

import com.example.data.firebase.FirestoreSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing System Settings in Firestore.
 */
interface SettingsRepository {
    fun observeSettings(): Flow<FirestoreSettings?>
    suspend fun getSettings(): FirestoreSettings?
    suspend fun saveSettings(settings: FirestoreSettings)
}
