package model

data class UserRecord(
    val userId: Int,
    val name: String,
    val email: String,
    val address: String,
    val passwordHash: String,
    val role: String,
)
