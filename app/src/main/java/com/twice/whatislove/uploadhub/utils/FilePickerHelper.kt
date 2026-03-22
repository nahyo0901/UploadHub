package com.twice.whatislove.uploadhub.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FilePickerHelper {

    fun getFileFromUri(context: Context, uri: Uri): File {
        // Safely get the display name
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            var result = "temp"
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
            result
        } ?: "temp"

        // Safely copy the file using use blocks
        context.contentResolver.openInputStream(uri)?.use { input ->
            val file = File(context.cacheDir, name)
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
            return file
        } ?: throw Exception("Cannot open input stream for URI: $uri")
    }
}