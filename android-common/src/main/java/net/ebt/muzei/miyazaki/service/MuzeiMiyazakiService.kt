package net.ebt.muzei.miyazaki.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * This class is kept only to serve as a tombstone to Muzei to know to replace it
 * with [net.ebt.muzei.miyazaki.GhibliArtProvider].
 */
class MuzeiMiyazakiService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
