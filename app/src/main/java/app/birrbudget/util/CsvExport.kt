package app.birrbudget.util

import app.birrbudget.calendar.EthiopianCalendar
import app.birrbudget.data.TransactionWithCategory
import java.time.LocalDate
import java.util.Locale

object CsvExport {
    private const val HEADER = "date_gregorian,date_ethiopian,type,category,amount_etb,note\n"

    fun build(rows: List<TransactionWithCategory>): String {
        val sb = StringBuilder(HEADER)
        for (row in rows) {
            val date = LocalDate.ofEpochDay(row.epochDay)
            val eth = EthiopianCalendar.fromLocalDate(date)
            val ethText = String.format(Locale.US, "%d-%02d-%02d", eth.year, eth.month, eth.day)
            sb.append(date.toString()).append(',')
                .append(ethText).append(',')
                .append(if (row.isIncome) "income" else "expense").append(',')
                .append(escape(row.categoryName)).append(',')
                .append(Money.plain(row.amountCents)).append(',')
                .append(escape(row.note)).append('\n')
        }
        return sb.toString()
    }

    /** Quotes fields per RFC 4180 and neutralises spreadsheet formula injection (=, +, -, @). */
    internal fun escape(value: String): String {
        var text = value
        if (text.isNotEmpty() && text[0] in "=+-@") text = "'$text"
        val needsQuotes = text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + text.replace("\"", "\"\"") + "\"" else text
    }
}
