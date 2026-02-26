package ru.rostov

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val invalid: String
)