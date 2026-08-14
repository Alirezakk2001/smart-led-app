package com.technest.smartled.core.model

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    init {
        require(red in 0..255) { "Red must be 0-255: $red" }
        require(green in 0..255) { "Green must be 0-255: $green" }
        require(blue in 0..255) { "Blue must be 0-255: $blue" }
    }

    companion object {
        val Black = RgbColor(0, 0, 0)
        val White = RgbColor(255, 255, 255)
        val Red = RgbColor(255, 0, 0)
        val Green = RgbColor(0, 255, 0)
        val Blue = RgbColor(0, 0, 255)
    }
}
