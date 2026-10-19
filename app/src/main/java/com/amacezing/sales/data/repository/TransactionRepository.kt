package com.amacezing.sales.data.repository

import com.amacezing.sales.data.firebase.FirestoreTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Sales Transactions in Firestore.
 * Preserves sales calculations, profit, payment tracking, and line items summary.
 */
interface TransactionRepository {
    fun observeTransactions(): Flow<List<FirestoreTransaction>>
    suspend fun getTransactions(): List<FirestoreTransaction>
    suspend fun getTransactionById(transactionId: String): FirestoreTransaction?
    suspend fun saveTransaction(tx: FirestoreTransaction)
    suspend fun updateTransaction(tx: FirestoreTransaction)
    suspend fun deleteTransaction(transactionId: String)
}
