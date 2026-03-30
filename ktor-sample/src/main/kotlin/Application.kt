import config.configurePlugins
import config.configureRouting
import config.configureSessions
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configurePlugins()
    configureSessions()
    configureRouting()
}
