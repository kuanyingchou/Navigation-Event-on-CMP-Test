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
import org.w3c.dom.Document
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
