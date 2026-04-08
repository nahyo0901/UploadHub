package com.twice.whatislove.uploadhub.datastore

import android.content.Context
import androidx.datastore.preferences.core.LongPreferencesKey
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Unique DataStore instance to avoid conflict
private val Context.transferOffsetDataStore by preferencesDataStore(name = "ftp_transfer_offsets")

class TransferOffsetDataStore(private val context: Context) {

    suspend fun saveOffset(transferId: String, offset: Long) {
        val key = preferencesKey<Long>(transferId)
        context.transferOffsetDataStore.edit { prefs ->
            prefs[key] = offset
        }
    }

    fun getOffsetFlow(transferId: String): Flow<Long> {
        val key = preferencesKey<Long>(transferId)
        return context.transferOffsetDataStore.data.map { prefs ->
            prefs[key] ?: 0L
        }
    }

    suspend fun getOffsetOnce(transferId: String): Long {
        return getOffsetFlow(transferId).first()
    }

    suspend fun clearOffset(transferId: String) {
        val key = preferencesKey<Long>(transferId)
        context.transferOffsetDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }
}