package net.ebt.muzei.miyazaki

import android.app.PendingIntent
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
            ArtworkLoadWorker.enqueueInitialLoad(context)
        } else {
            ArtworkLoadWorker.enqueueReload(context)
        }
    }

    override fun getArtworkInfo(artwork: Artwork): PendingIntent? {
        val context = context ?: return null
        val byline = artwork.byline
        if (byline != null && byline.contains('-')) {
            val query = byline.substring(0, byline.indexOf('-'))
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return PendingIntent.getActivity(context, artwork.id.toInt(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return null
    }
}
