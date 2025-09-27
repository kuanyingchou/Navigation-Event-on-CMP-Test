package com.example.testcmp3

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window

class BrowserInput internal constructor(
    private val browserWindow: BrowserWindow,
    private val coroutineScope: CoroutineScope = MainScope()
): NavigationEventInput() {

    companion object {
        const val TYPE_POPSTATE = "popstate"
    }

    public constructor(window: Window): this(BrowserWindowImpl(window))
    private var currentHistory : MyNavigationEventHistory? = null

    private var processPopState = true

    private var processHistoryChange = true

    private val browserHistory = browserWindow.history

    private val browserDocument = browserWindow.document

    @OptIn(DelicateCoroutinesApi::class)
    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        coroutineScope.launch {
            browserWindow.createPopStateFlow().collect(::onPopState)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onPopState(popStateEvent: PopStateEvent) {
        if (!processPopState) {
            return
        }
        val state = popStateEvent.state ?: return

        val newIndex = (state as JsNumber).toInt()

        val currentHistory = currentHistory

        if (currentHistory == null) return
        val currentIndex = currentHistory.index

        // println("newIndex: $newIndex, currentIndex: $currentIndex")
        if (newIndex != currentIndex && currentHistory.entries[newIndex] == Invalid) {
            // User goes to an invalid entry, so we move them back.
            val delta = currentHistory.index - newIndex
            println("moving back from ${newIndex} to ${newIndex+delta}")
            coroutineScope.launch {
                disableOnPopStateCallback {
                    browserHistory.go(delta)
                }
            }
        } else {
            if (newIndex < currentIndex) {
                val timesToGoBack = currentIndex - newIndex
                disableHistoryUpdateCallback {
                    repeat(timesToGoBack - 1) {
                        dispatchOnBackCompleted()
                    }
                }
                dispatchOnBackCompleted()
            } else if (newIndex > currentIndex) {
                val timesToGoForward = newIndex - currentIndex
                disableHistoryUpdateCallback {
                    repeat(timesToGoForward - 1) {
                        dispatchOnForwardCompleted()
                    }
                }
                dispatchOnForwardCompleted()
            } else { // newIndex == currentIndex
                println("index == current == $newIndex !?")
            }
            this@BrowserInput.currentHistory = MyNavigationEventHistory(currentHistory.entries, newIndex)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onHistoryChanged(history: NavigationEventHistory) {
        //println("gyz: onHistoryChanged, current: ${currentHistory?.debugString()}, new: ${history.debugString()}")
        if (!processHistoryChange) {
            //println("onHistoryChanged ignored")
            return
        }

        // We may get None first when disposing the previous destination
        if (history.currentIndex < 0 ||
            history.mergedHistory[history.currentIndex] == NavigationEventInfo.None
        ) {
            //println("skipped empty history")
            return
        }

        coroutineScope.launch {
            disableOnPopStateCallback {
                currentHistory = updateBrowserHistory(currentHistory, MyNavigationEventHistory(history))
            }
        }
    }

    internal suspend fun updateBrowserHistory(
        oldHistory: MyNavigationEventHistory?,
        newHistory: MyNavigationEventHistory
    ): MyNavigationEventHistory? {
        println("updateBrowserHistory(oldHistory=${oldHistory?.debugString()}, newHistory=${newHistory.debugString()})")
        if (newHistory.entries.isEmpty() || newHistory.index < 0) {
            return oldHistory
        }
        return if (oldHistory == null) { // App first started
            var index = 0
            if (newHistory.entries.isNotEmpty()) {
                val info = newHistory.entries.first()
                browserHistory.replace(index.toJsNumber(), "#${info}")
                browserDocument.title = info.toString()
                index++
            }
            for (info in newHistory.entries.drop(1)) {
                browserHistory.push(index.toJsNumber(), "#${info}")
                browserDocument.title = info.toString()
                index++
            }
            // Go back to current index.
            val delta = newHistory.entries.size - newHistory.index - 1
            if (delta != 0) {
                browserHistory.go(delta)
            }
            newHistory
        } else {
            if (oldHistory.entries == newHistory.entries) {
                if (newHistory.index != oldHistory.index) {
                    // The infos stay the same, only index changed, then go there directly.
                    browserHistory.go(newHistory.index - oldHistory.index)
                }
                newHistory
            } else {
                // Go back to the beginning, e.g. [0, 1, 2*] > [0*, 1, 2]
                if (oldHistory.index > 0) {
                    browserHistory.go(-oldHistory.index)
                }

                // Start replacing or pushing new entries
                for ((index, info) in newHistory.entries.withIndex()) {
                    if (index < oldHistory.entries.size) {
                        if (info != oldHistory.entries[index]) {
                            browserHistory.replace(
                                index.toJsNumber(),
                                "#${info}"
                            )
                            browserDocument.title = info.toString()
                        } else {
                            println("skip $index as the infos are the same: $info")
                        }
                        if (index < newHistory.entries.size - 1 &&
                            index < oldHistory.entries.size - 1) {
                            browserHistory.go(1)
                        }
                    } else {
                        browserHistory.push(index.toJsNumber(), "#${info}")
                        browserDocument.title = info.toString()
                    }
                }
                println("after adding or replacing: ${browserHistory.state}")

                // If there are extra entries in the browser history, push an Invalid entry to remove them.
                // [0, 1]
                // [0, Invalid], oldHistory: [0]
                //
                val updatedNewHistory = if (newHistory.entries.size < oldHistory.entries.size) {
                    browserHistory.push(newHistory.entries.size.toJsNumber(), "#invalid")
                    browserDocument.title = "Invalid"
                    MyNavigationEventHistory(newHistory.entries + Invalid, newHistory.index)
                } else {
                    newHistory
                }

                // Go back to current index. [0, 1, 2*] > [0, 1*, 2]
                val lastIndex = updatedNewHistory.entries.size - 1
                val delta = updatedNewHistory.index - lastIndex
                if (delta != 0) {
                    browserHistory.go(delta)
                }
                updatedNewHistory
            }
        }
    }

    private inline fun disableOnPopStateCallback(content: () -> Unit) {
        processPopState = false
        content()
        processPopState = true
    }

    private inline fun disableHistoryUpdateCallback(content: () -> Unit) {
        processHistoryChange = false
        content()
        processHistoryChange = true
    }

    private object Invalid: NavigationEventInfo()
}
