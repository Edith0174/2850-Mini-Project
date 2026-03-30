package util

import io.ktor.server.application.Application
import java.io.File

fun Application.seedIfMissing(
    dataDir: File,
    resourceName: String,
) {
    val target = File(dataDir, resourceName)
    if (target.exists()) return
    val bytes = environment.classLoader.getResourceAsStream("data/$resourceName")?.readAllBytes() ?: return
    target.writeBytes(bytes)
}
