package model

data class ReservationRecord(
    val reservationId: Int,
    val userId: Int,
    val isbn13: String,
    val status: String,
    val createdAt: String,
    val fulfilledAt: String,
)
