package net.ebt.muzei.miyazaki.load

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.ebt.muzei.miyazaki.database.ArtworkDatabase
import java.io.IOException

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
                            .build())
                    .then(OneTimeWorkRequestBuilder<UpdateMuzeiWorker>()
                            .build())
                    .enqueue()
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
        return Result.SUCCESS
    }
}
