package ru.dontour.citymodule

data class CityItem (
    val title: String,
    val imageResId: Int,
    var isSelected: Boolean = false,
)