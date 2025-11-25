package com.twice.whatislove.uploadhub.catbox

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object CatboxApi {
    private const val CATBOX_URL = "https://catbox.moe/user/api.php"
    private val client = OkHttpClient()

    fun uploadFile(file: File): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("fileToUpload", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()
        val req = Request.Builder().url(CATBOX_URL).post(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Upload failed: ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }
}
