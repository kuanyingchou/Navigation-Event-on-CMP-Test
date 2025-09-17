package com.example.testcmp3

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.example.testcmp3.BrowserInput.Companion.TYPE_POPSTATE
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            //Test(BrowserHistory(window))
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun Test(browserHistory: BrowserHistory) {
    Column {
        var text by remember { mutableStateOf("n/a") }
        val hist = remember { mutableStateListOf<String>("?") }
        var index by remember { mutableStateOf(0) }
        var count by remember { mutableStateOf(0)}

        val historyEntries = hist.withIndex().joinToString { (i, info) ->
            if (i == index) {
                "$info*"
            } else {
                "$info"
            }
        }
        Text(text = "[$historyEntries]")

        Button(onClick = {
            val oldState = browserHistory.state
            browserHistory.replace(count.toString().toJsString())
            document.title = count.toString()
            println("replace $oldState with $count")
            text = count.toString()
            hist[index] = text
            count++
        }) {
            Text("Replace")
        }
        Button(onClick = {
            browserHistory.push(count.toString().toJsString())
            document.title = count.toString()
            println("push $count")
            text = count.toString()
            hist.removeRange(index+1, hist.size)
            hist.add(count.toString())
            index++
            count++
        }) {
            Text("Push")
        }
        Button(onClick = {
            GlobalScope.launch {
                val event = browserHistory.go(-1)
                text = event.state.toString()
                if (index > 0) index--
            }
        }) {
            Text("Go back")
        }
        Button(onClick = {
            GlobalScope.launch {
                val event = browserHistory.go(1)
                text = event.state.toString()
                if (index < hist.size - 1) index++
            }
        }) {
            Text("Go forward")
        }
    }
}

class BrowserInput(private val window: Window): NavigationEventInput() {

    private var navigationEventHistory : NavigationEventHistory? = null

    private val browserHistory = BrowserHistory(window)

    companion object {
        const val TYPE_POPSTATE = "popstate"
        const val RESERVED_TITLE = "[reserved]"
    }

    private var processPopState = true

    private var processHistoryChange = true

    @OptIn(DelicateCoroutinesApi::class)
    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        println("onAdded")
        GlobalScope.launch {
            window.createPopStateFlow().collect(::onPopState)
        }
        // Use the first entry as the reserved entry.
        browserHistory.replace((-1).toJsNumber())
        document.title = RESERVED_TITLE
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onPopState(popStateEvent: PopStateEvent) {
        println("onPopState: index: ${popStateEvent.state}")
        if (!processPopState) return
        val state = popStateEvent.state ?: return

        val newIndex = (state as JsNumber).toInt()

        if (navigationEventHistory == null) return

        val currentIndex = navigationEventHistory!!.currentIndex

        if (newIndex < 0) {
            // User goes to our reserved entry, so we move them to the first entry.
            GlobalScope.launch {
                browserHistory.go(1)
            }
        } else {
            if (newIndex < currentIndex) {
                val timesToGoBack = currentIndex - newIndex
                processHistoryChange = false
                repeat(timesToGoBack - 1) {
                    dispatchOnBackCompleted()
                }
                processHistoryChange = true
                dispatchOnBackCompleted()
            } else if (newIndex > currentIndex) {
                val timesToGoForward = newIndex - currentIndex
                processHistoryChange = false
                repeat(timesToGoForward - 1) {
                    dispatchOnForwardCompleted()
                }
                processHistoryChange = true
                dispatchOnForwardCompleted()
            } else if (newIndex == currentIndex){
                println("index == current !?")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onHistoryChanged(history: NavigationEventHistory) {
        println("gyz: onHistoryChanged, new: ${history.debugString()}, current: ${navigationEventHistory?.debugString()}")
        if (!processHistoryChange) return

        // We may get None first when disposing the previous destination
        if (history.currentIndex < 0 ||
            history.mergedHistory[history.currentIndex] == NavigationEventInfo.None
        ) return

        GlobalScope.launch {
            updateBrowserHistory(history)
        }
    }

    suspend fun updateBrowserHistory(
        newHistory: NavigationEventHistory
    ) {
        if (newHistory.mergedHistory.isEmpty() || newHistory.currentIndex < 0) {
            return
        }

        // Go back to the reserved entry.
        if (navigationEventHistory != null) {
            val browserHistoryState = browserHistory.state
            val stepsToGoBack = (browserHistoryState as? JsNumber)?.toInt() ?: error("got state $browserHistoryState")
            println("steps to go back: $stepsToGoBack")
            processPopState = false
            val event = browserHistory.go(-(stepsToGoBack + 1))
            processPopState = true
            println("After going back: ${browserHistory.state} vs ${event.state}")
        }

        // Make sure we're at the beginning
        val currentIndex = (browserHistory.state as JsNumber).toInt()
        check(currentIndex == -1) {
            "current index = $currentIndex!?"
        }

        // Start pushing new entries.
        document.title = RESERVED_TITLE // otherwise the title is still the same as before going back.
        for ((index, info) in newHistory.mergedHistory.withIndex()) {
            println("before push: ${document.title}")
            browserHistory.push(index.toJsNumber(), "#${info}")
            document.title = info.toString() // todo: title?
            println("setting title to $info")
        }

        // Now we go back to "current".
        val stepsToGoBack = newHistory.mergedHistory.size - newHistory.currentIndex - 1
        if (stepsToGoBack > 0) {
            processPopState = false
            browserHistory.go(-stepsToGoBack)
            processPopState = true
        }

        // Update history.
        navigationEventHistory = newHistory
    }
}

// utils

private fun NavigationEventHistory.debugString(): String {
    val historyEntries = mergedHistory.withIndex().joinToString { (index, info) ->
        if (index == currentIndex) {
            "$index: $info*"
        } else {
            "$index: $info"
        }
    }
    return "[$historyEntries]"
}

private fun Window.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event ->
        trySend(event as PopStateEvent)
    }
    window.addEventListener(TYPE_POPSTATE, callback)
    awaitClose {
        window.removeEventListener(TYPE_POPSTATE, callback)
    }
}

class BrowserHistory(private val window: Window) {
    val state: JsAny?
        get() = window.history.state

    fun push(data: JsAny?, url: String? = null) {
        println("BrowserHistory.push($data, $url)")
        window.history.pushState(data, "", url)
    }

    fun replace(data: JsAny?, url: String? = null) {
        println("BrowserHistory.replace($data, $url)")
        window.history.replaceState(data, "", url)
    }

    suspend fun go(delta: Int): PopStateEvent {
        println("BrowserHistory.go($delta)")

        window.history.go(delta)
        return window.createPopStateFlow().first() // todo: will get stuck if we go out of range.

//        val state = window.history.state
//        window.history.go(delta)
//        while (window.history.state == state) {
//            delay(100)
//        }
//        println("end of BrowserHistory.go($delta)")
//        return state
    }
}


