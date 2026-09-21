package com.example.govsalary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.govsalary.data.db.SalaryHistoryDao
import com.example.govsalary.data.model.SalaryHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

sealed class ImportState {
    object Idle : ImportState()
    data class Preview(val parsedData: List<SalaryHistoryEntity>, val existingConflicts: Int) : ImportState()
    data class Error(val message: String) : ImportState()
    object Success : ImportState()
}

class ImportViewModel(private val dao: SalaryHistoryDao) : ViewModel() {
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    fun previewCsvImport(context: Context, uri: Uri, startYear: Int? = null, endYear: Int? = null) {
        viewModelScope.launch {
            try {
                val parsedList = mutableListOf<SalaryHistoryEntity>()
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val lines = reader.readLines()
                    if (lines.size <= 1) {
                        _importState.value = ImportState.Error("Файл пуст или содержит только заголовок")
                        return@launch
                    }

                    for (i in 1 until lines.size) {
                        val line = lines[i]
                        if (line.isBlank()) continue
                        val tokens = line.split(";", ",").map { it.trim().replace(",", ".") }
                        
                        if (tokens.size >= 11) {
                            val year = tokens[0].toIntOrNull() ?: continue
                            val month = tokens[1].toIntOrNull() ?: continue
                            
                            // Фильтрация
                            if (startYear != null && year < startYear) continue
                            if (endYear != null && year > endYear) continue
                            if (month !in 1..12) continue

                            val base = tokens[2].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rank = tokens[3].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val incentive = tokens[4].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val secret = tokens[5].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val conditions = tokens[6].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val experience = tokens[7].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val bonus = tokens[8].toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val workDays = tokens[9].toIntOrNull() ?: 21
                            val actualDays = tokens[10].toIntOrNull() ?: workDays

                            val allowances = base * (incentive + secret + conditions + experience)
                            var gross = base + rank + allowances + bonus
                            
                            val workRatio = if (workDays > 0) BigDecimal(actualDays.toDouble() / workDays) else BigDecimal.ONE
                            gross *= workRatio
                            
                            val net = gross - (gross * BigDecimal("0.13"))

                            parsedList.add(
                                SalaryHistoryEntity(
                                    year = year,
                                    month = month,
                                    baseSalary = base,
                                    rankSalary = rank,
                                    netAmount = net.setScale(2, RoundingMode.HALF_EVEN)
                                )
                            )
                        }
                    }
                }

                if (parsedList.isEmpty()) {
                    _importState.value = ImportState.Error("Не удалось распознать данные.")
                } else {
                    val existingData = dao.getAllHistoryList()
                    val conflicts = parsedList.count { parsed ->
                        existingData.any { it.year == parsed.year && it.month == parsed.month }
                    }
                    _importState.value = ImportState.Preview(parsedList, conflicts)
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Ошибка: ${e.localizedMessage}")
            }
        }
    }

    fun applyImport(data: List<SalaryHistoryEntity>, overwrite: Boolean) {
        viewModelScope.launch {
            try {
                data.forEach { entity ->
                    if (overwrite) {
                        dao.insert(entity)
                    } else {
                        dao.insertIgnore(entity)
                    }
                }
                _importState.value = ImportState.Success
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Ошибка сохранения: ${e.localizedMessage}")
            }
        }
    }
}

class ImportViewModelFactory(private val dao: SalaryHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImportViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
