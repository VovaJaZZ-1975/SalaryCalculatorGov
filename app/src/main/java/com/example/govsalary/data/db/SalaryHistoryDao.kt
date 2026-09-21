package com.example.govsalary.data.db

import androidx.room.*
import com.example.govsalary.data.model.SalaryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryHistoryDao {
    @Query("SELECT * FROM salary_history ORDER BY year DESC, month DESC")
    fun getAllHistory(): Flow<List<SalaryHistoryEntity>>

    @Query("SELECT * FROM salary_history")
    suspend fun getAllHistoryList(): List<SalaryHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SalaryHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(history: SalaryHistoryEntity)
}
