package net.ebt.muzei.miyazaki.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
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
