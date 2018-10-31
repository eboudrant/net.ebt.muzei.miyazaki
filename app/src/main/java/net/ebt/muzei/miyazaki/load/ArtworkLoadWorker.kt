package net.ebt.muzei.miyazaki.load

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.ebt.muzei.miyazaki.BuildConfig
import net.ebt.muzei.miyazaki.database.ArtworkDatabase
import java.io.IOException
import java.util.concurrent.TimeUnit

class ArtworkLoadWorker(
        context: Context,
        workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "ArtworkLoadWorker"
        private const val LAST_LOADED_MILLIS = "last_loaded_millis"
        private val EXPIRATION = TimeUnit.DAYS.toMillis(15)

        fun enqueueLoad(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val lastLoadedMillis = sharedPreferences.getLong(LAST_LOADED_MILLIS, 0L)
            if (System.currentTimeMillis() - lastLoadedMillis > EXPIRATION || BuildConfig.DEBUG) {
                val workManager = WorkManager.getInstance()
                workManager.beginUniqueWork("load", ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<ArtworkLoadWorker>()
                                .setConstraints(Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build())
                                .build())
                        .then(OneTimeWorkRequestBuilder<UpdateMuzeiWorker>()
                                .build())
                        .enqueue()
            }
        }
    }

    override fun doWork(): Result {
        val artworkList = try {
            GhibliService.list()
        } catch(e: IOException) {
            Log.w(TAG, "Error loading artwork", e)
            return Result.RETRY
        }
        ArtworkDatabase.getInstance(applicationContext)
                .artworkDao()
                .setArtwork(artworkList)
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit {
            putLong(LAST_LOADED_MILLIS, System.currentTimeMillis())
        }
        return Result.SUCCESS
    }
}
