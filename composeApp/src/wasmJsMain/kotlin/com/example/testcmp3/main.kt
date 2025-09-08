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
import org.w3c.dom.History
import org.w3c.dom.PopStateEvent
import org.w3c.performance.PerformanceNavigation

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

    val history = window.history

    var oldInfo :Info? = null

    var todo: Info? = null

    var ignoreCount = 0

    fun createHistoryFromInfo(newInfo: Info, history: History) {
        val allInfo = newInfo.backInfo + newInfo.currentInfo + newInfo.forwardInfo
        println("gyz: allinfo: " + allInfo.joinToString() + ", before history.length = "+history.length)
        if (allInfo.isEmpty()) {
            error("No info provided!")
        }

        for((index, info) in allInfo.withIndex()) {
            val replaceOrPush = if (index == 0) {
                println("replacing")
                history::replaceState
            } else {
                println("pushing")
                history::pushState
            }
            replaceOrPush(info.toJsString(), "", "")
            println("gyz: setting title to $info")
            document.title = info
        }

        // Now we go back to "current".
        val stepsToGoBack = newInfo.forwardInfo.size
        if (stepsToGoBack > 0) {
            println("gyz: 2 go back $stepsToGoBack steps")
            history.go(-stepsToGoBack) // todo
            ignoreCount++
        }
        oldInfo = newInfo
        println("gyz: filled in history: ${allInfo.joinToString()}, history.length = ${history.length}")
    }

    // Clear all forward entries. This should disable the forward button but will add a new entry.
    fun History.clearForwardHistory() {
        println("before clear: ${history.length}")
        pushState(null, "")

        println("after clear: ${history.length}")
    }

    window.addEventListener("popstate") { event ->
        val popStateEvent = event as PopStateEvent
        val state = popStateEvent.state
        if (state == null) return@addEventListener

        val info = (state as JsString).toString()
        println("gyz: got popstate: $info, todo: $todo, ignoreCount: $ignoreCount")

        if (todo != null) {
            history.clearForwardHistory()
            println("gyz: current: ${history.state}")
            createHistoryFromInfo(todo!!, history)
            todo = null
        } else if (ignoreCount > 0) {
            ignoreCount--
            println("gyz: current: ${history.state}")
        } else if (oldInfo != null) {
            val backIndex = oldInfo.backInfo.indexOf(info)
            if (backIndex >= 0) {
                val step = oldInfo.backInfo.size - backIndex
                repeat(step) {
                    input.backCompleted()
                }
                return@addEventListener
            }
            val forwardIndex = oldInfo.forwardInfo.indexOf(info)
            if (forwardIndex >= 0) {
                repeat(forwardIndex + 1) {
                    input.forwardCompleted()
                }
                return@addEventListener
            }
        } else {
            println("what?")
        }
    }

    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            App()
        }

        LaunchedEffect(Unit) {
            dispatcher.state.map {
                Info.create(it.currentInfo, it.backInfo, it.forwardInfo)
            }.distinctUntilChanged()
                .collect { newInfo ->
                    println("gyz: got new info: ${newInfo}")

                    // Go back to the first entry of our app in the history if we're not there.
                    if (oldInfo != null) {
                        val stepsToGoBack = oldInfo.backInfo.size
                        if (stepsToGoBack > 0) {
                            println("gyz: 1 go back $stepsToGoBack steps")
                            todo = newInfo
                            history.go(-stepsToGoBack)
//                            window.dispatchEvent(
//                                PopStateEvent(type = "popstate", eventInitDict = PopStateEventInit(state = "".toJsString()))
//                            )
                            return@collect
                        }
                    }


//                    val nav = window.performance.navigation
//                    window.performance.timing
//                    println("nav: ${nav.type == PerformanceNavigation.TYPE_RELOAD}")

                    println("gyz: doesn't need to go back")
                    history.clearForwardHistory()
                    createHistoryFromInfo(newInfo, history)
                }
        }
    }
}

data class Info(val currentInfo: String, val backInfo: List<String>, val forwardInfo: List<String>) {
    companion object {
        fun <T> create(currentInfo: T, backInfo: List<T>, forwardInfo: List<T>): Info {
            return Info(currentInfo.toString(), backInfo.map { it.toString() }, forwardInfo.map { it.toString() })
        }
    }
}
