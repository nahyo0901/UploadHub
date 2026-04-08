package com.twice.whatislove.uploadhub.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun getFileName(context: Context, uri: Uri): String {
    return try {
        var name: String? = null

        // Try to get DISPLAY_NAME from content resolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }

        // Fallback to last path segment if provider didn't give a name
        name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file_${System.currentTimeMillis()}"
    } catch (e: Exception) {
        // Absolute fallback so app never crashes
        "file_${System.currentTimeMillis()}"
    }
}