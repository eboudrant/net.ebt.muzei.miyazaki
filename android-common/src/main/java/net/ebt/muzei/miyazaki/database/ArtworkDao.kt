package net.ebt.muzei.miyazaki.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import net.ebt.muzei.miyazaki.Artwork

@Dao
abstract class ArtworkDao {

    @Transaction
    open suspend fun setArtwork(artworkList: List<Artwork>) {
        deleteAll()
        insert(artworkList)
    }

    @Query("DELETE FROM artwork")
    internal abstract suspend fun deleteAll()

    @Insert
    internal abstract suspend fun insert(artworkList: List<Artwork>)

    suspend fun getArtwork(color: String? = null) = when (color) {
        "black" -> getBlackArtwork()
        "maroon" -> getMaroonArtwork()
        "green" -> getGreenArtwork()
        "teal" -> getTealArtwork()
        "navy" -> getNavyArtwork()
        else -> getAllArtwork()
    }

    @Query("SELECT * FROM artwork")
    internal abstract suspend fun getAllArtwork(): List<Artwork>

    @Query("SELECT * FROM artwork WHERE black > (" +
            "SELECT avg(black) FROM artwork WHERE black > 0)")
    internal abstract suspend fun getBlackArtwork(): List<Artwork>

    @Query("SELECT * FROM artwork WHERE maroon > (" +
            "SELECT avg(maroon) FROM artwork WHERE maroon > 0)")
    internal abstract suspend fun getMaroonArtwork(): List<Artwork>

    @Query("SELECT * FROM artwork WHERE green > (" +
            "SELECT avg(green) FROM artwork WHERE green > 0)")
    internal abstract suspend fun getGreenArtwork(): List<Artwork>

    @Query("SELECT * FROM artwork WHERE teal > (" +
            "SELECT avg(teal) FROM artwork WHERE teal > 0)")
    internal abstract suspend fun getTealArtwork(): List<Artwork>

    @Query("SELECT * FROM artwork WHERE navy > (" +
            "SELECT avg(navy) FROM artwork WHERE navy > 0)")
    internal abstract suspend fun getNavyArtwork(): List<Artwork>
}
