package data

import at.favre.lib.crypto.bcrypt.BCrypt
import model.BookCopy
import model.BookSummary
import model.LoanRecord
import model.ReservationRecord
import model.UserRecord
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CsvStore(
    private val baseDir: File,
) {
    private val booksFile = File(baseDir, "books.csv")
    private val usersFile = File(baseDir, "users.csv")
    private val loansFile = File(baseDir, "loans.csv")
    private val reservationsFile = File(baseDir, "reservations.csv")

    private val formatNames =
        mapOf(
            "PB" to "Paperback",
            "LP" to "Large Print",
            "ACD" to "Audiobook CD",
            "ADIG" to "Digital Audio",
            "EBK" to "eBook",
        )

    @Synchronized
    fun listBooks(query: String = ""): List<BookSummary> {
        val loans = activeLoans()
        val q = query.trim().lowercase()
        val grouped = readBookCopies().groupBy { it.isbn13.ifBlank { it.title + "|" + it.author } }
        return grouped.values
            .map { copies ->
                val first = copies.first()
                val available = copies.count { it.copyKey !in loans }
                BookSummary(
                    isbn13 = first.isbn13,
                    title = first.title,
                    author = first.author,
                    totalCopies = copies.size,
                    availableCopies = available,
                    locations =
                        copies
                            .map { it.locationCode }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted(),
                    formats = copies.map { formatNames[it.formatCode] ?: it.formatCode }.distinct().sorted(),
                    notes = copies.map { it.notes }.filter { it.isNotBlank() }.distinct(),
                )
            }.filter {
                if (q.isBlank()) {
                    true
                } else {
                    listOf(
                        it.title,
                        it.author,
                        it.isbn13,
                        it.locations.joinToString(" "),
                        it.formats.joinToString(" "),
                        it.notes.joinToString(" "),
                    ).joinToString(" ").lowercase().contains(q)
                }
            }.sortedWith(compareBy<BookSummary> { it.title.lowercase() }.thenBy { it.author.lowercase() })
    }

    @Synchronized
    fun getBook(isbn13: String): Pair<BookSummary, List<BookCopy>>? {
        val copies = readBookCopies().filter { it.isbn13 == isbn13 }
        if (copies.isEmpty()) return null
        val summary = listBooks().firstOrNull { it.isbn13 == isbn13 } ?: return null
        return summary to copies
    }

    @Synchronized
    fun createUser(
        name: String,
        email: String,
        address: String,
        password: String,
    ): Result<UserRecord> {
        val users = readUsers()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("An account with that email already exists."))
        }
        val nextId = (users.maxOfOrNull { it.userId } ?: 0) + 1
        val record =
            UserRecord(
                userId = nextId,
                name = name,
                email = email,
                address = address,
                passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray()),
                role = "member",
            )
        appendCsv(usersFile, listOf(record.userId.toString(), record.name, record.email, record.address, record.passwordHash, record.role))
        return Result.success(record)
    }

    @Synchronized
    fun authenticate(
        email: String,
        password: String,
    ): UserRecord? {
        val user = readUsers().firstOrNull { it.email.equals(email, ignoreCase = true) } ?: return null
        return if (BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified) user else null
    }

    @Synchronized
    fun getUser(userId: Int): UserRecord? = readUsers().firstOrNull { it.userId == userId }

    @Synchronized
    fun borrowBook(
        userId: Int,
        isbn13: String,
        pickupAt: String,
        accessibilityRequested: Boolean,
    ): Result<String> {
        val copies = readBookCopies().filter { it.isbn13 == isbn13 }
        if (copies.isEmpty()) return Result.failure(IllegalArgumentException("Book not found."))
        val activeLoans = activeLoans()
        val availableCopy =
            copies.firstOrNull { it.copyKey !in activeLoans }
                ?: return Result.failure(IllegalArgumentException("No copy is currently available to borrow."))
        val nextId = (readLoans().maxOfOrNull { it.loanId } ?: 0) + 1
        appendCsv(
            loansFile,
            listOf(
                nextId.toString(),
                userId.toString(),
                isbn13,
                availableCopy.copyKey,
                "BORROWED",
                timestamp(),
                "",
                pickupAt,
                availableCopy.locationCode,
                accessibilityRequested.toString(),
            ),
        )
        tryFulfilReservation(isbn13)
        val supportMessage = if (accessibilityRequested) " Staff assistance has been requested for your arrival." else ""
        return Result.success(
            "Borrowed \"${availableCopy.title}\" successfully. Copy ${availableCopy.copyKey} is at ${availableCopy.locationCode}. Pick-up time: ${pickupAt.ifBlank { "not set" }}.$supportMessage",
        )
    }

    @Synchronized
    fun returnBook(
        userId: Int,
        copyKey: String,
    ): Result<String> {
        val loans = readLoans().toMutableList()
        val index = loans.indexOfFirst { it.userId == userId && it.copyKey == copyKey && it.status == "BORROWED" }
        if (index == -1) return Result.failure(IllegalArgumentException("Active loan not found."))
        loans[index] = loans[index].copy(status = "RETURNED", returnedAt = timestamp())
        writeLoans(loans)
        tryFulfilReservation(loans[index].isbn13)
        return Result.success("Book returned successfully.")
    }

    @Synchronized
    fun reserveBook(
        userId: Int,
        isbn13: String,
    ): Result<String> {
        val existing = readReservations().any { it.userId == userId && it.isbn13 == isbn13 && it.status in listOf("ACTIVE", "READY") }
        if (existing) return Result.failure(IllegalArgumentException("You already have an active reservation for this book."))
        val nextId = (readReservations().maxOfOrNull { it.reservationId } ?: 0) + 1
        appendCsv(reservationsFile, listOf(nextId.toString(), userId.toString(), isbn13, "ACTIVE", timestamp(), ""))
        tryFulfilReservation(isbn13)
        return Result.success("Reservation created.")
    }

    @Synchronized
    fun getLoansForUser(userId: Int): List<LoanRecord> = readLoans().filter { it.userId == userId }.sortedByDescending { it.loanId }

    @Synchronized
    fun getReservationsForUser(userId: Int): List<ReservationRecord> =
        readReservations().filter { it.userId == userId }.sortedByDescending { it.reservationId }


    @Synchronized
    fun findFirstAvailableCopy(isbn13: String): BookCopy? {
        val activeLoans = activeLoans()
        return readBookCopies().firstOrNull { it.isbn13 == isbn13 && it.copyKey !in activeLoans }
    }

    @Synchronized
    fun addBook(
        title: String,
        author: String,
        isbn13: String,
        formatCode: String,
        locationCode: String,
        notes: String,
    ) {
        appendCsv(booksFile, listOf(title, author, isbn13, formatCode, locationCode, notes))
    }

    @Synchronized
    fun updateBook(
        isbn13: String,
        title: String,
        author: String,
        formatCode: String,
        locationCode: String,
        notes: String,
    ) {
        val rows = readRawBooks()
        val updated =
            rows.map { row ->
                if (row.getOrNull(2).orEmpty() == isbn13) listOf(title, author, isbn13, formatCode, locationCode, notes) else row
            }
        writeCsv(booksFile, listOf(listOf("title", "author", "isbn_13", "format_code", "location_code", "notes")) + updated)
    }

    @Synchronized
    fun removeBook(isbn13: String) {
        val rows = readRawBooks().filterNot { it.getOrNull(2).orEmpty() == isbn13 }
        writeCsv(booksFile, listOf(listOf("title", "author", "isbn_13", "format_code", "location_code", "notes")) + rows)
    }

    private fun tryFulfilReservation(isbn13: String) {
        val loans = activeLoans()
        val hasAvailable = readBookCopies().any { it.isbn13 == isbn13 && it.copyKey !in loans }
        if (!hasAvailable) return
        val reservations = readReservations().toMutableList()
        val idx = reservations.indexOfFirst { it.isbn13 == isbn13 && it.status == "ACTIVE" }
        if (idx != -1) {
            reservations[idx] = reservations[idx].copy(status = "READY", fulfilledAt = timestamp())
            writeReservations(reservations)
        }
    }

    private fun activeLoans(): Set<String> = readLoans().filter { it.status == "BORROWED" }.map { it.copyKey }.toSet()

    private fun readBookCopies(): List<BookCopy> =
        readRawBooks().mapIndexed { index, row ->
            BookCopy(
                copyKey = "COPY-${index + 1}",
                title = row.getOrNull(0).orEmpty(),
                author = row.getOrNull(1).orEmpty(),
                isbn13 = row.getOrNull(2).orEmpty().removeSuffix(".0"),
                formatCode = row.getOrNull(3).orEmpty(),
                locationCode = row.getOrNull(4).orEmpty(),
                notes = row.getOrNull(5).orEmpty(),
            )
        }

    private fun readRawBooks(): List<List<String>> = readCsv(booksFile).drop(1)

    private fun readUsers(): List<UserRecord> =
        readCsv(usersFile).drop(1).mapNotNull { row ->
            val id = row.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            UserRecord(
                id,
                row.getOrNull(1).orEmpty(),
                row.getOrNull(2).orEmpty(),
                row.getOrNull(3).orEmpty(),
                row.getOrNull(4).orEmpty(),
                row.getOrNull(5).orEmpty(),
            )
        }

    private fun readLoans(): List<LoanRecord> =
        readCsv(loansFile).drop(1).mapNotNull { row ->
            val id = row.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val userId = row.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val pickupLocation = if (row.size >= 10) row.getOrNull(8).orEmpty() else ""
            val accessibilityRequested =
                if (row.size >= 10) {
                    row.getOrNull(9).orEmpty().equals("true", ignoreCase = true)
                } else {
                    row.getOrNull(8).orEmpty().equals("true", ignoreCase = true)
                }
            LoanRecord(
                id,
                userId,
                row.getOrNull(2).orEmpty().removeSuffix(".0"),
                row.getOrNull(3).orEmpty(),
                row.getOrNull(4).orEmpty(),
                row.getOrNull(5).orEmpty(),
                row.getOrNull(6).orEmpty(),
                row.getOrNull(7).orEmpty(),
                pickupLocation,
                accessibilityRequested,
            )
        }

    private fun readReservations(): List<ReservationRecord> =
        readCsv(reservationsFile).drop(1).mapNotNull { row ->
            val id = row.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val userId = row.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            ReservationRecord(
                id,
                userId,
                row.getOrNull(2).orEmpty().removeSuffix(".0"),
                row.getOrNull(3).orEmpty(),
                row.getOrNull(4).orEmpty(),
                row.getOrNull(5).orEmpty(),
            )
        }

    private fun writeLoans(loans: List<LoanRecord>) {
        writeCsv(
            loansFile,
            listOf(listOf("loan_id", "user_id", "isbn_13", "copy_key", "status", "borrowed_at", "returned_at", "pickup_at", "pickup_location", "accessibility_requested")) +
                loans.map {
                    listOf(
                        it.loanId.toString(),
                        it.userId.toString(),
                        it.isbn13,
                        it.copyKey,
                        it.status,
                        it.borrowedAt,
                        it.returnedAt,
                        it.pickupAt,
                        it.pickupLocation,
                        it.accessibilityRequested.toString(),
                    )
                },
        )
    }

    private fun writeReservations(reservations: List<ReservationRecord>) {
        writeCsv(
            reservationsFile,
            listOf(listOf("reservation_id", "user_id", "isbn_13", "status", "created_at", "fulfilled_at")) +
                reservations.map {
                    listOf(it.reservationId.toString(), it.userId.toString(), it.isbn13, it.status, it.createdAt, it.fulfilledAt)
                },
        )
    }

    private fun readCsv(file: File): List<List<String>> = if (!file.exists()) emptyList() else file.readLines().map { parseCsvLine(it) }

    private fun appendCsv(
        file: File,
        values: List<String>,
    ) {
        file.appendText(values.joinToString(",") { csvEscape(it) } + "\n")
    }

    private fun writeCsv(
        file: File,
        rows: List<List<String>>,
    ) {
        file.writeText(rows.joinToString("\n") { row -> row.joinToString(",") { csvEscape(it) } } + "\n")
    }

    private fun csvEscape(value: String): String {
        val safe = value.replace("\r", " ").replace("\n", " ")
        return if (safe.contains(",") || safe.contains('"')) '"' + safe.replace("\"", "\"\"") + '"' else safe
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        out += current.toString()
        return out
    }

    companion object {
        private fun timestamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}
