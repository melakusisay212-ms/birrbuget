package app.birrbudget.data

import android.content.Context
import app.birrbudget.calendar.CalendarSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny SharedPreferences-backed settings holder exposing observable state. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _calendar = MutableStateFlow(readCalendar())
    val calendar: StateFlow<CalendarSystem> = _calendar.asStateFlow()

    private val _amharic = MutableStateFlow(prefs.getBoolean(KEY_AMHARIC, false))
    val amharic: StateFlow<Boolean> = _amharic.asStateFlow()

    fun setCalendar(system: CalendarSystem) {
        prefs.edit().putString(KEY_CALENDAR, system.name).apply()
        _calendar.value = system
    }

    fun setAmharic(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMHARIC, enabled).apply()
        _amharic.value = enabled
    }

    private fun readCalendar(): CalendarSystem =
        runCatching { CalendarSystem.valueOf(prefs.getString(KEY_CALENDAR, null) ?: "") }
            .getOrDefault(CalendarSystem.ETHIOPIAN)

    private companion object {
        const val KEY_CALENDAR = "calendar"
        const val KEY_AMHARIC = "amharic"
    }
}
