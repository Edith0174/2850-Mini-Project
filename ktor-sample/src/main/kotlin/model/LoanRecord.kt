package model

data class LoanRecord(
    val loanId: Int,
    val userId: Int,
    val isbn13: String,
    val copyKey: String,
    val status: String,
    val borrowedAt: String,
    val returnedAt: String,
    val pickupAt: String,
    val pickupLocation: String,
    val accessibilityRequested: Boolean,
)
