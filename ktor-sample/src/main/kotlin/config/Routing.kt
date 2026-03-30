package config

import data.CsvStore
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import routes.accountRoutes
import routes.authRoutes
import routes.bookRoutes
import routes.staffRoutes
import util.seedIfMissing
import java.io.File

fun Application.configureRouting() {
    val dataDir =
        File("data").also { dir ->
            if (!dir.exists()) dir.mkdirs()
            seedIfMissing(dir, "books.csv")
            seedIfMissing(dir, "users.csv")
            seedIfMissing(dir, "loans.csv")
            seedIfMissing(dir, "reservations.csv")
        }

    val store = CsvStore(dataDir)

    routing {
        staticResources("/static", "static")

        bookRoutes(store)
        authRoutes(store)
        accountRoutes(store)
        staffRoutes(store)
    }
}
