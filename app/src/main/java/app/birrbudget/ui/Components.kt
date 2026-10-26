package app.birrbudget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.birrbudget.data.TransactionWithCategory
import app.birrbudget.util.Money

@Composable
fun MonthHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
        }
        TextButton(onClick = onToday) { Text("Today") }
    }
}

@Composable
fun SummaryCard(summary: MonthSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Amounts in ETB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryItem("Income", Money.number(summary.incomeCents), incomeColor())
                SummaryItem("Expense", Money.number(summary.expenseCents), MaterialTheme.colorScheme.error)
                SummaryItem(
                    "Balance",
                    (if (summary.balanceCents < 0) "−" else "") + Money.number(Math.abs(summary.balanceCents)),
                    MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RowScope.SummaryItem(title: String, value: String, color: Color) {
    Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun TransactionRow(t: TransactionWithCategory, onClick: () -> Unit) {
    val supporting: (@Composable () -> Unit)? =
        if (t.note.isNotBlank()) {
            ({ Text(t.note, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        } else {
            null
        }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Text(t.categoryEmoji, style = MaterialTheme.typography.titleLarge) },
        headlineContent = { Text(t.categoryName) },
        supportingContent = supporting,
        trailingContent = {
            Text(
                text = (if (t.isIncome) "+" else "−") + Money.number(t.amountCents),
                color = if (t.isIncome) incomeColor() else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}
