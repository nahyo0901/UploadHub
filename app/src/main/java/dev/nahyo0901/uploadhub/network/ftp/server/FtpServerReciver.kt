package dev.nahyo0901.uploadhub.network.ftp.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FtpServerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val running = intent.getBooleanExtra(
            FtpServerService.EXTRA_RUNNING,
            false
        )

        val log = intent.getStringExtra(
            FtpServerService.EXTRA_LOG
        )

        // TODO: forward to UI layer (recommended options below)
    }
}