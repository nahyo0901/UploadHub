package dev.nahyo0901.uploadhub.datastore

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// IMPORTANT: We reuse the SAME datastore created in SettingsDataStore
// Do NOT create another Context.dataStore or you'll get conflicts again.
private val Context.transferDataStore by preferencesDataStore(name = "ftp_transfer_offsets")

class TransferOffsetDataStore(private val context: Context) {

    /**
     * Save offset in bytes for a given transferId
     */
    suspend fun saveOffset(transferId: String, offset: Long) {
        val key = longPreferencesKey(transferId)
        context.transferDataStore.edit { prefs ->
            prefs[key] = offset
        }
    }

    /**
     * Retrieve offset in bytes for a given transferId
     * Returns 0 if not found
     */
    suspend fun getOffset(transferId: String): Long {
        val key = longPreferencesKey(transferId)
        val prefs = context.transferDataStore.data.first()
        return prefs[key] ?: 0L
    }

    /**
     * Retrieve offset once for synchronous usage inside transfer loop
     */
    suspend fun getOffsetOnce(transferId: String): Long {
        return getOffset(transferId)
    }

    /**
     * Clear offset when transfer completes
     */
    suspend fun clearOffset(transferId: String) {
        val key = longPreferencesKey(transferId)
        context.transferDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }
}
