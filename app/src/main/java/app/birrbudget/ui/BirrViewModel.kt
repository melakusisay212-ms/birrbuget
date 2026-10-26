package app.birrbudget.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.birrbudget.calendar.MonthRef
import app.birrbudget.data.BirrDatabase
import app.birrbudget.data.BudgetEntity
import app.birrbudget.data.CategoryEntity
import app.birrbudget.data.SettingsStore
import app.birrbudget.data.TransactionEntity
import app.birrbudget.data.TransactionWithCategory
import app.birrbudget.util.CsvExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MonthSummary(val incomeCents: Long, val expenseCents: Long) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class CategoryTotal(val emoji: String, val name: String, val cents: Long)

data class BudgetProgress(val category: CategoryEntity, val limitCents: Long, val spentCents: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class BirrViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BirrDatabase.get(application).dao()
    val settings = SettingsStore(application)

    private fun <T> kotlinx.coroutines.flow.Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val categories: StateFlow<List<CategoryEntity>> = dao.categories().asState(emptyList())

    private val _month = MutableStateFlow(MonthRef.of(settings.calendar.value, LocalDate.now()))
    val month: StateFlow<MonthRef> = _month.asStateFlow()

    val monthTransactions: StateFlow<List<TransactionWithCategory>> = _month
        .flatMapLatest { m ->
            val range = m.range()
            dao.transactionsBetween(range.start.toEpochDay(), range.endInclusive.toEpochDay())
        }
        .asState(emptyList())

    val summary: StateFlow<MonthSummary> = monthTransactions
        .map { list ->
            MonthSummary(
                incomeCents = list.filter { it.isIncome }.sumOf { it.amountCents },
                expenseCents = list.filter { !it.isIncome }.sumOf { it.amountCents },
            )
        }
        .asState(MonthSummary(0L, 0L))

    val expenseBreakdown: StateFlow<List<CategoryTotal>> = monthTransactions
        .map { list ->
            list.filter { !it.isIncome }
                .groupBy { it.categoryId }
                .map { (_, rows) ->
                    CategoryTotal(rows.first().categoryEmoji, rows.first().categoryName, rows.sumOf { it.amountCents })
                }
                .sortedByDescending { it.cents }
        }
        .asState(emptyList())

    val budgetProgress: StateFlow<List<BudgetProgress>> =
        combine(dao.budgets(), categories, monthTransactions) { budgets, cats, txs ->
            val byId = cats.associateBy { it.id }
            val spent = txs.filter { !it.isIncome }
                .groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountCents } }
            budgets.mapNotNull { budget ->
                byId[budget.categoryId]?.let { cat ->
                    BudgetProgress(cat, budget.limitCents, spent[budget.categoryId] ?: 0L)
                }
            }.sortedBy { it.category.name }
        }.asState(emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        // When the calendar setting changes, keep showing the period that contains today
        // (or the start of the month being viewed) in the new calendar.
        viewModelScope.launch {
            settings.calendar.collect { system ->
                val current = _month.value
                if (current.system != system) {
                    val range = current.range()
                    val today = LocalDate.now()
                    val anchor =
                        if (!today.isBefore(range.start) && !today.isAfter(range.endInclusive)) today else range.start
                    _month.value = MonthRef.of(system, anchor)
                }
            }
        }
    }

    fun shiftMonth(delta: Int) {
        _month.value = _month.value.plusMonths(delta)
    }

    fun goToToday() {
        _month.value = MonthRef.of(settings.calendar.value, LocalDate.now())
    }

    fun saveTransaction(
        id: Long,
        amountCents: Long,
        isIncome: Boolean,
        categoryId: Long,
        note: String,
        date: LocalDate,
    ) {
        viewModelScope.launch {
            dao.upsertTransaction(
                TransactionEntity(
                    id = id,
                    amountCents = amountCents,
                    isIncome = isIncome,
                    categoryId = categoryId,
                    note = note.trim(),
                    epochDay = date.toEpochDay(),
                ),
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { dao.deleteTransaction(id) }
    }

    fun saveBudget(categoryId: Long, limitCents: Long) {
        viewModelScope.launch { dao.upsertBudget(BudgetEntity(categoryId = categoryId, limitCents = limitCents)) }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch { dao.deleteBudgetFor(categoryId) }
    }

    fun addCategory(name: String, emoji: String, isIncome: Boolean) {
        viewModelScope.launch {
            dao.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    emoji = emoji.trim().ifEmpty { "🏷️" },
                    isIncome = isIncome,
                ),
            )
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val csv = CsvExport.build(dao.allTransactions())
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("Could not open the file for writing")
                    stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
            }
            _messages.tryEmit(if (result.isSuccess) "CSV exported" else "Export failed")
        }
    }
}
