package app.birrbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.birrbudget.calendar.CalendarSystem
import app.birrbudget.calendar.DateText
import app.birrbudget.calendar.MonthRef
import app.birrbudget.data.CategoryEntity
import app.birrbudget.data.TransactionWithCategory
import app.birrbudget.util.Money
import java.time.LocalDate

// ---------------------------------------------------------------------------------------------
// Transaction editor (add + edit)
// ---------------------------------------------------------------------------------------------

@Composable
fun TransactionEditorDialog(
    existing: TransactionWithCategory?,
    categories: List<CategoryEntity>,
    system: CalendarSystem,
    amharic: Boolean,
    onSave: (id: Long, amountCents: Long, isIncome: Boolean, categoryId: Long, note: String, date: LocalDate) -> Unit,
    onDelete: (id: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var isIncome by remember { mutableStateOf(existing?.isIncome ?: false) }
    var amountText by remember { mutableStateOf(existing?.let { Money.plain(it.amountCents) } ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember {
        mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now())
    }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val visibleCategories = categories.filter { it.isIncome == isIncome }
    val errorText = error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add transaction" else "Edit transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = {
                            isIncome = false
                            categoryId = null
                        },
                        label = { Text("Expense") },
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = {
                            isIncome = true
                            categoryId = null
                        },
                        label = { Text("Income") },
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Amount (ETB)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleCategories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = {
                                categoryId = category.id
                                error = null
                            },
                            label = { Text("${category.emoji} ${category.name}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showPicker = true }) {
                    Text(DateText.format(date, system, amharic))
                }
                if (errorText != null) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = Money.parse(amountText)
                    val chosen = categoryId
                    when {
                        cents == null -> error = "Enter an amount above 0"
                        chosen == null -> error = "Pick a category"
                        else -> onSave(existing?.id ?: 0L, cents, isIncome, chosen, note, date)
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing.id) }) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (showPicker) {
        BirrDatePickerDialog(
            initial = date,
            system = system,
            amharic = amharic,
            onDismiss = { showPicker = false },
            onPick = {
                date = it
                showPicker = false
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Date picker that works for both calendars (Material's picker is Gregorian-only)
// ---------------------------------------------------------------------------------------------

@Composable
fun BirrDatePickerDialog(
    initial: LocalDate,
    system: CalendarSystem,
    amharic: Boolean,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    var shown by remember { mutableStateOf(MonthRef.of(system, initial)) }
    var selected by remember { mutableStateOf(initial) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(selected) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shown = shown.plusMonths(-1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = shown.label(amharic),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { shown = shown.plusMonths(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (column in 0..6) {
                        Text(
                            text = DateText.weekdayShort(column, system, amharic),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                val range = shown.range()
                val leadingBlanks = DateText.columnOf(range.start)
                val days = generateSequence(range.start) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(range.endInclusive) }
                    .toList()
                val cells: List<LocalDate?> = List<LocalDate?>(leadingBlanks) { null } + days

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            DayCell(
                                label = day?.let { DateText.dayNumber(it, system).toString() } ?: "",
                                enabled = day != null,
                                isSelected = day != null && day == selected,
                                isToday = day != null && day == today,
                                onClick = { if (day != null) selected = day },
                            )
                        }
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }

                TextButton(
                    onClick = {
                        selected = today
                        shown = MonthRef.of(system, today)
                    },
                ) { Text("Today") }
            }
        },
    )
}

@Composable
private fun RowScope.DayCell(
    label: String,
    enabled: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Budget dialog
// ---------------------------------------------------------------------------------------------

@Composable
fun BudgetDialog(
    expenseCategories: List<CategoryEntity>,
    existing: BudgetProgress?,
    onSave: (categoryId: Long, limitCents: Long) -> Unit,
    onDelete: (categoryId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryId by remember { mutableStateOf(existing?.category?.id) }
    var amountText by remember { mutableStateOf(existing?.let { Money.plain(it.limitCents) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val errorText = error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add monthly budget" else "Edit budget") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (existing == null) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expenseCategories, key = { it.id }) { category ->
                            FilterChip(
                                selected = categoryId == category.id,
                                onClick = {
                                    categoryId = category.id
                                    error = null
                                },
                                label = { Text("${category.emoji} ${category.name}") },
                            )
                        }
                    }
                } else {
                    Text("${existing.category.emoji} ${existing.category.name}")
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Monthly limit (ETB)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = Money.parse(amountText)
                    val chosen = categoryId
                    when {
                        chosen == null -> error = "Pick a category"
                        cents == null -> error = "Enter an amount above 0"
                        else -> onSave(chosen, cents)
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing.category.id) }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Add category dialog
// ---------------------------------------------------------------------------------------------

@Composable
fun AddCategoryDialog(
    onSave: (name: String, emoji: String, isIncome: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val errorText = error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Expense") })
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Income") })
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) error = "Enter a name" else onSave(name, emoji, isIncome)
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
