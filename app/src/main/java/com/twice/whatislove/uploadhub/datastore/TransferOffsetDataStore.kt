package com.twice.whatislove.uploadhub.datastore

import android.content.Context
import androidx.datastore.preferences.core.LongPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
     * Retrieve offset in bytes for a given transferId as Flow
     */
    fun getOffsetFlow(transferId: String): Flow<Long> {
        val key = preferencesKey<Long>(transferId)
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: 0L
        }
    }

    /**
     * Retrieve offset once (synchronous-friendly inside loops)
     */
    suspend fun getOffsetOnce(transferId: String): Long {
        return getOffsetFlow(transferId).first()
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