package ru.rostov

data class Attraction(
    val id: String,
    val name: String,
    val description: String,
    val wiki_link: String?,
    val coordinates: String?,
    val pic: String?,
    val level: Int?
)