package com.example.testcmp3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationEventHandler
import com.example.testcmp3.Destination.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import testcmp3.composeapp.generated.resources.Res
import testcmp3.composeapp.generated.resources.compose_multiplatform
import kotlin.reflect.KClass

enum class Destination: NavigationEventInfo {
    Settings,
    Connection,
    Battery,
    General,
    About,
    Language,
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            Content()
        }
    }
}

@Composable fun Content() {
    val backStack = remember { mutableStateListOf<Destination>(Settings) }

    when (backStack.lastOrNull()) {
        Settings -> Screen(
            Settings,
            null,
            listOf(Connection, Battery, General),
            backStack
        )
        Connection -> Screen(
            Connection,
            Settings,
            listOf(),
            backStack
        )
        Battery -> Screen(
            Battery,
            Settings,
            listOf(),
            backStack
        )
        General -> Screen(
            General,
            Settings,
            listOf(About, Language),
            backStack
        )
        About -> Screen(
            About,
            General,
            listOf(),
            backStack
        )
        Language -> Screen(
            Language,
            General,
            listOf(),
            backStack
        )
        null -> {}
    }
}

@Composable
fun Screen(current: Destination, up: Destination?, downs: List<Destination>, backStack: MutableList<Destination>) {
    Column {
        if (up == null) {
            Text("Nowhere to go back to")
        } else {
            Button(onClick = {
                backStack.removeLastOrNull() // pop
            }) {
                Text("Back to $up")
            }
        }

        Text(text = backStack.joinToString(" > "), fontSize = 24.sp)

        for (down in downs) {
            Button(onClick = {
                backStack.add(down) // push
            }) {
                Text(text = "$down")
            }
        }
    }

    NavigationEventHandler(currentInfo = current, previousInfo = up) { progress ->
        try {
            progress.collect()
            backStack.removeLastOrNull()
        } catch (_: Exception) {
            // cancelled
        }
    }
}
