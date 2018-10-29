package net.ebt.muzei.miyazaki.load

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.BaseColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.apps.muzei.api.MuzeiContract
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import net.ebt.muzei.miyazaki.BuildConfig.GHIBLI_AUTHORITY
import net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME
import net.ebt.muzei.miyazaki.Constants.MUZEI_COLOR
import net.ebt.muzei.miyazaki.database.ArtworkDatabase

class UpdateMuzeiWorker(
        context: Context,
        workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "UpdateMuzeiWorker"

        fun enqueueUpdate() {
            val workManager = WorkManager.getInstance()
            workManager.beginUniqueWork("load", ExistingWorkPolicy.APPEND,
                    OneTimeWorkRequestBuilder<UpdateMuzeiWorker>()
                            .build())
                    .enqueue()
        }
    }

    override fun doWork(): Result {
        val settings = applicationContext.getSharedPreferences(
                CURRENT_PREF_NAME, Context.MODE_PRIVATE)
        val artworkList = ArtworkDatabase.getInstance(applicationContext)
                .artworkDao()
                .getArtwork(settings.getString(MUZEI_COLOR, ""))
        val currentTime = System.currentTimeMillis()
        artworkList.forEach { artwork ->
            if (!isCancelled) {
                ProviderContract.Artwork.addArtwork(applicationContext, GHIBLI_AUTHORITY,
                        Artwork.Builder()
                                .token(artwork.hash)
                                .persistentUri(artwork.url.toUri())
                                .title(artwork.caption)
                                .byline(artwork.subtitle)
                                .build())
            }
        }
        if (!isCancelled) {
            val contentUri = ProviderContract.Artwork.getContentUri(GHIBLI_AUTHORITY)
            val deleteOperations = ArrayList<ContentProviderOperation>()
            applicationContext.contentResolver.query(
                    contentUri,
                    arrayOf(BaseColumns._ID, MuzeiContract.Artwork.COLUMN_NAME_TOKEN),
                    "${ProviderContract.Artwork.DATE_MODIFIED}<?",
                    arrayOf(currentTime.toString()),
                    null)?.use { data ->
                while (data.moveToNext()) {
                    val artworkUri = ContentUris.withAppendedId(contentUri,
                            data.getLong(0))
                    val token = data.getString(1)
                    if (artworkList.firstOrNull { it.hash == token } == null) {
                        deleteOperations += ContentProviderOperation
                                .newDelete(artworkUri)
                                .build()
                    }
                }
            }
            if (deleteOperations.isNotEmpty()) {
                try {
                    applicationContext.contentResolver.applyBatch(GHIBLI_AUTHORITY,
                            deleteOperations)
                } catch(e: Exception) {
                    Log.i(TAG, "Error removing deleted artwork", e)
                }
            }
        }
        return Result.SUCCESS
    }
}
