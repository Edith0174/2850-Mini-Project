package util

import java.nio.charset.StandardCharsets

fun enc(text: String): String = java.net.URLEncoder.encode(text, StandardCharsets.UTF_8)
