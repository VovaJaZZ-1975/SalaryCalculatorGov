package com.example.govsalary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "salary_history")
data class SalaryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val month: Int,
    val baseSalary: BigDecimal,
    val rankSalary: BigDecimal,
    val netAmount: BigDecimal
)
