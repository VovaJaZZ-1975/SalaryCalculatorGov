package com.example.govsalary

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.govsalary.data.model.SalaryHistoryEntity
import java.math.BigDecimal
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GovSalaryApp
        val factory = SalaryViewModelFactory(app.database.salaryHistoryDao())
        
        setContent {
            MaterialTheme {
                val viewModel: SalaryViewModel = viewModel(factory = factory)
                MainScreen(viewModel)
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Дашборд", Icons.Default.Home)
    object Calculator : BottomNavItem("calculator", "Расчет", Icons.Default.DateRange)
    object History : BottomNavItem("history", "История", Icons.Default.List)
}

@Composable
fun MainScreen(viewModel: SalaryViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Calculator.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) { DashboardScreen(viewModel) }
            composable(BottomNavItem.Calculator.route) { SalaryCalculatorScreen(viewModel) }
            composable(BottomNavItem.History.route) { HistoryScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Calculator,
        BottomNavItem.History
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SalaryViewModel) {
    val historyList by viewModel.history.collectAsState()
    
    val totalNet = historyList.sumOf { it.netAmount.toDouble() }
    val avgNet = if (historyList.isNotEmpty()) totalNet / historyList.size else 0.0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Мой Доход", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        
        DashboardCard("Всего заработано", "${"%.2f".format(totalNet)} ₽", Icons.Default.CheckCircle)
        Spacer(modifier = Modifier.height(16.dp))
        
        DashboardCard("Средний доход в месяц", "${"%.2f".format(avgNet)} ₽", Icons.Default.Info)
        Spacer(modifier = Modifier.height(16.dp))
        
        DashboardCard("Сохранено периодов", "${historyList.size} мес.", Icons.Default.DateRange)
    }
}

@Composable
fun DashboardCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: SalaryViewModel) {
    val historyList by viewModel.history.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { parseCsvAndSave(context, it, viewModel) }
        }
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ретроспектива", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Формат CSV:", fontWeight = FontWeight.Bold)
                Text("Год;Месяц;Оклад;Чин;НаРуки", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { launcher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить из CSV")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Text("История пуста. Загрузите данные или сохраните расчет в Калькуляторе.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyList) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${String.format("%02d", item.month)}.${item.year}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Оклад: ${item.baseSalary} ₽ | Чин: ${item.rankSalary} ₽")
                            Text("На руки: ${item.netAmount} ₽", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun parseCsvAndSave(context: Context, uri: Uri, viewModel: SalaryViewModel) {
    try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val lines = reader.readLines()
            if (lines.size <= 1) {
                Toast.makeText(context, "Файл пуст или содержит только заголовок", Toast.LENGTH_SHORT).show()
                return
            }
            var successCount = 0
            for (i in 1 until lines.size) {
                val line = lines[i]
                val tokens = line.split(";", ",").map { it.trim().replace(",", ".") } // Поддержка разделителей
                if (tokens.size >= 5) {
                    val year = tokens[0].toIntOrNull() ?: continue
                    val month = tokens[1].toIntOrNull() ?: continue
                    val base = tokens[2].toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val rank = tokens[3].toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val net = tokens[4].toBigDecimalOrNull() ?: BigDecimal.ZERO
                    
                    viewModel.saveCalculation(SalaryHistoryEntity(year = year, month = month, baseSalary = base, rankSalary = rank, netAmount = net))
                    successCount++
                }
            }
            Toast.makeText(context, "Успешно загружено записей: $successCount", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка чтения CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SalaryCalculatorScreen(viewModel: SalaryViewModel) {
    val context = LocalContext.current
    var baseSalary by remember { mutableStateOf("24296") }
    var rankSalary by remember { mutableStateOf("13853") }
    var incentiveRatio by remember { mutableStateOf("0.9") }
    var secretRatio by remember { mutableStateOf("0.1") }
    var conditionsRatio by remember { mutableStateOf("1.2") }
    var experienceRatio by remember { mutableStateOf("0.1") }
    var bonus by remember { mutableStateOf("0") }

    val base = baseSalary.toDoubleOrNull() ?: 0.0
    val rank = rankSalary.toDoubleOrNull() ?: 0.0
    val iRatio = incentiveRatio.toDoubleOrNull() ?: 0.0
    val sRatio = secretRatio.toDoubleOrNull() ?: 0.0
    val cRatio = conditionsRatio.toDoubleOrNull() ?: 0.0
    val eRatio = experienceRatio.toDoubleOrNull() ?: 0.0
    val bns = bonus.toDoubleOrNull() ?: 0.0

    val incentive = base * iRatio
    val secret = base * sRatio
    val conditions = base * cRatio
    val experience = base * eRatio
    
    val gross = base + rank + incentive + secret + conditions + experience + bns
    val tax = gross * 0.13
    val net = gross - tax

    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Калькулятор ЗП", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        InputField("Должностной оклад", baseSalary, Icons.Default.Person) { baseSalary = it }
        InputField("Классный чин", rankSalary, Icons.Default.Star) { rankSalary = it }
        InputField("Поощрение (коэф)", incentiveRatio, Icons.Default.ThumbUp) { incentiveRatio = it }
        InputField("Гостайна (коэф)", secretRatio, Icons.Default.Lock) { secretRatio = it }
        InputField("Особые условия (коэф)", conditionsRatio, Icons.Default.Info) { conditionsRatio = it }
        InputField("Выслуга лет (коэф)", experienceRatio, Icons.Default.DateRange) { experienceRatio = it }
        InputField("Премия", bonus, Icons.Default.Favorite) { bonus = it }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        ResultRow("Начислено (Gross):", gross)
        ResultRow("НДФЛ (13%):", tax)
        Text("Итого на руки: ${"%.2f".format(net)} ₽", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val cal = Calendar.getInstance()
                val entity = SalaryHistoryEntity(
                    year = cal.get(Calendar.YEAR),
                    month = cal.get(Calendar.MONTH) + 1,
                    baseSalary = BigDecimal(base),
                    rankSalary = BigDecimal(rank),
                    netAmount = BigDecimal(net)
                )
                viewModel.saveCalculation(entity)
                Toast.makeText(context, "Расчет успешно сохранен", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Сохранить расчет")
        }
    }
}

@Composable
fun InputField(label: String, value: String, icon: ImageVector, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
fun ResultRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("${"%.2f".format(amount)} ₽", style = MaterialTheme.typography.bodyLarge)
    }
}
