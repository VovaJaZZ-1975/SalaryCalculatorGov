package com.example.govsalary.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.govsalary.data.model.SalaryHistoryEntity

@Database(entities = [SalaryHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salaryHistoryDao(): SalaryHistoryDao
}
