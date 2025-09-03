package com.example.testcmp3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.w3c.dom.PopStateEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val dispatcher = NavigationEventDispatcher()
    val owner = object: NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher
            get() = dispatcher
    }
    val input = DirectNavigationEventInput().also {
        dispatcher.addInput(it)
    }
    window.addEventListener("popstate") { event ->
        // todo: back or forward or go?
        input.backCompleted()
    }
    val history = window.history

    var isFirstStart = true

    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            App()
        }
        LaunchedEffect(Unit) {
            dispatcher.state.map {
                it.currentInfo
            }.distinctUntilChanged().collect { info ->
                if (isFirstStart) {
                    history.replaceState(info.toString().toJsString(), "", info.toString())
                    isFirstStart = false
                } else {
                    history.pushState(info.toString().toJsString(), "", info.toString())
                }

                println("gyz: pushed ${info}, history length: ${history.length}")
            }
        }
    }
}

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