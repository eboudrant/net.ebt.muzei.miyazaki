package net.ebt.muzei.miyazaki.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ebt.muzei.miyazaki.Artwork

@Dao
abstract class ArtworkDao {

    @Transaction
    open suspend fun setArtwork(artworkList: List<Artwork>): Unit = withContext(Dispatchers.IO) {
        deleteAll()
        insert(artworkList)
    }

    @Query("DELETE FROM artwork")
    internal abstract fun deleteAll()

    @Insert
    internal abstract fun insert(artworkList: List<Artwork>)

    suspend fun getArtwork(color: String? = null) = withContext(Dispatchers.IO) {
        when (color) {
            "black" -> blackArtwork
            "grey" -> greyArtwork
            "silver" -> silverArtwork
            "maroon" -> maroonArtwork
            "olive" -> oliveArtwork
            "green" -> greenArtwork
            "teal" -> tealArtwork
            "navy" -> navyArtwork
            "purple" -> purpleArtwork
            else -> artwork
        }
    }

    @get:Query("SELECT * FROM artwork")
    internal abstract val artwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE black > (" +
            "SELECT avg(black) FROM artwork WHERE black > 0)")
    internal abstract val blackArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE grey > (" +
            "SELECT avg(grey) FROM artwork WHERE grey > 0)")
    internal abstract val greyArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE silver > (" +
            "SELECT avg(silver) FROM artwork WHERE silver > 0)")
    internal abstract val silverArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE maroon > (" +
            "SELECT avg(maroon) FROM artwork WHERE maroon > 0)")
    internal abstract val maroonArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE olive > (" +
            "SELECT avg(olive) FROM artwork WHERE olive > 0)")
    internal abstract val oliveArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE green > (" +
            "SELECT avg(green) FROM artwork WHERE green > 0)")
    internal abstract val greenArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE teal > (" +
            "SELECT avg(teal) FROM artwork WHERE teal > 0)")
    internal abstract val tealArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE navy > (" +
            "SELECT avg(navy) FROM artwork WHERE navy > 0)")
    internal abstract val navyArtwork: List<Artwork>

    @get:Query("SELECT * FROM artwork WHERE purple > (" +
            "SELECT avg(purple) FROM artwork WHERE purple > 0)")
    internal abstract val purpleArtwork: List<Artwork>
}
