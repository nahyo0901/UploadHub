package dev.nahyo0901.uploadhub.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")
object SettingsKeys { val DARK_MODE = booleanPreferencesKey("dark_mode") }

class SettingsDataStore(private val ctx: Context) {
    val darkMode: Flow<Boolean> = ctx.dataStore.data.map { it[SettingsKeys.DARK_MODE] ?: false }
    suspend fun setDarkMode(v:Boolean) { ctx.dataStore.edit { it[SettingsKeys.DARK_MODE]=v } }
}
