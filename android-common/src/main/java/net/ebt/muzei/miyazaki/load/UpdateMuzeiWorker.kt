package net.ebt.muzei.miyazaki.load

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ebt.muzei.miyazaki.common.BuildConfig.GHIBLI_AUTHORITY
import net.ebt.muzei.miyazaki.database.ArtworkDatabase

class UpdateMuzeiWorker(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CURRENT_PREF_NAME = "MuzeiGhibli.current"
        private const val SELECTED_COLOR = "muzei_color"

        fun toggleColor(context: Context, color: String) {
            val sharedPreferences = context.getSharedPreferences(
                    CURRENT_PREF_NAME, Context.MODE_PRIVATE)
            val currentColor = sharedPreferences.getString(SELECTED_COLOR,  null)
            sharedPreferences.edit {
                if (color == currentColor) {
                    remove(SELECTED_COLOR)
                } else {
                    putString(SELECTED_COLOR, color)
                }
            }
            enqueueUpdate()
        }

        fun getCurrentColor(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences(
                    CURRENT_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(SELECTED_COLOR,  null)
        }

        private fun enqueueUpdate() {
            val workManager = WorkManager.getInstance()
            workManager.enqueueUniqueWork("load", ExistingWorkPolicy.APPEND,
                    OneTimeWorkRequestBuilder<UpdateMuzeiWorker>()
                            .build())
        }
    }

    override suspend fun doWork(): Payload {
        val sharedPreferences = applicationContext.getSharedPreferences(
                CURRENT_PREF_NAME, Context.MODE_PRIVATE)
        val artworkList = ArtworkDatabase.getInstance(applicationContext)
                .artworkDao()
                .getArtwork(sharedPreferences.getString(SELECTED_COLOR, ""))
        withContext(Dispatchers.IO) {
            val providerClient = ProviderContract.getProviderClient(
                    applicationContext, GHIBLI_AUTHORITY)
            providerClient.setArtwork(artworkList.map { artwork ->
                Artwork.Builder()
                        .token(artwork.hash)
                        .persistentUri(artwork.url.toUri())
                        .title(artwork.caption)
                        .byline(artwork.subtitle)
                        .build()
            })
        }
        return Payload(Result.SUCCESS)
    }
}
