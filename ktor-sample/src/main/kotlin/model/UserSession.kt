package model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: Int,
    val role: String,
)
