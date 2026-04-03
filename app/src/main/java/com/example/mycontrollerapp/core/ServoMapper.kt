// core/ServoMapper.kt
package com.example.mycontrollerapp.core

import kotlin.math.roundToInt

class ServoMapper(
    private val boardScale: BoardScale = BoardScale.ANGLE_DEG_DIRECT
) {
    enum class BoardScale { ANGLE_DEG_DIRECT, PULSE_US_DIRECT }

    fun angleDegToPulseUs(def: ServoDef, angleDeg: Double): Double {
        val a = angleDeg.coerceIn(-45.0, 45.0)
        val t = (a + 45.0) / 90.0
        return def.minus45 + (def.plus45 - def.minus45) * t
    }

    fun angleDegToBoardValue(def: ServoDef, angleDeg: Double): Int {
        val pulseUs = angleDegToPulseUs(def, angleDeg)
        return when (boardScale) {
            BoardScale.ANGLE_DEG_DIRECT -> angleDeg.roundToIntSafe()
            BoardScale.PULSE_US_DIRECT -> pulseUs.roundToIntSafe()
        }
    }
}

private fun Double.roundToIntSafe(): Int {
    if (!isFinite()) return 0
    return roundToInt()
}