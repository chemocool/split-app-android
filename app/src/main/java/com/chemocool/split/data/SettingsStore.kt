package com.chemocool.split.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice(val label: String) { LIGHT("Clair"), DARK("Sombre"), SYSTEM("Système") }

data class LocalSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val notifyNewExpense: Boolean = true,
    val notifyMonthlyReminder: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "split_settings")

/** Préférences propres à ce téléphone (thème, notifications). */
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val newExpenseKey = booleanPreferencesKey("notify_new_expense")
    private val monthlyKey = booleanPreferencesKey("notify_monthly")

    val settings: Flow<LocalSettings> = context.dataStore.data.map { p: Preferences ->
        LocalSettings(
            theme = runCatching { ThemeChoice.valueOf(p[themeKey]!!) }.getOrDefault(ThemeChoice.SYSTEM),
            notifyNewExpense = p[newExpenseKey] ?: true,
            notifyMonthlyReminder = p[monthlyKey] ?: false,
        )
    }

    suspend fun setTheme(t: ThemeChoice) = context.dataStore.edit { it[themeKey] = t.name }
    suspend fun setNotifyNewExpense(v: Boolean) = context.dataStore.edit { it[newExpenseKey] = v }
    suspend fun setNotifyMonthly(v: Boolean) = context.dataStore.edit { it[monthlyKey] = v }
}
