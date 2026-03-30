package routes

import data.CsvStore
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import model.UserSession
import util.enc
import views.authPage
import views.libraryPage

fun Route.authRoutes(store: CsvStore) {
    get("/signup") {
        call.respondHtml {
            libraryPage("Sign up", call.sessions.get(), call.request.queryParameters["flash"].orEmpty()) {
                authPage("Create library account", "/signup", false)
            }
        }
    }

    post("/signup") {
        val p = call.receiveParameters()
        val name = p["name"].orEmpty().trim()
        val email = p["email"].orEmpty().trim()
        val address = p["address"].orEmpty().trim()
        val password = p["password"].orEmpty()

        when {
            name.isBlank() || email.isBlank() || address.isBlank() || password.isBlank() -> {
                call.respondHtml {
                    libraryPage("Sign up", null, "") {
                        authPage("Create library account", "/signup", false, "Please fill in every field.")
                    }
                }
            }
            else -> {
                val result = store.createUser(name, email, address, password)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    call.sessions.set(UserSession(user.userId, user.role))
                    call.respondRedirect("/account?flash=" + enc("Welcome, ${user.name}!"))
                } else {
                    call.respondHtml {
                        libraryPage("Sign up", null, "") {
                            authPage("Create library account", "/signup", false, result.exceptionOrNull()?.message)
                        }
                    }
                }
            }
        }
    }

    get("/login") {
        call.respondHtml {
            libraryPage(
                "Log in",
                call.sessions.get(),
                call.request.queryParameters["flash"].orEmpty(),
            ) {
                authPage("Log in", "/login", true)
            }
        }
    }

    post("/login") {
        val p = call.receiveParameters()
        val user = store.authenticate(p["email"].orEmpty(), p["password"].orEmpty())
        if (user == null) {
            call.respondHtml {
                libraryPage("Log in", null, "") {
                    authPage("Log in", "/login", true, "Invalid email or password.")
                }
            }
        } else {
            call.sessions.set(UserSession(user.userId, user.role))
            call.respondRedirect("/account?flash=" + enc("Logged in successfully."))
        }
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/books?flash=" + enc("You have been logged out."))
    }
}
