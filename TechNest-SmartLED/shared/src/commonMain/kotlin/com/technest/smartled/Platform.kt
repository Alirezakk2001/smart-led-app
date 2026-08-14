package com.technest.smartled

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform