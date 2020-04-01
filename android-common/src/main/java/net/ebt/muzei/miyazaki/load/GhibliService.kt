package net.ebt.muzei.miyazaki.load

import android.content.Context
import android.preference.PreferenceManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import net.ebt.muzei.miyazaki.Artwork
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.UUID

internal interface GhibliService {

    companion object {

        private fun createService(context: Context): GhibliService {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (!sharedPreferences.contains("uuid")) {
                sharedPreferences.edit()
                        .putString("uuid", UUID.randomUUID().toString())
                        .apply()
            }
            val uuid = sharedPreferences.getString("uuid", null) ?: ""
            val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        var request = chain.request()
                        val url = request.url().newBuilder()
                                .addQueryParameter("a", uuid).build()
                        request = request.newBuilder().url(url).build()
                        chain.proceed(request)
                    }
                    .build()

            val retrofit = Retrofit.Builder()
                    .baseUrl("https://muzei-ghibli.appspot.com/")
                    .client(okHttpClient)
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

            return retrofit.create(GhibliService::class.java)
        }

        internal suspend fun list(context: Context): List<Artwork> {
            return createService(context).list.await().artworks
        }
    }

    @get:GET("/list?sort=no")
    val list: Deferred<ArtworkList>

    data class ArtworkList(val artworks: List<Artwork>)
}
