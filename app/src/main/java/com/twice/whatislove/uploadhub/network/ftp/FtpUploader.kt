package com.twice.whatislove.uploadhub.ftp

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.FileInputStream

class FtpUploader {
    fun upload(host: String, port: Int, user: String, pass: String, remotePath: String, localFile: String): Boolean {
        val ftp = FTPClient()
        ftp.connect(host, port)
        val logged = ftp.login(user, pass)
        if (!logged) return false
        ftp.enterLocalPassiveMode()
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        FileInputStream(localFile).use { fis ->
            return ftp.storeFile(remotePath, fis)
        }
    }
}
