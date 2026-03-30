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
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.textArea
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import model.BookSummary
import kotlin.collections.take

fun FlowContent.inventoryPage(books: List<BookSummary>) {
    div("grid") {
        div("card") {
            h1 { +"Inventory management" }
            p { +"Staff can add a new copy, update all rows for one ISBN, or remove an ISBN entirely." }
            h3 { +"Add copy" }
            inventoryForm("/staff/add", "Add")
        }
        div("card") {
            h2 { +"Update by ISBN" }
            inventoryForm("/staff/update", "Update")
            h3 { +"Remove by ISBN" }
            form(action = "/staff/remove", method = FormMethod.post) {
                label { +"ISBN" }
                input(type = InputType.text, name = "isbn") { required = true }
                button(type = ButtonType.submit, classes = "danger") { +"Remove all copies" }
            }
        }
    }
    div("card") {
        h2 { +"Current catalogue snapshot" }
        table {
            thead {
                tr {
                    th { +"ISBN" }
                    th { +"Title" }
                    th { +"Copies" }
                    th { +"Location" }
                }
            }
            tbody {
                books.take(120).forEach { book ->
                    tr {
                        td { +book.isbn13 }
                        td { +book.title }
                        td { +book.totalCopies.toString() }
                        td { +(book.locations.joinToString(", ")) }
                    }
                }
            }
        }
    }
}

fun FlowContent.inventoryForm(
    action: String,
    submitText: String,
) {
    form(action = action, method = FormMethod.post) {
        label { +"Title" }
        input(type = InputType.text, name = "title") { required = true }
        label { +"Author" }
        input(type = InputType.text, name = "author") { required = true }
        label { +"ISBN" }
        input(type = InputType.text, name = "isbn") { required = true }
        label { +"Format code" }
        input(type = InputType.text, name = "format") { required = true }
        label { +"Location code" }
        input(type = InputType.text, name = "location") { required = true }
        label { +"Notes" }
        textArea(rows = "3", cols = "20") { name = "notes" }
        button(type = ButtonType.submit) { +submitText }
    }
}

fun FlowContent.navLink(
    href: String,
    text: String,
) {
    a(href = href) { +text }
}
