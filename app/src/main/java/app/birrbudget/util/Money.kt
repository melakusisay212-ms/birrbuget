package app.birrbudget.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Amounts are stored as Long cents (santim). 1 Birr = 100 santim. */
object Money {
    private fun formatter() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    /** "1,234.50" without a currency prefix, for compact layouts. */
    fun number(cents: Long): String = formatter().format(BigDecimal(cents).movePointLeft(2))

    fun format(cents: Long): String = "ETB " + number(cents)

    /** Plain "1234.50" form used in text fields and CSV. */
    fun plain(cents: Long): String = BigDecimal(cents).movePointLeft(2).setScale(2).toPlainString()

    /** Parses user input like "1,250.5" into cents; returns null when empty, invalid or not positive. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            if (value.signum() <= 0) null else value.movePointRight(2).longValueExact()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }
}
