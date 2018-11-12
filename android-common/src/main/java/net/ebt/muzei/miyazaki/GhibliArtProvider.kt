package net.ebt.muzei.miyazaki

import android.app.SearchManager
import android.content.Intent
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import net.ebt.muzei.miyazaki.common.BuildConfig
import net.ebt.muzei.miyazaki.load.ArtworkLoadWorker

class GhibliArtProvider : MuzeiArtProvider() {
    companion object {
        const val AUTHORITY = BuildConfig.GHIBLI_AUTHORITY
    }

    override fun onLoadRequested(initial: Boolean) {
        val context = context ?: return
        if (initial) {
            ArtworkLoadWorker.enqueueInitialLoad()
        } else {
            ArtworkLoadWorker.enqueueReload(context)
        }
    }

    override fun openArtworkInfo(artwork: Artwork): Boolean {
        val context = context ?: return false
        val byline = artwork.byline
        if (byline != null && byline.contains('-')) {
            try {
                val query = byline.substring(0, byline.indexOf('-'))
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
            }
        }
        return false
    }
}
