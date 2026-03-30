package model

data class BookSummary(
    val isbn13: String,
    val title: String,
    val author: String,
    val totalCopies: Int,
    val availableCopies: Int,
    val locations: List<String>,
    val formats: List<String>,
    val notes: List<String>,
)
