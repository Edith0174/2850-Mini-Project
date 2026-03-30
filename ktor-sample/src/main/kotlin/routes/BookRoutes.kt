package routes

import data.CsvStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import model.UserSession
import views.bookDetailPage
import views.booksPage
import views.libraryPage

fun Route.bookRoutes(store: CsvStore) {
    get("/") {
        call.respondRedirect("/signup")
    }

    get("/books") {
        val query = call.request.queryParameters["q"].orEmpty()
        val books = store.listBooks(query)
        call.respondHtml {
            libraryPage("Library Catalogue", call.sessions.get(), call.request.queryParameters["flash"].orEmpty()) {
                booksPage(books, query, call.sessions.get())
            }
        }
    }

    get("/books/{isbn}") {
        val isbn = call.parameters["isbn"].orEmpty()
        val result = store.getBook(isbn)
        if (result == null) {
            call.respondText("Book not found", status = HttpStatusCode.NotFound)
            return@get
        }
        val (summary, copies) = result
        val borrowedKeys =
            store
                .getLoansForUser(call.sessions.get<UserSession>()?.userId ?: -1)
                .filter { it.status == "BORROWED" }
                .map { it.copyKey }
                .toSet()
        val nextAvailableCopy = store.findFirstAvailableCopy(isbn)

        call.respondHtml {
            libraryPage(summary.title, call.sessions.get(), call.request.queryParameters["flash"].orEmpty()) {
                bookDetailPage(summary, copies, borrowedKeys, call.sessions.get(), nextAvailableCopy)
            }
        }
    }
}
