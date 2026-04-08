package com.twice.whatislove.uploadhub.data

import android.content.Context
import androidx.datastore.preferences.core.LongPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Create DataStore instance
private val Context.dataStore by preferencesDataStore(name = "ftp_transfer_offsets")

class TransferOffsetDataStore(private val context: Context) {

    /**
     * Save offset in bytes for a given transferId
     */
    suspend fun saveOffset(transferId: String, offset: Long) {
        val key = preferencesKey<Long>(transferId)
        context.dataStore.edit { prefs ->
            prefs[key] = offset
        }
    }

    /**
     * Retrieve offset in bytes for a given transferId
     * Returns 0 if not found
     */
    suspend fun getOffset(transferId: String): Long {
        val key = preferencesKey<Long>(transferId)
        val prefs = context.dataStore.data.first()
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
        val key = preferencesKey<Long>(transferId)
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }
}