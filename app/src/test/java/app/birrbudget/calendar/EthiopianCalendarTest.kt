package app.birrbudget.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class EthiopianCalendarTest {

    private fun eth(y: Int, m: Int, d: Int) = EthiopianDate(y, m, d)

    @Test
    fun knownAnchorsConvertBothWays() {
        val anchors = listOf(
            LocalDate.of(2025, 9, 11) to eth(2018, 1, 1), // Enkutatash 2018
            LocalDate.of(2023, 9, 12) to eth(2016, 1, 1), // Enkutatash after a leap Pagume
            LocalDate.of(2023, 9, 11) to eth(2015, 13, 6), // Pagume 6 (leap year)
            LocalDate.of(2026, 9, 11) to eth(2019, 1, 1), // Enkutatash 2019
            LocalDate.of(2026, 1, 7) to eth(2018, 4, 29), // Genna
            LocalDate.of(2026, 1, 19) to eth(2018, 5, 11), // Timket
            LocalDate.of(2026, 3, 2) to eth(2018, 6, 23), // Adwa Victory Day
            LocalDate.of(2024, 2, 29) to eth(2016, 6, 21), // Gregorian leap day
            LocalDate.of(2026, 10, 25) to eth(2019, 2, 15),
        )
        for ((gregorian, ethiopian) in anchors) {
            assertEquals("to Ethiopian: $gregorian", ethiopian, EthiopianCalendar.fromLocalDate(gregorian))
            assertEquals("to Gregorian: $ethiopian", gregorian, EthiopianCalendar.toLocalDate(ethiopian))
        }
    }

    @Test
    fun roundTripsEveryDayFrom1900To2100() {
        var day = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        while (!day.isAfter(end)) {
            val e = EthiopianCalendar.fromLocalDate(day)
            assertEquals(day, EthiopianCalendar.toLocalDate(e))
            day = day.plusDays(1)
        }
    }

    @Test
    fun pagumeHasFiveOrSixDays() {
        assertEquals(6, EthiopianCalendar.daysInMonth(2015, 13))
        assertEquals(5, EthiopianCalendar.daysInMonth(2016, 13))
        assertEquals(6, EthiopianCalendar.daysInMonth(2019, 13))
        assertEquals(30, EthiopianCalendar.daysInMonth(2019, 1))
    }

    @Test
    fun invalidDatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { eth(2016, 13, 6) } // 2016 is not leap
        assertThrows(IllegalArgumentException::class.java) { eth(2019, 14, 1) }
        assertThrows(IllegalArgumentException::class.java) { eth(2019, 1, 31) }
    }

    @Test
    fun ethiopianMonthRanges() {
        val meskerem = MonthRef(CalendarSystem.ETHIOPIAN, 2019, 1).range()
        assertEquals(LocalDate.of(2026, 9, 11), meskerem.start)
        assertEquals(LocalDate.of(2026, 10, 10), meskerem.endInclusive)

        val tikimt = MonthRef(CalendarSystem.ETHIOPIAN, 2019, 2).range()
        assertEquals(LocalDate.of(2026, 10, 11), tikimt.start)
        assertEquals(LocalDate.of(2026, 11, 9), tikimt.endInclusive)

        val pagume = MonthRef(CalendarSystem.ETHIOPIAN, 2019, 13).range()
        assertEquals(LocalDate.of(2027, 9, 6), pagume.start)
        assertEquals(LocalDate.of(2027, 9, 11), pagume.endInclusive)

        val shortPagume = MonthRef(CalendarSystem.ETHIOPIAN, 2016, 13).range()
        assertEquals(LocalDate.of(2024, 9, 6), shortPagume.start)
        assertEquals(LocalDate.of(2024, 9, 10), shortPagume.endInclusive)
    }

    @Test
    fun gregorianMonthRangeHandlesLeapFebruary() {
        val feb = MonthRef(CalendarSystem.GREGORIAN, 2028, 2).range()
        assertEquals(LocalDate.of(2028, 2, 1), feb.start)
        assertEquals(LocalDate.of(2028, 2, 29), feb.endInclusive)
    }

    @Test
    fun monthStepsWrapAcrossYears() {
        val pagume = MonthRef(CalendarSystem.ETHIOPIAN, 2019, 13)
        assertEquals(MonthRef(CalendarSystem.ETHIOPIAN, 2020, 1), pagume.plusMonths(1))
        assertEquals(MonthRef(CalendarSystem.ETHIOPIAN, 2019, 12), pagume.plusMonths(-1))
        val meskerem = MonthRef(CalendarSystem.ETHIOPIAN, 2019, 1)
        assertEquals(MonthRef(CalendarSystem.ETHIOPIAN, 2018, 13), meskerem.plusMonths(-1))
        assertEquals(MonthRef(CalendarSystem.ETHIOPIAN, 2020, 1), meskerem.plusMonths(13))

        val dec = MonthRef(CalendarSystem.GREGORIAN, 2026, 12)
        assertEquals(MonthRef(CalendarSystem.GREGORIAN, 2027, 1), dec.plusMonths(1))
    }

    @Test
    fun monthRefOfPicksTheContainingMonth() {
        val today = LocalDate.of(2026, 10, 25)
        assertEquals(MonthRef(CalendarSystem.ETHIOPIAN, 2019, 2), MonthRef.of(CalendarSystem.ETHIOPIAN, today))
        assertEquals(MonthRef(CalendarSystem.GREGORIAN, 2026, 10), MonthRef.of(CalendarSystem.GREGORIAN, today))
    }
}
