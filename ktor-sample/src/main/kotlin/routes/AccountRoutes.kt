package routes

import data.CsvStore
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import model.UserSession
import util.enc
import views.accountPage
import views.libraryPage

fun Route.accountRoutes(store: CsvStore) {
    get("/account") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login?flash=" + enc("Please log in first."))
            return@get
        }
        val user = store.getUser(session.userId)!!
        val loans = store.getLoansForUser(user.userId)
        val reservations = store.getReservationsForUser(user.userId)
        call.respondHtml {
            libraryPage("My account", session, call.request.queryParameters["flash"].orEmpty()) {
                accountPage(user, loans, reservations, store)
            }
        }
    }

    post("/borrow") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login?flash=" + enc("Please log in to borrow books."))
            return@post
        }
        val params = call.receiveParameters()
        val isbn = params["isbn"].orEmpty()
        val pickupAt = params["pickupAt"].orEmpty()
        val accessibilityRequested = params["accessibilityRequested"] == "on"
        val result = store.borrowBook(session.userId, isbn, pickupAt, accessibilityRequested)
        call.respondRedirect("/books/$isbn?flash=" + enc(result.getOrElse { it.message ?: "Unable to borrow book." }))
    }

    post("/reserve") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login?flash=" + enc("Please log in to reserve books."))
            return@post
        }
        val isbn = call.receiveParameters()["isbn"].orEmpty()
        val result = store.reserveBook(session.userId, isbn)
        call.respondRedirect("/books/$isbn?flash=" + enc(result.getOrElse { it.message ?: "Unable to reserve book." }))
    }

    post("/return") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login?flash=" + enc("Please log in first."))
            return@post
        }
        val copyKey = call.receiveParameters()["copyKey"].orEmpty()
        val result = store.returnBook(session.userId, copyKey)
        call.respondRedirect("/account?flash=" + enc(result.getOrElse { it.message ?: "Unable to return book." }))
    }
}
