package com.amacezing.sales.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.amacezing.sales.model.BusinessProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {

    @Query("SELECT * FROM business_profile WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String = "active_profile"): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileSnapshot(id: String = "active_profile"): BusinessProfile?

    @Query("SELECT * FROM business_profile WHERE userId = :userId LIMIT 1")
    suspend fun getProfileByUserId(userId: String): BusinessProfile?

    @Query("SELECT COUNT(*) FROM business_profile")
    suspend fun countProfiles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: BusinessProfile)

    @Update
    suspend fun update(profile: BusinessProfile)

    @Query("DELETE FROM business_profile WHERE id = :id")
    suspend fun deleteProfile(id: String = "active_profile")
}
