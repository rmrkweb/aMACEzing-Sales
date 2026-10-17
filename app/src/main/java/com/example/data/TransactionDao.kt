package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.PaymentRecord
import com.example.model.SalesTransaction
import com.example.model.TransactionLineItem
import com.example.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY orderDateEpochMillis DESC, createdAt DESC")
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionWithDetailsById(id: String): Flow<TransactionWithDetails?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): SalesTransaction?

    @Query("SELECT orderNumber FROM transactions")
    suspend fun getAllOrderNumbers(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SalesTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<SalesTransaction>)

    @Update
    suspend fun updateTransaction(transaction: SalesTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: SalesTransaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<TransactionLineItem>)

    @Query("DELETE FROM line_items WHERE transactionId = :transactionId")
    suspend fun deleteLineItemsByTransactionId(transactionId: String)

    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId")
    suspend fun getLineItemsByTransactionId(transactionId: String): List<TransactionLineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentRecord>)

    @Query("SELECT * FROM payments WHERE transactionId = :transactionId ORDER BY paymentDateEpochMillis ASC")
    fun getPaymentsForTransaction(transactionId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE transactionId = :transactionId")
    suspend fun getPaymentsListForTransaction(transactionId: String): List<PaymentRecord>

    @Query("DELETE FROM payments WHERE transactionId = :transactionId")
    suspend fun deletePaymentsByTransactionId(transactionId: String)

    @Transaction
    suspend fun saveFullTransaction(
        transaction: SalesTransaction,
        lineItems: List<TransactionLineItem>,
        initialPayment: PaymentRecord? = null
    ) {
        insertTransaction(transaction)
        deleteLineItemsByTransactionId(transaction.id)
        if (lineItems.isNotEmpty()) {
            val mappedItems = lineItems.map { it.copy(transactionId = transaction.id) }
            insertLineItems(mappedItems)
        }
        if (initialPayment != null) {
            insertPayment(initialPayment.copy(transactionId = transaction.id))
        }
    }
}
