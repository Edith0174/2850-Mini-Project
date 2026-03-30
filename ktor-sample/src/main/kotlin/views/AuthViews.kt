package views

import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p

fun FlowContent.authPage(
    titleText: String,
    action: String,
    isLogin: Boolean,
    error: String? = null,
) {
    div("sketch-hero auth-shell") {
        div("eyebrow") { + if (isLogin) "Start your trip" else "Create your library account" }
        h1 { +titleText }
        p("hero-note") {
            +(if (isLogin) "Sign in, search books, then head to your reservation space." else "A quick sign-up unlocks borrowing and reservations.")
        }
    }

    div("card auth-card") {
        if (!error.isNullOrBlank()) p("status-bad") { +error }
        form(action = action, method = FormMethod.post) {
            if (!isLogin) {
                div {
                    label { +"Full name" }
                    input(type = InputType.text, name = "name") {
                        required = true
                        placeholder = "Your full name"
                    }
                }
                div {
                    label { +"Home address" }
                    input(type = InputType.text, name = "address") {
                        required = true
                        placeholder = "Home address"
                    }
                }
            }

            div {
                label { +"Email" }
                input(type = InputType.email, name = "email") {
                    required = true
                    placeholder = "name@example.com"
                }
            }

            div {
                label { +"Password" }
                input(type = InputType.password, name = "password") {
                    required = true
                    placeholder = "Password"
                }
            }

            div("auth-actions") {
                button(type = ButtonType.submit) {
                    +(if (isLogin) "Log in" else "Create account")
                }
            }
        }
        p("switch-link") {
            if (isLogin) {
                +"Need an account? "
                a(href = "/signup") { +"Sign up" }
            } else {
                +"Already registered? "
                a(href = "/login") { +"Log in" }
            }
        }
    }
}
