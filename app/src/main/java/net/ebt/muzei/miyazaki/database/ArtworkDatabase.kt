package net.ebt.muzei.miyazaki.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.ebt.muzei.miyazaki.Artwork

@Database(entities = [Artwork::class], version = 1)
abstract class ArtworkDatabase : RoomDatabase() {

    abstract fun artworkDao(): ArtworkDao

    companion object {
        @Volatile
        private var instance: ArtworkDatabase? = null

        fun getInstance(context: Context): ArtworkDatabase {
            val applicationContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(applicationContext,
                        ArtworkDatabase::class.java, "artwork.db")
                        .build()
            }
        }
    }
}
