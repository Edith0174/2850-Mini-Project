package views

import io.ktor.http.ContentType
import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.title
import model.UserSession

fun HTML.libraryPage(
    pageTitle: String,
    session: UserSession?,
    flash: String = "",
    content: FlowContent.() -> Unit,
) {
    head {
        title("$pageTitle |  Miniproject")
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        link(rel = "stylesheet", href = "/static/css/library.css", type = ContentType.Text.CSS.toString())
    }
    body {
        div("page-shell") {
            div("nav") {
                div("container nav-inner") {
                    div("brand-wrap") {
                        div("brand-kicker") { +"Library system" }
                        div("brand") { +"Miniproject" }
                    }
                    div("nav-links") {
                        navLink("/books", "Browse books")
                        if (session != null) navLink("/account", "My account")
                        if (session?.role == "staff") navLink("/staff", "Inventory")
                        if (session == null) {
                            navLink("/login", "Log in")
                            navLink("/signup", "Sign up")
                        } else {
                            navLink("/logout", "Log out")
                        }
                    }
                }
            }
            main("container") {
                if (flash.isNotBlank()) div("flash") { +flash }
                content()
            }
            footer("container page-footer") {
                +"Search, reserve, borrow, return. Built for the same backend, now wearing a sketchbook coat."
            }
        }
    }
}
