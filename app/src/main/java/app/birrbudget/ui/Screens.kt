package app.birrbudget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.birrbudget.calendar.CalendarSystem
import app.birrbudget.calendar.DateText
import app.birrbudget.data.TransactionWithCategory
import app.birrbudget.util.Money
import java.time.LocalDate

@Composable
fun HomeScreen(vm: BirrViewModel, modifier: Modifier = Modifier) {
    val month by vm.month.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val breakdown by vm.expenseBreakdown.collectAsStateWithLifecycle()
    val amharic by vm.settings.amharic.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            MonthHeader(
                label = month.label(amharic),
                onPrev = { vm.shiftMonth(-1) },
                onNext = { vm.shiftMonth(1) },
                onToday = { vm.goToToday() },
            )
        }
        item { SummaryCard(summary) }
        item { SectionTitle("Where your money went") }
        if (breakdown.isEmpty()) {
            item { EmptyHint("No expenses in this month yet. Tap + to add one.") }
        } else {
            val largest = breakdown.first().cents.toFloat()
            itemsIndexed(breakdown) { _, row ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("${row.emoji} ${row.name}", modifier = Modifier.weight(1f))
                        Text(Money.number(row.cents), fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { if (largest > 0f) row.cents / largest else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    vm: BirrViewModel,
    onEdit: (TransactionWithCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val month by vm.month.collectAsStateWithLifecycle()
    val txs by vm.monthTransactions.collectAsStateWithLifecycle()
    val system by vm.settings.calendar.collectAsStateWithLifecycle()
    val amharic by vm.settings.amharic.collectAsStateWithLifecycle()
    val grouped = remember(txs) { txs.groupBy { it.epochDay }.entries.toList() }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            MonthHeader(
                label = month.label(amharic),
                onPrev = { vm.shiftMonth(-1) },
                onNext = { vm.shiftMonth(1) },
                onToday = { vm.goToToday() },
            )
        }
        if (grouped.isEmpty()) {
            item { EmptyHint("Nothing recorded in this month.") }
        }
        grouped.forEach { (day, rows) ->
            item(key = "day-$day") {
                val date = LocalDate.ofEpochDay(day)
                val net = rows.sumOf { if (it.isIncome) it.amountCents else -it.amountCents }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = DateText.weekdayName(date, system, amharic) + " · " +
                            DateText.format(date, system, amharic),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = (if (net < 0) "−" else "") + Money.number(Math.abs(net)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(rows, key = { it.id }) { row -> TransactionRow(row) { onEdit(row) } }
        }
    }
}

@Composable
fun BudgetsScreen(
    vm: BirrViewModel,
    onEditBudget: (BudgetProgress?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val month by vm.month.collectAsStateWithLifecycle()
    val progress by vm.budgetProgress.collectAsStateWithLifecycle()
    val amharic by vm.settings.amharic.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            MonthHeader(
                label = month.label(amharic),
                onPrev = { vm.shiftMonth(-1) },
                onNext = { vm.shiftMonth(1) },
                onToday = { vm.goToToday() },
            )
        }
        item {
            OutlinedButton(
                onClick = { onEditBudget(null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) { Text("Add monthly budget") }
        }
        if (progress.isEmpty()) {
            item { EmptyHint("No budgets yet. Set a monthly limit for a category to track it here.") }
        }
        items(progress, key = { it.category.id }) { row ->
            val over = row.spentCents > row.limitCents
            val fraction = if (row.limitCents > 0) row.spentCents.toFloat() / row.limitCents else 0f
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onEditBudget(row) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${row.category.emoji} ${row.category.name}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${Money.number(row.spentCents)} / ${Money.number(row.limitCents)}",
                            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    Text(
                        text = if (over) {
                            "Over by ${Money.number(row.spentCents - row.limitCents)}"
                        } else {
                            "${Money.number(row.limitCents - row.spentCents)} left"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    vm: BirrViewModel,
    onAddCategory: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val system by vm.settings.calendar.collectAsStateWithLifecycle()
    val amharic by vm.settings.amharic.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        SectionTitle("Calendar")
        CalendarOption(
            label = "Ethiopian calendar (የኢትዮጵያ ዘመን አቆጣጠር)",
            selected = system == CalendarSystem.ETHIOPIAN,
            onSelect = { vm.settings.setCalendar(CalendarSystem.ETHIOPIAN) },
        )
        CalendarOption(
            label = "Gregorian calendar",
            selected = system == CalendarSystem.GREGORIAN,
            onSelect = { vm.settings.setCalendar(CalendarSystem.GREGORIAN) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Amharic month and day names", modifier = Modifier.weight(1f))
            Switch(checked = amharic, onCheckedChange = { vm.settings.setAmharic(it) })
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SectionTitle("Data")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onAddCategory) { Text("Add category") }
            OutlinedButton(onClick = onExport) { Text("Export all transactions (CSV)") }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SectionTitle("About")
        Text(
            text = "Birr Budget 0.1.0\n" +
                "Your data stays on this phone. Dates are stored as Gregorian days, so you can " +
                "switch calendars at any time without losing or changing anything. " +
                "The Ethiopian calendar has 13 months: twelve of 30 days plus Pagume (5 days, or 6 in a leap year).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun CalendarOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
