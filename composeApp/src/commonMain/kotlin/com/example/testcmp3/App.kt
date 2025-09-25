package com.example.testcmp3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.testcmp3.Destination.*
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class Destination: NavigationEventInfo() {
    object Settings: Destination()
    object Connection: Destination()
    object Battery: Destination()
    object Health: Destination()
    object General: Destination()
    object About: Destination()
    object Language: Destination()

    override fun toString(): String {
        return this::class.simpleName.toString()
    }
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

@Composable
fun Content() {
    val backStack = remember { mutableStateListOf<Destination>(Settings) }

    val (current, parent, children) = when (backStack.lastOrNull()) {
        Settings ->
            Triple(Settings,
            null,
            listOf(Connection, Battery, General)
        )
        Connection -> Triple(
            Connection,
            Settings,
            listOf()
        )
        Battery -> Triple(
            Battery,
            Settings,
            listOf(Health),
        )
        Health -> Triple(
            Health,
            Battery,
            listOf(),
        )
        General -> Triple(
            General,
            Settings,
            listOf(About, Language),
        )
        About -> Triple(
            About,
            General,
            listOf(),
        )
        Language -> Triple(
            Language,
            General,
            listOf(),
        )
        null -> {
            error("Error: Back stack is empty!")
        }
    }
    Screen(current, parent, children, backStack)
}

@Composable
fun Screen(
    current: Destination,
    parent: Destination?,
    children: List<Destination>,
    backStack: MutableList<Destination>
) {
    Column {
        if (parent == null) {
            Text("This is the root screen")
        } else {
            Button(onClick = {
                backStack.removeLastOrNull() // pop
            }) {
                Text("Back to $parent")
            }
        }

        Text(text = backStack.joinToString(" > "), fontSize = 24.sp)

        for (child in children) {
            Button(onClick = {
                backStack.add(child) // push
            }) {
                Text(text = "$child")
            }
        }
    }

    val state = rememberNavigationEventState(
        currentInfo = current,
        backInfo = if (backStack.isEmpty()) emptyList() else backStack.dropLast(1),
        forwardInfo = if (children.size == 1) listOf(children.single()) else emptyList(),
    )
    NavigationEventHandler(
        state,
        onBackCompleted = {
            println("gyz:onBackCompleted")
            backStack.removeLastOrNull()
        },
        onForwardCompleted = {
            println("gyz:onForwardCompleted")
            if (children.size == 1) {
                backStack.add(children.single())
            }
        }
    )
}
