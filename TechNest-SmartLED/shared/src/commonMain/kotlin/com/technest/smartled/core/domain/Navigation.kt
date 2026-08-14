package com.technest.smartled.core.domain

sealed class Screen {
    data object Dashboard : Screen()
    data object Effects : Screen()
    data object Devices : Screen()
    data object Settings : Screen()
    data object Setup : Screen()
}
