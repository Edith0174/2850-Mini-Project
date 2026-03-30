package model

data class BookCopy(
    val copyKey: String,
    val title: String,
    val author: String,
    val isbn13: String,
    val formatCode: String,
    val locationCode: String,
    val notes: String,
)
