package com.example.testcmp3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.NavigationEventHandler
import org.jetbrains.compose.resources.painterResource
import testcmp3.composeapp.generated.resources.Res
import testcmp3.composeapp.generated.resources.compose_multiplatform
import kotlin.collections.plus

var id = 0

@Composable
fun testNavigationEvent() {
    var showContent by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var progressState by remember { mutableStateOf(0f) }
        var touchXState by remember { mutableStateOf(0f) }
        var touchYState by remember { mutableStateOf(0f) }
        var events by remember { mutableStateOf(listOf<String>()) }

//        NavigationEventHandler { progress ->
//            events += "started"
//            try {
//                progress.collect { e ->
//                    progressState = e.progress
//                    touchXState = e.touchX
//                    touchYState = e.touchY
//                    events += "${id} (${e.hashCode()}): $e"
//                    id++
//                }
//                events += "completed"
//            } catch(e: Exception) {
//                events += "cancelled"
//            }
//        }

        Text("touchX")
        Slider(
            value = touchXState / LocalWindowInfo.current.containerSize.width,
            onValueChange = {})
        Text("touchY")
        Slider(
            value = touchYState / LocalWindowInfo.current.containerSize.height,
            onValueChange = {})
        Text("progress")
        Slider(value = progressState, onValueChange = {})
        events.takeLast(5).forEach {
            Text(it)
            Box(modifier = Modifier.height(10.dp))
        }

        AnimatedVisibility(showContent) {
            val greeting = remember { Greeting().greet() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(Res.drawable.compose_multiplatform), null)
                Text("Compose: $greeting")
            }
        }

        var enabled by remember { mutableStateOf(true) }
        if (enabled) {
            Button(onClick = {
                enabled = false
            }) {}
        }
    }

}