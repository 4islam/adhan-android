package com.adhan.app.infra

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AdhanService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
