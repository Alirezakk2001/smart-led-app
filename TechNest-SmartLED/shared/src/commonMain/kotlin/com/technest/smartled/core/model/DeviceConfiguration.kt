package com.technest.smartled.core.model

data class DeviceConfiguration(
    val lineCount: Int = 1,
    val ledsPerLine: List<Int> = listOf(60),
) {
    init {
        require(lineCount > 0) { "lineCount must be positive: $lineCount" }
        require(ledsPerLine.size == lineCount) {
            "ledsPerLine size ($ledsPerLine.size) must match lineCount ($lineCount)"
        }
        require(ledsPerLine.all { it > 0 }) { "All LED counts must be positive" }
    }
}
