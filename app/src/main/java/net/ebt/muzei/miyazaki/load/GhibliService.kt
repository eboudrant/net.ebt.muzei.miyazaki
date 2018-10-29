package net.ebt.muzei.miyazaki.load

import net.ebt.muzei.miyazaki.app.MuzeiMiyazakiApplication
import net.ebt.muzei.miyazaki.Artwork
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.io.IOException

internal interface GhibliService {

    companion object {

        private fun createService(): GhibliService {
            val uuid = MuzeiMiyazakiApplication.getInstance().uuid
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
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

            return retrofit.create<GhibliService>(GhibliService::class.java)
        }

        @Throws(IOException::class)
        internal fun list(): List<Artwork> {
            return createService().list.execute().body()?.artworks
                    ?: throw IOException("Response was null")
        }
    }

    @get:GET("/list?sort=no")
    val list: Call<ArtworkList>

    data class ArtworkList(val artworks: List<Artwork>)
}
