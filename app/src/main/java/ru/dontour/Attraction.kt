package ru.dontour

import com.google.gson.annotations.SerializedName

data class Attraction(
    val id: String,
    val name: String,
    val description: String,
    val wiki_link: String?,
    val coordinates: Coordinates?,
    val pic: String?,
    val level: Int?
)