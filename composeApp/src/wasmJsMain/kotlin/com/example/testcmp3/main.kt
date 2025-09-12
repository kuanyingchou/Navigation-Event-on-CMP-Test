package com.example.testcmp3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.History
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val dispatcher = NavigationEventDispatcher()
    val owner = object: NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher
            get() = dispatcher
    }
    val input = BrowserInput(window)
    dispatcher.addInput(input)

    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            App()
        }
    }
}

class BrowserInput(window: Window): NavigationEventInput() {

    private val history = window.history
    private var oldInfos : Infos? = null
    private var todo: Infos? = null
    private var ignoreCount = 0

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        window.addEventListener("popstate", ::onPopState)
    }

    override fun onRemoved() {
        window.removeEventListener("popstate", ::onPopState)
    }

    private fun onPopState(event: Event) {
        val popStateEvent = event as PopStateEvent
        val state = popStateEvent.state
        if (state == null) return

        val restoredInfos = (state as JsString).toString()
        println("gyz: got popstate: $restoredInfos, todo: $todo, ignoreCount: $ignoreCount")

        if (todo != null) {
            history.clearForwardHistory()
            println("gyz: current: ${history.state}")
            createHistoryFromInfo(todo!!)
            todo = null
        } else if (ignoreCount > 0) {
            ignoreCount--
            println("gyz: current: ${history.state}")
        } else if (oldInfos != null) {
            val backIndex = oldInfos!!.backInfo.indexOf(restoredInfos)
            if (backIndex >= 0) {
                val step = oldInfos!!.backInfo.size - backIndex
                repeat(step) {
                    dispatchOnBackCompleted()
                }
                return
            }
            val forwardIndex = oldInfos!!.forwardInfo.indexOf(restoredInfos)
            if (forwardIndex >= 0) {
                repeat(forwardIndex + 1) {
                    dispatchOnForwardCompleted()
                }
                return
            }
        } else {
            println("what?")
        }
    }

    override fun onInfoChanged(
        currentInfo: NavigationEventInfo,
        backInfo: List<NavigationEventInfo>,
        forwardInfo: List<NavigationEventInfo>
    ) {
        // We may get None first when disposing the previous destination
        if (currentInfo == NavigationEventInfo.None) return

        val newInfos = Infos.create(currentInfo, backInfo, forwardInfo)

        println("gyz: got new info: ${newInfos}")

        // Go back to the first entry of our app in the history if we're not there.
        if (oldInfos != null) {
            val stepsToGoBack = oldInfos!!.backInfo.size
            if (stepsToGoBack > 0) {
                println("gyz: 1 go back $stepsToGoBack steps")
                todo = newInfos
                history.go(-stepsToGoBack)
                return
            }
            println("gyz: doesn't need to go back")
            history.clearForwardHistory()
        }

        createHistoryFromInfo(newInfos)
    }

    fun createHistoryFromInfo(newInfo: Infos) {
        val combinedInfos = newInfo.backInfo + newInfo.currentInfo + newInfo.forwardInfo
        println("gyz: allinfo: " + combinedInfos.joinToString() + ", before history.length = " + history.length)
        if (combinedInfos.isEmpty()) {
            error("No info provided!")
        }

        for ((index, info) in combinedInfos.withIndex()) {
            val replaceOrPush = if (index == 0) {
                history::replaceState
            } else {
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
        oldInfos = newInfo
        println("gyz: filled in history: ${combinedInfos.joinToString()}, history.length = ${history.length}")
    }

    // Clear all forward entries. This should disable the forward button but will add a new entry.
    private fun History.clearForwardHistory() {
        pushState(null, "")
    }
}

data class Infos(val currentInfo: String, val backInfo: List<String>, val forwardInfo: List<String>) {
    companion object Companion {
        fun <T> create(currentInfo: T, backInfo: List<T>, forwardInfo: List<T>): Infos {
            return Infos(currentInfo.toString(), backInfo.map { it.toString() }, forwardInfo.map { it.toString() })
        }
    }
}
