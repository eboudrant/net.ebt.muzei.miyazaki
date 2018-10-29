package net.ebt.muzei.miyazaki

import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import net.ebt.muzei.miyazaki.load.ArtworkLoadWorker

class GhibliArtProvider : MuzeiArtProvider() {
    override fun onLoadRequested(initial: Boolean) {
        ArtworkLoadWorker.enqueueLoad()
    }
}