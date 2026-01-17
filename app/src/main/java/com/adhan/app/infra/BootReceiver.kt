package com.adhan.app.infra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Here we would ideally trigger a reschedule of all prayer alarms.
            // For now, we'll just log or start a background work to redo the scheduling.
            android.util.Log.d("BootReceiver", "Reboot completed, rescheduling alarms...")
        }
    }
}
