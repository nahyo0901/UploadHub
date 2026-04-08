package com.twice.whatislove.uploadhub.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "transfer_offsets"
private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

class TransferOffsetDataStore(private val context: Context) {

    private fun keyForFile(fileIdentifier: String) = longPreferencesKey(fileIdentifier)

    fun getOffset(fileIdentifier: String): Flow<Long> =
        context.dataStore.data.map { prefs ->
            prefs[keyForFile(fileIdentifier)] ?: 0L
        }

    suspend fun saveOffset(fileIdentifier: String, offset: Long) {
        context.dataStore.edit { prefs ->
            prefs[keyForFile(fileIdentifier)] = offset
        }
    }

    suspend fun clearOffset(fileIdentifier: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(keyForFile(fileIdentifier))
        }
    }
}