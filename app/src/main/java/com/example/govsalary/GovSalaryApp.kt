package com.example.govsalary

import android.app.Application
import androidx.room.Room
import com.example.govsalary.data.db.AppDatabase

class GovSalaryApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "gov_salary_db"
        ).build()
    }
}
