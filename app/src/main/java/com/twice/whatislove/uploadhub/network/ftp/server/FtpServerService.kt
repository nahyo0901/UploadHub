package com.twice.whatislove.uploadhub.network.ftp.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File

class FtpServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    private var currentPort = DEFAULT_PORT
    private var currentRootDir: File? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START -> startServer(intent)
            ACTION_STOP -> stopServer()
            ACTION_STATUS -> broadcastStatus(isRunning, "Status requested")
            else -> startServer(intent)
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
    // Start / Stop
    // =========================

    private fun startServer(intent: Intent?) {
        if (isRunning) return

        currentPort = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val rootDirPath = intent?.getStringExtra(EXTRA_ROOT_DIR)

        currentRootDir = if (!rootDirPath.isNullOrBlank()) {
            File(rootDirPath).apply { if (!exists()) mkdirs() }
        } else {
            File(filesDir, "ftp_root").apply { if (!exists()) mkdirs() }
        }

        startForeground(NOTIFICATION_ID, buildNotification(false))
        broadcastStatus(false, "Starting FTP server…")

        serviceScope.launch {
            val result = ServerLauncher.startBlocking(currentPort, currentRootDir!!)

            when (result) {
                is ServerLauncher.StartResult.Success -> {
                    isRunning = true
                    updateNotification(true)
                    broadcastStatus(true, "FTP running on port ${result.port}")
                }

                is ServerLauncher.StartResult.Error -> {
                    broadcastStatus(false, "Server failed: ${result.message}")
                    stopSelf()
                }
            }
        }
    }

    private fun stopServer() {
        broadcastStatus(false, "Stopping FTP server…")
        stopSelf()
    }

    private fun stopServerInternal() {
        try {
            if (ServerLauncher.isRunning()) {
                ServerLauncher.stop()
            }
        } catch (_: Throwable) {
        } finally {
            isRunning = false
            broadcastStatus(false, "FTP Server stopped")
        }
    }

    // =========================
    // Broadcast
    // =========================

    private fun broadcastStatus(running: Boolean, log: String) {
        val intent = Intent().apply {
            setClassName(
                packageName,
                "${packageName}.network.ftp.server.FtpServerReceiver"
            )
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
        val text = if (isRunning) "Listening on port $currentPort" else "Preparing server"

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
    // Companion
    // =========================

    companion object {

        private const val NOTIFICATION_CHANNEL_ID = "ftp_server_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_PORT = 2121

        const val ACTION_START = "ftp_server_start"
        const val ACTION_STOP = "ftp_server_stop"
        const val ACTION_STATUS = "ftp_server_status"

        const val EXTRA_RUNNING = "running"
        const val EXTRA_LOG = "log"
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
            ContextCompat.startForegroundService(context, intent)
        }

        fun requestStatus(context: Context) {
            val intent = Intent(context, FtpServerService::class.java).apply {
                action = ACTION_STATUS
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}