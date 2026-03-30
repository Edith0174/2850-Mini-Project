package views

import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.hiddenInput
import kotlinx.html.hr
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import model.BookCopy
import model.BookSummary
import model.UserSession
import kotlin.collections.take

private fun formatLocationLabel(locationCode: String): String {
    if (locationCode.isBlank()) return "Online or ask staff"
    val match = Regex("F(\\d+)-B(\\d+)-S(\\d+)").matchEntire(locationCode)
    return if (match != null) {
        val (floor, bay, shelf) = match.destructured
        "Floor $floor, Bay $bay, Shelf $shelf"
    } else {
        locationCode
    }
}

fun FlowContent.booksPage(
    books: List<BookSummary>,
    query: String,
    session: UserSession?,
) {
    div("sketch-hero") {
        div("eyebrow") { +"1. Search page" }
        h1 {
            +"Library catalogue"
        }
        p("hero-note") { +"Search what you are looking for, open a title, then move into reservation or borrowing." }

        form(action = "/books", method = FormMethod.get, classes = "search-form") {
            div {
                label { +"Search" }
                input(type = InputType.text, name = "q") {
                    value = query
                    placeholder = "title, author, ISBN, location..."
                }
            }
            button(type = ButtonType.submit) { +"Search" }
        }

        p("muted") { +"${books.size} matching titles" }

        table {
            thead {
                tr {
                    th { +"Title" }
                    th { +"Author" }
                    th { +"Availability" }
                    th { +"Location" }
                    th { +"Formats" }
                }
            }
            tbody {
                for (book in books.take(200)) {
                    tr {
                        td {
                            a(href = "/books/${book.isbn13}") { +book.title }
                            br {}
                            small { +"ISBN: ${book.isbn13}" }
                        }
                        td { +book.author }
                        td {
                            span(classes = if (book.availableCopies > 0) "status-ok" else "status-bad") {
                                +"${book.availableCopies} of ${book.totalCopies} available"
                            }
                        }
                        td { +(book.locations.joinToString(", ").ifBlank { "Unknown" }) }
                        td { +(book.formats.joinToString(", ")) }
                    }
                }
            }
        }

        if (books.size > 200) {
            p("muted") { +"Showing first 200 results to keep the page tidy." }
        }

        if (session == null) {
            p("muted") { +"Visitors can search freely. Borrowing and reserving need an account." }
        }
    }
}

fun FlowContent.bookDetailPage(
    summary: BookSummary,
    copies: List<BookCopy>,
    borrowedKeys: Set<String>,
    session: UserSession?,
    nextAvailableCopy: BookCopy?,
) {
    div("sketch-hero") {
        div("eyebrow") { +"2. Reservation details" }
        h1 { +summary.title }
        p("hero-note") { +"Check availability, see locations, then borrow or reserve from the action panel." }
    }

    div("split") {
        div {
            div("card") {
                h2 { +summary.title }
                p { +"by ${summary.author}" }
                div {
                    span("pill") { +"ISBN ${summary.isbn13}" }
                    span("pill") { +"${summary.availableCopies}/${summary.totalCopies} available" }
                }
                p { +"Formats: ${summary.formats.joinToString(", ")}" }
                p { +"Locations: ${summary.locations.joinToString(", ")}" }
                if (summary.notes.isNotEmpty()) {
                    p { +"Notes: ${summary.notes.joinToString(" | ")}" }
                }
            }

            div("card") {
                h2 { +"Copy list" }
                table {
                    thead {
                        tr {
                            th { +"Copy" }
                            th { +"Format" }
                            th { +"Location" }
                            th { +"Notes" }
                        }
                    }
                    tbody {
                        copies.forEach { copy ->
                            tr {
                                td { +copy.copyKey }
                                td { +copy.formatCode }
                                td {
                                    +formatLocationLabel(copy.locationCode)
                                    br {}
                                    small { +copy.locationCode.ifBlank { "No code" } }
                                }
                                td { +(copy.notes.ifBlank { "-" }) }
                            }
                        }
                    }
                }
            }
        }

        div {
            div("card") {
                h3 { +"Actions" }
                if (session == null) {
                    p { +"Log in to borrow or reserve this book." }
                } else {
                    if (nextAvailableCopy != null) {
                        div("pickup-preview") {
                            p {
                                span("reservation-label") { +"Next available copy" }
                                br {}
                                +"${nextAvailableCopy.copyKey}"
                            }
                            p {
                                span("reservation-label") { +"Library location" }
                                br {}
                                +formatLocationLabel(nextAvailableCopy.locationCode)
                            }
                        }
                    } else {
                        p("muted") { +"No copy is available right now, but you can still reserve the title." }
                    }

                    form(action = "/borrow", method = FormMethod.post) {
                        hiddenInput(name = "isbn") { value = summary.isbn13 }
                        div("form-stack") {
                            label {
                                +"Pick-up time"
                                input(type = InputType.dateTimeLocal, name = "pickupAt") {
                                    required = true
                                }
                            }
                            label("checkbox-row") {
                                checkBoxInput(name = "accessibilityRequested") {}
                                span {
                                    +"Accessibility support needed"
                                }
                            }
                            p("muted") {
                                +"If selected, a staff member will be ready to assist you when you arrive at the library."
                            }
                        }
                        button(type = ButtonType.submit) { +"Borrow an available copy" }
                    }

                    hr {}

                    form(action = "/reserve", method = FormMethod.post) {
                        hiddenInput(name = "isbn") { value = summary.isbn13 }
                        button(type = ButtonType.submit, classes = "secondary") { +"Reserve this title" }
                    }

                    if (borrowedKeys.isNotEmpty()) {
                        p("muted") { +"You can return borrowed books from My account." }
                    }
                }
            }
        }
    }
}
