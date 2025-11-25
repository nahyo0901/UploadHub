package com.twice.whatislove.uploadhub.network.ftp.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Example usage snippet. Call from a coroutine scope in your app (not on main thread).
 */
fun example() {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        val client = FtpClient()
        try {
            client.connect("192.168.1.100", 21)
            client.login("user", "password")
            val pwd = client.pwd()
            println("PWD: $pwd")
            val listing = client.list()
            listing.forEach { println(it) }

            // download
            client.download("/remote/path/file.txt", File("/sdcard/Download/file.txt"))

            // upload
            client.upload(File("/sdcard/Download/upload.txt"), "/remote/path/upload.txt")

            client.quit()
        } catch (e: Exception) {
            e.printStackTrace()
            client.close()
        }
    }
}