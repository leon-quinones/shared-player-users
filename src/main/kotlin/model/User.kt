package com.example.model

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable


@Serializable
data class User(
    val id: Uuid,
    val userName: String,
    val name: String,
    val avatarUrl: String,
    val profileUrl: String,
    val email: String? = null
)

