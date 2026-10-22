package com.amacezing.sales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amacezing.sales.model.Business
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY sortOrder ASC, name ASC")
    fun getAllBusinessesFlow(): Flow<List<Business>>

    @Query("SELECT * FROM businesses ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllBusinesses(): List<Business>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: String): Business?

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(business: Business)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(businesses: List<Business>)
}
