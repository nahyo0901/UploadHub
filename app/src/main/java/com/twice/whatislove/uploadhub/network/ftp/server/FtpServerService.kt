package com.twice.whatislove.uploadhub.network.ftp.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Foreground Service that hosts the simple FTP server core.
 *
 * - Starts ServerLauncher on service start.
 * - Keeps the server running in a foreground notification so Android does not kill it.
 * - Stops the server when the service is destroyed.
 *
 * Notes:
 * - This service is a lightweight skeleton intended for development and local networks.
 * - Do not expose this server to untrusted networks without adding authentication and TLS.
 * - Ensure you declare INTERNET and FOREGROUND_SERVICE permissions in AndroidManifest.xml.
 */
class FtpServerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification(isRunning = false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val rootDirPath = intent?.getStringExtra(EXTRA_ROOT_DIR)
        val rootDir = if (!rootDirPath.isNullOrBlank()) {
            File(rootDirPath).apply { if (!exists()) mkdirs() }
        } else {
            File(filesDir, "ftp_root").apply { if (!exists()) mkdirs() }
        }

        serviceScope.launch {
            try {
                ServerLauncher.start(port = port, rootDir = rootDir)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isRunning = true))
            } catch (t: Throwable) {
                t.printStackTrace()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            ServerLauncher.stop()
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            serviceScope.coroutineContext.cancel()
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FTP Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification channel for FTP server foreground service"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isRunning: Boolean): Notification {
        val title = if (isRunning) "FTP Server running" else "Starting FTP Server"
        val text = if (isRunning) "Listening on port $DEFAULT_PORT" else "Preparing FTP server"

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent ?: Intent(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ftp_server_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_PORT = 2121

        const val EXTRA_PORT = "com.twice.whatislove.uploadhub.EXTRA_PORT"
        const val EXTRA_ROOT_DIR = "com.twice.whatislove.uploadhub.EXTRA_ROOT_DIR"

        fun startService(context: Context, port: Int = DEFAULT_PORT, rootDir: File? = null) {
            val intent = Intent(context, FtpServerService::class.java).apply {
                putExtra(EXTRA_PORT, port)
                rootDir?.let { putExtra(EXTRA_ROOT_DIR, it.absolutePath) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FtpServerService::class.java)
            context.stopService(intent)
        }
    }
}