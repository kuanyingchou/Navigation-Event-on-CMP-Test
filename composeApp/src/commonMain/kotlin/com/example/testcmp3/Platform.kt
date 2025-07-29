package com.example.testcmp3

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform