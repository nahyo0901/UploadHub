package com.twice.whatislove.uploadhub.litterbox

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object LitterboxApi {
    private const val API_URL = "https://litterbox.catbox.moe/resources/internals/api.php"
    private val client = OkHttpClient()
    fun upload(file: File, time: String = "72h"): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart("time", time)
            .addFormDataPart("fileToUpload", file.name, file.asRequestBody())
            .build()
        val req = Request.Builder().url(API_URL).post(body).build()
        client.newCall(req).execute().use { r -> if(!r.isSuccessful) throw Exception("upload failed") ; return r.body?.string() ?: "" }
    }
}
