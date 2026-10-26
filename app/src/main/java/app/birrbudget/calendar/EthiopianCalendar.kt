package app.birrbudget.calendar

import java.time.LocalDate

/** A date in the Ethiopian (Amete Mihret) calendar: 12 months of 30 days + Pagume (5 or 6 days). */
data class EthiopianDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(month in 1..13) { "month must be 1..13, was $month" }
        require(day in 1..EthiopianCalendar.daysInMonth(year, month)) {
            "day $day is out of range for $year-$month"
        }
    }
}

/**
 * Pure conversion between Gregorian [LocalDate] and [EthiopianDate] using Julian Day Numbers.
 * No Android dependencies, so it is fully unit-testable on the JVM.
 *
 * The algorithm was cross-checked against known dates (Enkutatash, Genna, Timket, Adwa,
 * Pagume 6) and a full round-trip of every day from 1900 to 2100.
 * Note: after Gregorian 2100 (not a leap year) Enkutatash moves from Sept 11/12 to Sept 12/13;
 * the formula handles this correctly because it works on day counts.
 */
object EthiopianCalendar {
    private const val EPOCH = 1_723_856L
    private const val EPOCH_DAY_TO_JDN = 2_440_588L // JDN of 1970-01-01

    val monthNamesEn = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit", "Megabit",
        "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume",
    )
    val monthNamesAm = listOf(
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
        "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜ",
    )

    // Index 0 = Sunday.
    val weekdayNamesEn = listOf("Ehud", "Segno", "Maksegno", "Rob", "Hamus", "Arb", "Kidame")
    val weekdayNamesAm = listOf("እሑድ", "ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "ዓርብ", "ቅዳሜ")
    val weekdayShortEn = listOf("Ehu", "Seg", "Mak", "Rob", "Ham", "Arb", "Kid")
    val weekdayShortAm = listOf("እሑ", "ሰኞ", "ማክ", "ረቡ", "ሐሙ", "ዓር", "ቅዳ")

    fun monthName(month: Int, amharic: Boolean): String =
        (if (amharic) monthNamesAm else monthNamesEn)[month - 1]

    /** Ethiopian leap years are those where year % 4 == 3; Pagume then has 6 days. */
    fun isLeapYear(year: Int): Boolean = Math.floorMod(year, 4) == 3

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        in 1..12 -> 30
        13 -> if (isLeapYear(year)) 6 else 5
        else -> throw IllegalArgumentException("month must be 1..13, was $month")
    }

    fun toJdn(date: EthiopianDate): Long {
        val y = date.year.toLong()
        return EPOCH + 365L + 365L * (y - 1L) + Math.floorDiv(y, 4L) + 30L * date.month + date.day - 31L
    }

    fun fromJdn(jdn: Long): EthiopianDate {
        val d = jdn - EPOCH
        val r = Math.floorMod(d, 1461L)
        val n = (r % 365L) + 365L * (r / 1460L)
        val year = 4L * Math.floorDiv(d, 1461L) + r / 365L - r / 1460L
        return EthiopianDate(year.toInt(), (n / 30L + 1L).toInt(), (n % 30L + 1L).toInt())
    }

    fun fromLocalDate(date: LocalDate): EthiopianDate = fromJdn(date.toEpochDay() + EPOCH_DAY_TO_JDN)

    fun toLocalDate(date: EthiopianDate): LocalDate =
        LocalDate.ofEpochDay(toJdn(date) - EPOCH_DAY_TO_JDN)

    /** Moves (year, month) by [delta] months across the 13-month year. */
    fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 13 + (month - 1) + delta
        return Pair(Math.floorDiv(index, 13), Math.floorMod(index, 13) + 1)
    }
}
