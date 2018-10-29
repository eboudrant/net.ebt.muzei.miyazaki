package net.ebt.muzei.miyazaki

data class Artwork(
        val aid: String,
        val hash: String,
        val url: String,
        val width: Int,
        val height: Int,
        val ratio: Float,
        val caption: String,
        val subtitle: String,
        val silver: Float,
        val grey: Float,
        val black: Float,
        val maroon: Float,
        val orange: Float,
        val yellow: Float,
        val olive: Float,
        val lime: Float,
        val green: Float,
        val aqua: Float,
        val teal: Float,
        val blue: Float,
        val navy: Float,
        val fuchsia: Float,
        val purple: Float,
        val white: Float
)
