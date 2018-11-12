package net.ebt.muzei.miyazaki

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Artwork(
        @PrimaryKey
        val aid: String,
        val hash: String,
        val url: String,
        val width: Int,
        val height: Int,
        val caption: String,
        val subtitle: String,
        val silver: Int,
        val grey: Int,
        val black: Int,
        val maroon: Int,
        val orange: Int,
        val yellow: Int,
        val olive: Int,
        val lime: Int,
        val green: Int,
        val aqua: Int,
        val teal: Int,
        val blue: Int,
        val navy: Int,
        val fuchsia: Int,
        val purple: Int,
        val white: Int
)
