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

    private suspend fun updateBrowserHistory(
        oldHistory: MyNavigationEventHistory?,
        newHistory: MyNavigationEventHistory
    ): MyNavigationEventHistory? {
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
            val current = newHistory.entries.size - 1
            val delta = newHistory.index - current
            if (delta != 0) {
                browserHistory.go(delta)
            }
            newHistory
        } else {
            if (oldHistory.entries == newHistory.entries) {
                if (newHistory.index != oldHistory.index) {
                    // The infos stay the same, only index changed, then go there directly.
                    browserHistory.go(newHistory.index - oldHistory.index)
                    browserDocument.title = newHistory.entries[newHistory.index].toString()
                }
                newHistory
            } else {
                // Go back to the beginning, e.g. [0, 1, 2*] > [0*, 1, 2]
                if (oldHistory.index > 0) {
                    browserHistory.go(-oldHistory.index)
                }

                // Start replacing or pushing new entries
                return if (oldHistory.entries.size >= newHistory.entries.size) {
                    println("updateBrowserHistory(oldHistory=${oldHistory.debugString()}, newHistory=${newHistory.debugString()})")
                    val newEntries = mutableListOf<NavigationEventInfo>()
                    newEntries.addAll(newHistory.entries)

                    for ((index, info) in oldHistory.entries.withIndex()) {
                        if (index < newHistory.entries.size) {
                            val newInfo = newHistory.entries[index]
                            if (info != newInfo) {
                                browserHistory.replace(index.toJsNumber(), "#${newInfo}")
                            }
                            browserDocument.title = newInfo.toString()
                        } else {
                            if (info != Invalid) {
                                browserHistory.replace(index.toJsNumber(), "#invalid")
                            }
                            // Hack: if the title is already Invalid setting it to the same string
                            // doesn't trigger any change in the dropdown menu, so we set it to
                            // empty string first.
                            browserDocument.title = ""

                            browserDocument.title = "Invalid"
                            newEntries.add(Invalid)
                        }
                        if (index < oldHistory.entries.size - 1) {
                            browserHistory.go(1)
                        }
                    }
                    val current = oldHistory.entries.size - 1
                    val delta = newHistory.index - current
                    browserHistory.go(delta)
                    MyNavigationEventHistory(newEntries, newHistory.index)
                } else { // newHistory.entries.size > oldHistory.entries.size
                    for ((index, info) in newHistory.entries.withIndex()) {
                        if (index < oldHistory.entries.size) {
                            if (info != oldHistory.entries[index]) {
                                browserHistory.replace(
                                    index.toJsNumber(),
                                    "#${info}"
                                )
                            }
                            browserDocument.title = info.toString()
                            if (index < oldHistory.entries.size - 1) {
                                browserHistory.go(1)
                            }
                        } else {
                            browserHistory.push(index.toJsNumber(), "#${info}")
                            browserDocument.title = info.toString()
                        }
                    }
                    val current = newHistory.entries.size - 1
                    val delta = newHistory.index - current
                    browserHistory.go(delta)
                    newHistory
                }
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

// The constructors of NavigationEventHistory are not public.
private class MyNavigationEventHistory(val entries: List<NavigationEventInfo>, val index: Int) {
    constructor(history: NavigationEventHistory): this(history.mergedHistory, history.currentIndex)
}

private fun MyNavigationEventHistory.debugString(): String {
    return historyString(this.entries, this.index)
}