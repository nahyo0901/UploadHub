package com.twice.whatislove.uploadhub.network.ftp.client.engine

import java.io.File

class ApacheFtpUploader(private val client: FtpClient) {

    fun upload(
        file: File,
        remotePath: String,
        progress: (Float) -> Unit
    ) {
        client.uploadFile(file, remotePath) { sent, total ->
            progress(sent.toFloat() / total.toFloat())
        }
    }

    fun download(
        remotePath: String,
        localFile: File,
        progress: (Float) -> Unit
    ) {
        client.downloadFile(remotePath, localFile) { recv, total ->
            progress(recv.toFloat() / total.toFloat())
        }
    }
}