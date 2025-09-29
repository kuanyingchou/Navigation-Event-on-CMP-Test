package com.example.testcmp3

import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import com.example.testcmp3.BrowserInput.Companion.TYPE_POPSTATE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import org.w3c.dom.Document
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event
import kotlin.getValue
import kotlin.setValue

internal fun historyString(list: List<Any?>, currentIndex: Int): String {
    return list.withIndex().joinToString(prefix = "[", postfix = "]") { (index, info) ->
        val suffix = if (index == currentIndex) {
            "*"
        } else {
            ""
        }
        "$info$suffix"
    }
}

internal fun Window.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event ->
        trySend(event as PopStateEvent)
    }
    window.addEventListener(TYPE_POPSTATE, callback)
    awaitClose {
        window.removeEventListener(TYPE_POPSTATE, callback)
    }
}

internal fun BrowserWindow.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event ->
        trySend(event as PopStateEvent)
    }
    addEventListener(TYPE_POPSTATE, callback)
    awaitClose {
        removeEventListener(TYPE_POPSTATE, callback)
    }
}

internal class BrowserHistoryImpl(private val window: Window): BrowserHistory {

    override val state: JsAny?
        get() = window.history.state

    override fun push(data: JsAny?, url: String?) {
        println("BrowserHistory.push($data, $url)")
        window.history.pushState(data, "", url)
    }

    override fun replace(data: JsAny?, url: String?) {
        println("BrowserHistory.replace($data, $url)")
        window.history.replaceState(data, "", url)
    }

    override suspend fun go(delta: Int) {
        println("BrowserHistory.go($delta)")
        if (delta == 0) return // Ignore "refresh" for now.
        window.history.go(delta)
        // TODO: Will get stuck if we go out of range. For example, if the history is [a, b*, c],
        // and we call `history.go(2)`, we'll be stuck here as the call will be ignored and we
        // won't receive a popstate event.
        window.createPopStateFlow().first()
    }
}

public interface BrowserHistory {
    public val state: JsAny?

    public fun push(data: JsAny?, url: String?)

    public fun replace(data: JsAny?, url: String?)

    public suspend fun go(delta: Int)
}

public interface BrowserDocument {
    public var title: String
}

internal class BrowserDocumentImpl(private val window: Window): BrowserDocument {
    override var title: String
        get() = window.document.title
        set(value) {
            println("setting title from ${window.document.title} to $value")

            // Hack: if the title is the same as `value` it won't trigger any change in the
            // dropdown menu, so we set it to empty string first.
            if (value == window.document.title) {
                window.document.title = ""
            }

            window.document.title = value
        }
}

public interface BrowserWindow {
    public val document: BrowserDocument
    public val history: BrowserHistory
    public fun addEventListener(type: String, callback: (Event) -> Unit)
    public fun removeEventListener(type: String, callback: (Event) -> Unit)
}

internal class BrowserWindowImpl(private val window: Window): BrowserWindow {
    override val document: BrowserDocument = BrowserDocumentImpl(window)

    override val history: BrowserHistory = BrowserHistoryImpl(window)

    override fun addEventListener(type: String, callback: (Event) -> Unit) {
        window.addEventListener(type, callback)
    }

    override fun removeEventListener(type: String, callback: (Event) -> Unit) {
        window.removeEventListener(type, callback)
    }
}