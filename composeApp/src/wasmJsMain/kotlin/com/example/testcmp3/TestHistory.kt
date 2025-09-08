package com.example.testcmp3

import kotlinx.browser.window
import org.w3c.dom.PopStateEvent

fun testHistoryAPI() {
    val history = window.history

    // Replace top entry, `data` can be any Json object, `title` is not used, `url` is what comes after domain, e.g. `www.mysite.com/hello` if it's "hello".
    history.replaceState("1".toJsString(), "", "1")

    // Push a new entry.
    history.pushState("2".toJsString(), "", "2")

    // Get the length of the history.
    history.length

    // Go back, same as clicking the browser back button.
    history.back()

    // Go forward, same as clicking the browser forward button.
    history.forward()

    // Go anywhere in the history, e.g. go(-1) is back, go(1) is forward, go() or go(0) is refresh.
    history.go(-1)

    window.addEventListener("popstate") { event ->
        val popStateEvent = event as PopStateEvent
        println("Got ${popStateEvent.state}")
    }
}