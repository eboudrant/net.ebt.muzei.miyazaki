package net.ebt.muzei.miyazaki

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import net.ebt.muzei.miyazaki.BuildConfig.GHIBLI_AUTHORITY
import java.io.IOException
import java.util.UUID

class ArtworkLoadWorker(
        context: Context,
        workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "ArtworkLoadWorker"

        fun enqueueLoad() {
            val workManager = WorkManager.getInstance()
            workManager.beginUniqueWork("load", ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<ArtworkLoadWorker>()
                            .setConstraints(Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build())
                            .build()).enqueue()
        }
    }

    override fun doWork(): Result {
        val artworkList = try {
             GhibliService.list()
        } catch(e: IOException) {
            Log.w(TAG, "Error loading artwork", e)
            return Result.RETRY
        }
        val loadId = UUID.randomUUID().toString()
        artworkList.forEach { artwork ->
            if (!isCancelled) {
                ProviderContract.Artwork.addArtwork(applicationContext, GHIBLI_AUTHORITY,
                        Artwork.Builder()
                                .token(artwork.hash)
                                .persistentUri(artwork.url.toUri())
                                .title(artwork.caption)
                                .byline(artwork.subtitle)
                                .metadata(loadId)
                                .build())
            }
        }
        if (!isCancelled) {
            val contentUri = ProviderContract.Artwork.getContentUri(GHIBLI_AUTHORITY)
            applicationContext.contentResolver.delete(contentUri,
                    "${ProviderContract.Artwork.METADATA} != ?",
                    arrayOf(loadId))
        }
        return Result.SUCCESS
    }
}
