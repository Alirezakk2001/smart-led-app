package com.technest.smartled.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class Brightness(val value: Int) {
    init {
        require(value in 0..255) { "Brightness must be 0-255: $value" }
    }

    fun toPercent(): Int = (value * 100) / 255

    companion object {
        val Off = Brightness(0)
        val Full = Brightness(255)
        val Default = Brightness(128)

        fun fromPercent(percent: Int): Brightness {
            require(percent in 0..100) { "Percent must be 0-100: $percent" }
            return Brightness((percent * 255 + 50) / 100)
        }
    }
}
