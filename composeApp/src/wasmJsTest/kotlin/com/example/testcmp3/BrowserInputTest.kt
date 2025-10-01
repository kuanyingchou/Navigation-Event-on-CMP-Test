package com.example.testcmp3

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.w3c.dom.PopStateEvent
import org.w3c.dom.PopStateEventInit
import org.w3c.dom.events.Event
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserInputTest {

    private object A: NavigationEventInfo()
    private object B: NavigationEventInfo()
    private object C: NavigationEventInfo()
    private object X: NavigationEventInfo()
    private object Y: NavigationEventInfo()
    private object Z: NavigationEventInfo()

    @Test
    fun initialStateWithSingleInfo() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        advanceUntilIdle()

        assertEquals(jsNumberList(0), window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun initialStateWithMultipleInfos() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        handler.setInfos(listOf(A, B, C), 1)
        dispatcher.addHandler(handler)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun changeDestination() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1), window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun newInfosAreLonger() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 1)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(1, window.history.index)
    }

    @Test
    fun newInfosAreShorter() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        handler.setInfos(listOf(A, B, C), 1)
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeDestinationAndHierarchicalBack() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)
        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1), window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeDestinationAndChronologicalBack() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {}
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)
        handler.setInfos(listOf(A, B), 0)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1), window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun browserBackWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {
            override fun onBackCompleted() {
                if (backInfo.isNotEmpty()) {
                    setInfo(
                        backInfo.last(),
                        backInfo.dropLast(1),
                        listOf(currentInfo) + forwardInfo
                    )
                }
            }
        }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 1)

        advanceUntilIdle()

        window.history.go(-1)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1), window.history.states)
        assertEquals(0, window.history.index)

        assertEquals(listOf(A, B), dispatcher.history.value.mergedHistory)
        assertEquals(0, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserForwardWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true, isForwardEnabled = true) {
            override fun onForwardCompleted() {
                println("receive forward")
                if (forwardInfo.isNotEmpty()) {
                    setInfo(
                        forwardInfo.first(),
                        backInfo + listOf(currentInfo),
                        forwardInfo.drop(1)
                    )
                }
            }
        }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B), 0)

        advanceUntilIdle()

        window.history.go(1)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1), window.history.states)
        assertEquals(1, window.history.index)

        assertEquals(listOf(A, B), dispatcher.history.value.mergedHistory)
        assertEquals(1, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserMultipleBackWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        var invokedCount = 0
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {
            override fun onBackCompleted() {
                invokedCount++
                if (backInfo.isNotEmpty()) {
                    setInfo(
                        backInfo.last(),
                        backInfo.dropLast(1),
                        listOf(currentInfo) + forwardInfo
                    )
                }
            }
        }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 2)

        advanceUntilIdle()

        window.history.go(-2)

        advanceUntilIdle()

        assertEquals(2, invokedCount)
        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(0, window.history.index)

        assertEquals(listOf(A, B, C), dispatcher.history.value.mergedHistory)
        assertEquals(0, dispatcher.history.value.currentIndex)
    }

    @Test
    fun browserMultipleForwardWorks() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        var invokedCount = 0
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true, isForwardEnabled = true) {
            override fun onForwardCompleted() {
                invokedCount++
                if (forwardInfo.isNotEmpty()) {
                    setInfo(
                        forwardInfo.first(),
                        backInfo + listOf(currentInfo),
                        forwardInfo.drop(1)
                    )
                }
            }
        }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 0)

        advanceUntilIdle()

        window.history.go(2)

        advanceUntilIdle()

        assertEquals(2, invokedCount)
        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(2, window.history.index)

        assertEquals(listOf(A, B, C), dispatcher.history.value.mergedHistory)
        assertEquals(2, dispatcher.history.value.currentIndex)
    }

    @Test
    fun goesToAnInvalidEntryGoesBackDirectly() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {
            override fun onForwardCompleted() {
                require(false) { "Should not be called"}
            }
        }
        dispatcher.addHandler(handler)

        handler.setInfos(listOf(A, B, C), 2)
        handler.setInfos(listOf(A), 0)

        advanceUntilIdle()

        window.history.go(2)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(0, window.history.index)
    }

    @Test
    fun changeHandlersShouldWork() = runTest {
        val window = TestWindow()
        val dispatcher = NavigationEventDispatcher()
        val input = BrowserInput(window, this.coroutineContext[CoroutineDispatcher]!!)
        dispatcher.addInput(input)
        val handler = object: NavigationEventHandler<NavigationEventInfo>(A, true) {
            override fun onForwardCompleted() {
                require(false) { "Should not be called"}
            }
        }
        handler.setInfos(listOf(A, B, C), 0)
        dispatcher.addHandler(handler)

        val handler2 = object: NavigationEventHandler<NavigationEventInfo>(A, true) {
            override fun onForwardCompleted() {
                require(false) { "Should not be called"}
            }
        }
        handler2.setInfos(listOf(X, Y, Z), 2)
        dispatcher.addHandler(handler2)

        advanceUntilIdle()

        assertEquals(jsNumberList(0, 1, 2), window.history.states)
        assertEquals(2, window.history.index)
    }
}

private fun <T: NavigationEventInfo> NavigationEventHandler<T>.setInfos(entries: List<T>, currentIndex: Int) {
    setInfo(entries[currentIndex], entries.take(currentIndex), entries.drop(currentIndex + 1))
}

private fun jsNumberList(vararg elements: Int): List<JsNumber> {
    return elements.map { it.toJsNumber() }
}

private class TestWindow: BrowserWindow {
    override val document: TestBrowserDocument = TestBrowserDocument()
    override val history: TestBrowserHistory = TestBrowserHistory(this)
    val eventListeners = mutableMapOf<String, MutableList<(Event) -> Unit>>()

    override fun addEventListener(
        type: String,
        callback: (Event) -> Unit
    ) {
        val callbackList = eventListeners.getOrPut(type) { mutableListOf() }
        callbackList.add(callback)
    }

    override fun removeEventListener(
        type: String,
        callback: (Event) -> Unit
    ) {
        eventListeners[type]?.remove(callback)
    }
}

private class TestBrowserDocument: BrowserDocument {
    override var title: String = ""
}

private class TestBrowserHistory(private val window: TestWindow): BrowserHistory {

    data class Entry(val state: JsAny?, val url: String?)

    val entries = mutableListOf<Entry>(Entry(null, null))

    val states
        get() = entries.map { it.state }

    var index = 0
        private set

    override val state: JsAny?
        get() = entries[index].state

    override fun push(data: JsAny?, url: String?) {
        // Removing
        for (i in entries.size - 1 downTo index + 1) {
            entries.removeAt(i)
        }

        // Adding
        entries.add(Entry(data, url))
        index++
    }

    override fun replace(data: JsAny?, url: String?) {
        // println("replace ${entries[index]} with ${Entry(data, url)}")
        entries[index] = Entry(data, url)
    }

    override suspend fun go(delta: Int) {
        println("go($delta)")
        index += delta
        window.eventListeners["popstate"]?.forEach {
            it.invoke(
                PopStateEvent(
                    "popstate",
                    PopStateEventInit(entries[index].state, false, false, false)
                )
            )
        }
    }
}