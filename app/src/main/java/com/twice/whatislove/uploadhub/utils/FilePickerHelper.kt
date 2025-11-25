package com.twice.whatislove.uploadhub.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FilePickerHelper {
    fun getFileFromUri(context: Context, uri: Uri): File {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        val name = cursor?.use { if (it.moveToFirst()) it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME)) else "temp" } ?: "temp"
        val input = context.contentResolver.openInputStream(uri) ?: throw Exception("cannot open")
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { output -> input.copyTo(output) }
        input.close()
        return file
    }
}
