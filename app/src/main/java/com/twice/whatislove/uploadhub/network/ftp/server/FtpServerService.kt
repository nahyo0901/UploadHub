package com.twice.whatislove.uploadhub.network.ftp.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File

class FtpServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    // =========================
    // Lifecycle
    // =========================

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_START -> startServer(intent)
            ACTION_STOP -> stopServer()

            else -> startServer(intent) // backward compatibility
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopServerInternal()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // =========================
    // Start / Stop Logic
    // =========================

    private fun startServer(intent: Intent?) {
        if (isRunning) return

        startForeground(NOTIFICATION_ID, buildNotification(false))
        broadcastStatus(false, "Starting FTP server…")

        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val rootDirPath = intent?.getStringExtra(EXTRA_ROOT_DIR)

        val rootDir = if (!rootDirPath.isNullOrBlank()) {
            File(rootDirPath).apply { if (!exists()) mkdirs() }
        } else {
            File(filesDir, "ftp_root").apply { if (!exists()) mkdirs() }
        }

        serviceScope.launch {
            try {
                ServerLauncher.start(port, rootDir)

                isRunning = true
                updateNotification(true)
                broadcastStatus(true, "FTP Server running on port $port")

            } catch (t: Throwable) {
                broadcastStatus(false, "Server failed: ${t.message}")
                stopSelf()
            }
        }
    }

    private fun stopServer() {
        broadcastStatus(false, "Stopping FTP server…")
        stopSelf()
    }

    private fun stopServerInternal() {
        try {
            if (isRunning) {
                ServerLauncher.stop()
                broadcastStatus(false, "FTP Server stopped")
            }
        } catch (_: Throwable) {}
        isRunning = false
    }

    // =========================
    // Broadcast to UI
    // =========================

    private fun broadcastStatus(running: Boolean, log: String) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_RUNNING, running)
            putExtra(EXTRA_LOG, log)
        }
        sendBroadcast(intent)
    }

    // =========================
    // Notification
    // =========================

    private fun updateNotification(isRunning: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(isRunning))
    }

    private fun buildNotification(isRunning: Boolean): Notification {
        val title = if (isRunning) "FTP Server running" else "Starting FTP Server"
        val text = if (isRunning) "Listening on port $DEFAULT_PORT" else "Preparing server"

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FTP Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    // =========================
    // Companion / API for UI
    // =========================

    companion object {

        private const val NOTIFICATION_CHANNEL_ID = "ftp_server_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_PORT = 2121

        // Actions
        const val ACTION_START = "ftp_server_start"
        const val ACTION_STOP = "ftp_server_stop"
        const val ACTION_STATUS = "ftp_server_status"

        // Broadcast extras
        const val EXTRA_RUNNING = "running"
        const val EXTRA_LOG = "log"

        // Existing extras
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_ROOT_DIR = "EXTRA_ROOT_DIR"

        fun startService(context: Context, port: Int = DEFAULT_PORT, rootDir: File? = null) {
            val intent = Intent(context, FtpServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
                rootDir?.let { putExtra(EXTRA_ROOT_DIR, it.absolutePath) }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FtpServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
