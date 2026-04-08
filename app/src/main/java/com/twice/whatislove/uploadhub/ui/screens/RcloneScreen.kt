package com.twice.whatislove.uploadhub.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) name = it.getString(index)
    }
    return name
}
