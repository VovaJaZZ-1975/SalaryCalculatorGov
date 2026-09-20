package com.example.govsalary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SalaryCalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun SalaryCalculatorScreen() {
    // Значения по умолчанию из таблицы "Расчет ЗП (2026).xlsx"
    var baseSalary by remember { mutableStateOf("24296") }
    var rankSalary by remember { mutableStateOf("13853") }
    var incentiveRatio by remember { mutableStateOf("0.9") }
    var secretRatio by remember { mutableStateOf("0.1") }
    var conditionsRatio by remember { mutableStateOf("1.2") }
    var experienceRatio by remember { mutableStateOf("0.1") }
    var bonus by remember { mutableStateOf("0") }

    // Конвертация текста в числа для расчетов
    val base = baseSalary.toDoubleOrNull() ?: 0.0
    val rank = rankSalary.toDoubleOrNull() ?: 0.0
    val iRatio = incentiveRatio.toDoubleOrNull() ?: 0.0
    val sRatio = secretRatio.toDoubleOrNull() ?: 0.0
    val cRatio = conditionsRatio.toDoubleOrNull() ?: 0.0
    val eRatio = experienceRatio.toDoubleOrNull() ?: 0.0
    val bns = bonus.toDoubleOrNull() ?: 0.0

    // Математика надбавок
    val incentive = base * iRatio
    val secret = base * sRatio
    val conditions = base * cRatio
    val experience = base * eRatio
    
    // Итоги
    val gross = base + rank + incentive + secret + conditions + experience + bns
    val tax = gross * 0.13
    val net = gross - tax

    // UI разметка экрана
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Расчет ЗП Госслужащего", 
            style = MaterialTheme.typography.headlineMedium, 
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        InputField("Должностной оклад (руб)", baseSalary) { baseSalary = it }
        InputField("Оклад по классному чину (руб)", rankSalary) { rankSalary = it }
        InputField("Ежемесячное поощрение (коэф)", incentiveRatio) { incentiveRatio = it }
        InputField("За гостайну (коэф)", secretRatio) { secretRatio = it }
        InputField("За особые условия (коэф)", conditionsRatio) { conditionsRatio = it }
        InputField("За выслугу лет (коэф)", experienceRatio) { experienceRatio = it }
        InputField("Премия (руб)", bonus) { bonus = it }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        ResultRow("Начислено (Gross):", gross)
        ResultRow("НДФЛ (13%):", tax)
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Итого на руки: ${"%.2f".format(net)} ₽", 
            style = MaterialTheme.typography.headlineSmall, 
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
fun ResultRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("${"%.2f".format(amount)} ₽", style = MaterialTheme.typography.bodyLarge)
    }
}
