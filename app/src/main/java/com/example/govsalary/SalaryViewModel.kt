package com.example.govsalary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.govsalary.data.db.SalaryHistoryDao
import com.example.govsalary.data.model.SalaryHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalaryViewModel(private val dao: SalaryHistoryDao) : ViewModel() {
    val history = dao.getAllHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun saveCalculation(entity: SalaryHistoryEntity) {
        viewModelScope.launch {
            dao.insert(entity)
        }
    }
}

class SalaryViewModelFactory(private val dao: SalaryHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalaryViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
