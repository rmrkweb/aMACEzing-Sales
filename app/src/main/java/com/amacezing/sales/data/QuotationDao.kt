package com.amacezing.sales.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.model.Quotation
import com.amacezing.sales.model.QuotationLineItem
import com.amacezing.sales.model.QuotationStatus
import com.amacezing.sales.model.QuotationWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotationDao {

    @Transaction
    @Query("SELECT * FROM quotations ORDER BY quotationDateEpochMillis DESC, id DESC")
    fun getAllQuotations(): Flow<List<QuotationWithItems>>

    @Transaction
    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    fun getQuotationById(id: Long): Flow<QuotationWithItems?>

    @Transaction
    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    suspend fun getQuotationSnapshot(id: Long): QuotationWithItems?

    @Transaction
    @Query("SELECT * FROM quotations WHERE businessType = :businessType ORDER BY quotationDateEpochMillis DESC")
    fun getQuotationsByBusinessType(businessType: BusinessType): Flow<List<QuotationWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: Quotation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<QuotationLineItem>)

    @Update
    suspend fun updateQuotation(quotation: Quotation)

    @Delete
    suspend fun deleteQuotation(quotation: Quotation)

    @Query("DELETE FROM quotation_line_items WHERE quotationId = :quotationId")
    suspend fun deleteLineItemsFor(quotationId: Long)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteQuotationById(id: Long)

    @Query("UPDATE quotations SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: QuotationStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE quotations SET status = 'CONVERTED', convertedTransactionId = :transactionId, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun markConverted(id: Long, transactionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM quotations")
    suspend fun countQuotations(): Int

    @Query("SELECT quotationNumber FROM quotations")
    suspend fun getAllQuotationNumbers(): List<String>

    @Query("SELECT quotationNumber FROM quotations ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuotationNumber(): String?
}
