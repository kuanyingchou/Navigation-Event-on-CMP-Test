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
import com.example.testcmp3.Destination.*
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Destination: NavigationEventInfo {
    Settings,
    Connection,
    Battery,
    Health,
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

@Composable
fun Content() {
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
            listOf(Health),
            backStack
        )
        Health -> Screen(
            Health,
            Battery,
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
        null -> {
            Text("Error: Back stack is empty!")
        }
    }
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

    NavigationEventHandler(
        currentInfo = current,
        backInfo = if (backStack.isEmpty()) emptyList() else backStack.dropLast(1),
        forwardInfo = if (children.size == 1) listOf(children.single()) else emptyList(),
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
