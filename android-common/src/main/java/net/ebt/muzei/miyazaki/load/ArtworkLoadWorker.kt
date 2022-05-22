package net.ebt.muzei.miyazaki.load

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ebt.muzei.miyazaki.Artwork
import net.ebt.muzei.miyazaki.common.BuildConfig
import net.ebt.muzei.miyazaki.database.ArtworkDatabase
import okio.buffer
import okio.source
import java.util.concurrent.TimeUnit

class ArtworkLoadWorker(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ArtworkLoadWorker"
        private const val INITIAL_TAG = "initial"
        private const val LAST_LOADED_MILLIS = "last_loaded_millis"
        private val EXPIRATION = TimeUnit.DAYS.toMillis(15)

        fun enqueueInitialLoad(context: Context) {
            val workManager = WorkManager.getInstance(context)
            // Kick off an immediate initial load with no network constraints
            workManager.beginUniqueWork("load", ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<ArtworkLoadWorker>()
                            .addTag(INITIAL_TAG)
                            .build())
                    .then(OneTimeWorkRequestBuilder<UpdateMuzeiWorker>()
                            .build())
                    .enqueue()
        }

        fun enqueueReload(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val lastLoadedMillis = sharedPreferences.getLong(LAST_LOADED_MILLIS, 0L)
            if (System.currentTimeMillis() - lastLoadedMillis > EXPIRATION || BuildConfig.DEBUG) {
                val workManager = WorkManager.getInstance(context)
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

    override suspend fun doWork(): Result {
        val (artworkList, loadedFromNetwork) = try {
            GhibliService.list(applicationContext) to true
        } catch(e: Exception) {
            Log.w(TAG, "Error loading artwork", e)
            if (INITIAL_TAG in tags) {
                // Try to load artwork from our data.json local asset only if we've never loaded
                // artwork before as we don't want to override previously successful loads
                // with our (potentially outdated) local asset
                try {
                    withContext(Dispatchers.IO) {
                        applicationContext.resources.assets.open("data.json").source().buffer().use { input ->
                            val moshi = Moshi.Builder().build()
                            val listType = Types.newParameterizedType(
                                    List::class.java, Artwork::class.java)
                            val adapter: JsonAdapter<List<Artwork>> = moshi.adapter(listType)
                            adapter.fromJson(input)!! to false
                        }
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Error loading data.json", error)
                    return Result.failure()
                }
            } else {
                return Result.retry()
            }
        }
        ArtworkDatabase.getInstance(applicationContext)
                .artworkDao()
                .setArtwork(artworkList)
        if (loadedFromNetwork) {
            withContext(Dispatchers.IO) {
                PreferenceManager.getDefaultSharedPreferences(applicationContext).edit {
                    putLong(LAST_LOADED_MILLIS, System.currentTimeMillis())
                }
            }
        }
        return Result.success()
    }
}