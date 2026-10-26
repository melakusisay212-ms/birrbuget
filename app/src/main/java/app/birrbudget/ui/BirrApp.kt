package app.birrbudget.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.birrbudget.data.TransactionWithCategory

private data class AppTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    AppTab("Home", Icons.Default.Home),
    AppTab("Transactions", Icons.AutoMirrored.Filled.List),
    AppTab("Budgets", Icons.Default.ShoppingCart),
    AppTab("Settings", Icons.Default.Settings),
)

@Composable
fun BirrApp(vm: BirrViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    var editorOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionWithCategory?>(null) }

    var budgetDialogOpen by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetProgress?>(null) }

    var categoryDialogOpen by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val system by vm.settings.calendar.collectAsStateWithLifecycle()
    val amharic by vm.settings.amharic.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> if (uri != null) vm.exportCsv(uri) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        editorOpen = true
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Add transaction") }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(vm, content)
            1 -> TransactionsScreen(
                vm = vm,
                onEdit = {
                    editingTransaction = it
                    editorOpen = true
                },
                modifier = content,
            )
            2 -> BudgetsScreen(
                vm = vm,
                onEditBudget = {
                    editingBudget = it
                    budgetDialogOpen = true
                },
                modifier = content,
            )
            else -> SettingsScreen(
                vm = vm,
                onAddCategory = { categoryDialogOpen = true },
                onExport = { exportLauncher.launch("birr-budget-export.csv") },
                modifier = content,
            )
        }
    }

    if (editorOpen) {
        TransactionEditorDialog(
            existing = editingTransaction,
            categories = categories,
            system = system,
            amharic = amharic,
            onSave = { id, amountCents, isIncome, categoryId, note, date ->
                vm.saveTransaction(id, amountCents, isIncome, categoryId, note, date)
                editorOpen = false
            },
            onDelete = { id ->
                vm.deleteTransaction(id)
                editorOpen = false
            },
            onDismiss = { editorOpen = false },
        )
    }

    if (budgetDialogOpen) {
        BudgetDialog(
            expenseCategories = categories.filter { !it.isIncome },
            existing = editingBudget,
            onSave = { categoryId, limitCents ->
                vm.saveBudget(categoryId, limitCents)
                budgetDialogOpen = false
            },
            onDelete = { categoryId ->
                vm.deleteBudget(categoryId)
                budgetDialogOpen = false
            },
            onDismiss = { budgetDialogOpen = false },
        )
    }

    if (categoryDialogOpen) {
        AddCategoryDialog(
            onSave = { name, emoji, isIncome ->
                vm.addCategory(name, emoji, isIncome)
                categoryDialogOpen = false
            },
            onDismiss = { categoryDialogOpen = false },
        )
    }
}
