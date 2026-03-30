package config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import model.UserSession

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("library_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7
            transform(SessionTransportTransformerMessageAuthentication("library-session-signing-key".toByteArray()))
        }
    }
}
