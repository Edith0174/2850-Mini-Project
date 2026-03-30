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
import views.inventoryPage
import views.libraryPage

fun Route.staffRoutes(store: CsvStore) {
    get("/staff") {
        val session = call.sessions.get<UserSession>()
        if (session?.role != "staff") {
            call.respondRedirect("/books?flash=" + enc("Staff access only."))
            return@get
        }
        call.respondHtml {
            libraryPage("Inventory", session, call.request.queryParameters["flash"].orEmpty()) {
                inventoryPage(store.listBooks())
            }
        }
    }

    post("/staff/add") {
        val session = call.sessions.get<UserSession>()
        if (session?.role != "staff") {
            call.respondRedirect("/books?flash=" + enc("Staff access only."))
            return@post
        }
        val p = call.receiveParameters()
        store.addBook(
            title = p["title"].orEmpty(),
            author = p["author"].orEmpty(),
            isbn13 = p["isbn"].orEmpty(),
            formatCode = p["format"].orEmpty(),
            locationCode = p["location"].orEmpty(),
            notes = p["notes"].orEmpty(),
        )
        call.respondRedirect("/staff?flash=" + enc("Book copy added."))
    }

    post("/staff/update") {
        val session = call.sessions.get<UserSession>()
        if (session?.role != "staff") {
            call.respondRedirect("/books?flash=" + enc("Staff access only."))
            return@post
        }
        val p = call.receiveParameters()
        store.updateBook(
            p["isbn"].orEmpty(),
            p["title"].orEmpty(),
            p["author"].orEmpty(),
            p["format"].orEmpty(),
            p["location"].orEmpty(),
            p["notes"].orEmpty(),
        )
        call.respondRedirect("/staff?flash=" + enc("Book details updated for that ISBN."))
    }

    post("/staff/remove") {
        val session = call.sessions.get<UserSession>()
        if (session?.role != "staff") {
            call.respondRedirect("/books?flash=" + enc("Staff access only."))
            return@post
        }
        val isbn = call.receiveParameters()["isbn"].orEmpty()
        store.removeBook(isbn)
        call.respondRedirect("/staff?flash=" + enc("All copies for that ISBN were removed."))
    }
}
