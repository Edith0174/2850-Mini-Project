package views

import data.CsvStore
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.hiddenInput
import kotlinx.html.hr
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.ul
import model.LoanRecord
import model.ReservationRecord
import model.UserRecord

private fun formatPickupLocationLabel(locationCode: String): String {
    if (locationCode.isBlank()) return "Ask staff at the desk"
    val match = Regex("""F(\d+)-B(\d+)-S(\d+)""").matchEntire(locationCode)
    return if (match != null) {
        val (floor, bay, shelf) = match.destructured
        "Floor $floor, Bay $bay, Shelf $shelf"
    } else {
        locationCode
    }
}

fun FlowContent.accountPage(
    user: UserRecord,
    loans: List<LoanRecord>,
    reservations: List<ReservationRecord>,
    store: CsvStore,
) {
    div("sketch-hero") {
        div("eyebrow") { +"My reservation space" }
        h1 { +"Your current reservation" }
        p("hero-note") { +"View reservation status, find the collection point, and manage the books connected to your account." }
        div("hero-meta") {
            span("pill") { +user.name }
            span("pill") { +user.email }
            span("pill") { +"Role: ${user.role}" }
        }
    }

    if (reservations.isEmpty()) {
        div("card reservation-card empty-state") {
            h2 { +"No current reservation" }
            p { +"You have no active or ready reservations yet." }
            a(href = "/books", classes = "button") { +"Book another reservation" }
        }
    } else {
        reservations.forEach { reservation ->
            val summary = store.getBook(reservation.isbn13)?.first
            val title = summary?.title ?: reservation.isbn13
            val collectionPoint = summary?.locations?.joinToString(", ")?.ifBlank { "Ask staff at the desk" } ?: "Ask staff at the desk"
            val statusLabel = reservation.status.lowercase()
            div("card reservation-card") {
                h2 { +"Your reservation" }
                div("reservation-detail-list") {
                    div("reservation-row") {
                        span("reservation-label") { +"Reserved" }
                        p("reservation-value") { +"$title, ISBN ${reservation.isbn13}" }
                    }
                    div("reservation-row") {
                        span("reservation-label") { +"Status" }
                        p("reservation-value") {
                            span(if (reservation.status == "READY") "status-ok" else "status-bad") { +statusLabel }
                        }
                    }
                    div("reservation-row") {
                        span("reservation-label") { +"Collection point" }
                        p("reservation-value") { +collectionPoint }
                    }
                    div("reservation-row") {
                        span("reservation-label") { +"Created" }
                        p("reservation-value") { +reservation.createdAt }
                    }
                    if (reservation.fulfilledAt.isNotBlank()) {
                        div("reservation-row") {
                            span("reservation-label") { +"Ready since" }
                            p("reservation-value") { +reservation.fulfilledAt }
                        }
                    }
                }

                div("reservation-actions") {
                    a(href = "/books", classes = "action-link") { +"Book another reservation" }
                    a(href = "/books/${reservation.isbn13}", classes = "action-link secondary-link") { +"View this title" }
                }
            }
        }
    }

    div("grid account-grid") {
        div("card") {
            h2 { +"Your current and past loans" }
            if (loans.isEmpty()) {
                p { +"No loans yet." }
            } else {
                loans.forEach { loan ->
                    val title = store.getBook(loan.isbn13)?.first?.title ?: loan.isbn13
                    div("loan-item") {
                        h3 { +title }
                        p { +"Copy: ${loan.copyKey}" }
                        p { +"Borrowed: ${loan.borrowedAt}" }
                        if (loan.pickupAt.isNotBlank()) {
                            p { +"Pick-up time: ${loan.pickupAt}" }
                        }
                        if (loan.pickupLocation.isNotBlank()) {
                            p { +"Pick-up location: ${formatPickupLocationLabel(loan.pickupLocation)}" }
                        }
                        p {
                            +"Accessibility support: "
                            span(if (loan.accessibilityRequested) "status-ok" else "muted") {
                                +(if (loan.accessibilityRequested) "requested" else "not requested")
                            }
                        }
                        p {
                            +"Status: "
                            span(if (loan.status == "BORROWED") "status-bad" else "status-ok") { +loan.status.lowercase() }
                        }
                        if (loan.status == "BORROWED") {
                            form(action = "/return", method = FormMethod.post, classes = "inline") {
                                hiddenInput(name = "copyKey") { value = loan.copyKey }
                                button(type = ButtonType.submit) { +"Return this book" }
                            }
                        } else {
                            p("muted") { +"Returned: ${loan.returnedAt}" }
                        }
                    }
                    hr {}
                }
            }
        }
        div("card") {
            h2 { +"Reservation list" }
            if (reservations.isEmpty()) {
                p { +"No reservations yet." }
            } else {
                ul("reservation-mini-list") {
                    reservations.forEach { reservation ->
                        val title = store.getBook(reservation.isbn13)?.first?.title ?: reservation.isbn13
                        li {
                            h3 { +title }
                            p { +"Status: ${reservation.status.lowercase()}" }
                            p("muted") { +"Created: ${reservation.createdAt}" }
                        }
                    }
                }
            }
        }
    }
}
