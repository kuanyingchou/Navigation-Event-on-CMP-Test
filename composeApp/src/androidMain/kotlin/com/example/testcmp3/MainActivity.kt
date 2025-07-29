package com.example.testcmp3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.backhandler.LocalNavigationEventDispatcherOwner
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            LocalNavigationEventDispatcherOwner.provides(navigationEventDispatcherOwner = this)
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}