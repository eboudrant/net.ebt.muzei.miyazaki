package net.ebt.muzei.miyazaki

import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

class GhibliArtProvider : MuzeiArtProvider() {
    override fun onLoadRequested(initial: Boolean) {
        ArtworkLoadWorker.enqueueLoad()
    }
}