package app.birrbudget.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarSystem { ETHIOPIAN, GREGORIAN }

data class DateRange(val start: LocalDate, val endInclusive: LocalDate)

/**
 * A month in either calendar. Budgets, summaries and lists are all computed over
 * [range], so switching calendars only changes which days a "month" covers.
 * Stored data is never touched (everything is saved as Gregorian epoch days).
 */
data class MonthRef(val system: CalendarSystem, val year: Int, val month: Int) {

    fun range(): DateRange = when (system) {
        CalendarSystem.ETHIOPIAN -> {
            val start = EthiopianCalendar.toLocalDate(EthiopianDate(year, month, 1))
            val length = EthiopianCalendar.daysInMonth(year, month)
            DateRange(start, start.plusDays((length - 1).toLong()))
        }
        CalendarSystem.GREGORIAN -> {
            val start = LocalDate.of(year, month, 1)
            DateRange(start, start.withDayOfMonth(start.lengthOfMonth()))
        }
    }

    fun plusMonths(delta: Int): MonthRef = when (system) {
        CalendarSystem.ETHIOPIAN -> {
            val (y, m) = EthiopianCalendar.addMonths(year, month, delta)
            MonthRef(system, y, m)
        }
        CalendarSystem.GREGORIAN -> {
            val ym = YearMonth.of(year, month).plusMonths(delta.toLong())
            MonthRef(system, ym.year, ym.monthValue)
        }
    }

    fun label(amharic: Boolean): String = when (system) {
        CalendarSystem.ETHIOPIAN -> "${EthiopianCalendar.monthName(month, amharic)} $year"
        CalendarSystem.GREGORIAN ->
            "${Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"
    }

    companion object {
        fun of(system: CalendarSystem, date: LocalDate): MonthRef = when (system) {
            CalendarSystem.ETHIOPIAN -> {
                val e = EthiopianCalendar.fromLocalDate(date)
                MonthRef(system, e.year, e.month)
            }
            CalendarSystem.GREGORIAN -> MonthRef(system, date.year, date.monthValue)
        }
    }
}

/** Formatting helpers so UI code never touches calendar math directly. */
object DateText {
    fun format(date: LocalDate, system: CalendarSystem, amharic: Boolean): String = when (system) {
        CalendarSystem.ETHIOPIAN -> {
            val e = EthiopianCalendar.fromLocalDate(date)
            "${e.day} ${EthiopianCalendar.monthName(e.month, amharic)} ${e.year}"
        }
        CalendarSystem.GREGORIAN ->
            date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    }

    /** Day-of-month number as shown in the calendar grid. */
    fun dayNumber(date: LocalDate, system: CalendarSystem): Int = when (system) {
        CalendarSystem.ETHIOPIAN -> EthiopianCalendar.fromLocalDate(date).day
        CalendarSystem.GREGORIAN -> date.dayOfMonth
    }

    private fun sundayIndex(day: DayOfWeek): Int = day.value % 7 // Sunday(7) -> 0

    fun weekdayName(date: LocalDate, system: CalendarSystem, amharic: Boolean): String = when (system) {
        CalendarSystem.ETHIOPIAN -> {
            val i = sundayIndex(date.dayOfWeek)
            if (amharic) EthiopianCalendar.weekdayNamesAm[i] else EthiopianCalendar.weekdayNamesEn[i]
        }
        CalendarSystem.GREGORIAN -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    /** Header label for column [index] (0 = Sunday) of the date picker grid. */
    fun weekdayShort(index: Int, system: CalendarSystem, amharic: Boolean): String = when (system) {
        CalendarSystem.ETHIOPIAN ->
            if (amharic) EthiopianCalendar.weekdayShortAm[index] else EthiopianCalendar.weekdayShortEn[index]
        CalendarSystem.GREGORIAN ->
            DayOfWeek.of(if (index == 0) 7 else index).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    /** Column offset (0 = Sunday) of the first day of the grid. */
    fun columnOf(date: LocalDate): Int = sundayIndex(date.dayOfWeek)
}
