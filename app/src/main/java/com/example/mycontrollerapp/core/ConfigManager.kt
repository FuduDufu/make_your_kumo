package com.example.mycontrollerapp.core

import android.content.Context

data class ServoDef(val name: String, val pin: String, val minus45: Int, val plus45: Int)
data class TouchSensorDef(val name: String, val pin: String, val highActive: Boolean)
data class SensorDef(val name: String, val pin: String, val v0: Double, val v1: Double)
data class WarnDef(val name: String, val durationSec: Int, val warn: Double, val shutoff: Double, val beepCount: Int)
data class RelayDef(val pin: String, val highActive: Boolean)

data class ModePreset(
    val name: String,
    val legRadius: Double,
    val cornerLegAngle: Double,
    val elongation: Double,
    val bodyLift: Double,
    val stepLift: Double,
    val verticalTouchCorrection: Double,
    val speedFactor: Double,
    val animationFactor: Double
)

data class RobotConfig(
    val servos: List<ServoDef>,
    val touchSensors: List<TouchSensorDef>,
    val sensors: List<SensorDef>,
    val warnings: List<WarnDef>,
    val relay: RelayDef?,
    val modes: Map<String, ModePreset>,
    val coxaLen: Double,
    val femurLen: Double,
    val tibiaLen: Double,
    val l1ToR1: Double,
    val l1ToL3: Double,
    val l2ToR2: Double,
    val legConnectionZ: Double,
    val legSittingZ: Double,
    val coxaAttachAngle: Double,
    val femurAttachAngle: Double,
    val tibiaAttachAngle: Double
)

class ConfigManager(private val context: Context) {
    fun load(assetName: String = "config-2040.txt"): RobotConfig {
        val servos = mutableListOf<ServoDef>()
        val touch = mutableListOf<TouchSensorDef>()
        val sensors = mutableListOf<SensorDef>()
        val warns = mutableListOf<WarnDef>()
        val modes = mutableMapOf<String, ModePreset>()
        var relay: RelayDef? = null

        // defaults (заавал шаардлагатай утгууд)
        var coxaLen = 0.0
        var femurLen = 0.0
        var tibiaLen = 0.0
        var l1ToR1 = 0.0
        var l1ToL3 = 0.0
        var l2ToR2 = 0.0
        var legConnectionZ = 0.0
        var legSittingZ = 0.0
        var coxaAttachAngle = 0.0
        var femurAttachAngle = 0.0
        var tibiaAttachAngle = 0.0

        val text = context.assets.open(assetName).bufferedReader().use { it.readText() }
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split(Regex("\\s+"))
            when {
                // Servo mapping: L11 P15 2000 1000
                Regex("^[LR]\\d\\d$").matches(parts[0]) -> {
                    servos += ServoDef(
                        name = parts[0],
                        pin = parts[1],
                        minus45 = parts[2].toInt(),
                        plus45 = parts[3].toInt()
                    )
                }

                // Touch sensors: TS_L1 P23 1
                parts[0].startsWith("TS_") -> {
                    touch += TouchSensorDef(parts[0], parts[1], parts[2] == "1")
                }

                // Analog sensors: CUR P24 0 0
                parts[0] == "CUR" || parts[0] == "VOL" -> {
                    sensors += SensorDef(parts[0], parts[1], parts[2].toDouble(), parts[3].toDouble())
                }

                // Warnings: WARN_CUR 2 8 10 3
                parts[0].startsWith("WARN_") -> {
                    warns += WarnDef(
                        name = parts[0],
                        durationSec = parts[1].toInt(),
                        warn = parts[2].toDouble(),
                        shutoff = parts[3].toDouble(),
                        beepCount = parts[4].toInt()
                    )
                }

                // Relay: RELAY P26 1
                parts[0] == "RELAY" -> {
                    relay = RelayDef(pin = parts[1], highActive = parts[2] == "1")
                }

                // Mode preset:
                // MODE_BLOCK 185 30 1.07 -40 80 0 1.0 1.0
                parts[0].startsWith("MODE_") -> {
                    modes[parts[0]] = ModePreset(
                        name = parts[0],
                        legRadius = parts[1].toDouble(),
                        cornerLegAngle = parts[2].toDouble(),
                        elongation = parts[3].toDouble(),
                        bodyLift = parts[4].toDouble(),
                        stepLift = parts[5].toDouble(),
                        verticalTouchCorrection = parts[6].toDouble(),
                        speedFactor = parts[7].toDouble(),
                        animationFactor = parts[8].toDouble()
                    )
                }

                parts[0] == "COXA_LEN" -> coxaLen = parts[1].toDouble()
                parts[0] == "FEMUR_LEN" -> femurLen = parts[1].toDouble()
                parts[0] == "TIBIA_LEN" -> tibiaLen = parts[1].toDouble()

                parts[0] == "L1_TO_R1" -> l1ToR1 = parts[1].toDouble()
                parts[0] == "L1_TO_L3" -> l1ToL3 = parts[1].toDouble()
                parts[0] == "L2_TO_R2" -> l2ToR2 = parts[1].toDouble()

                parts[0] == "LEG_CONNECTION_Z" -> legConnectionZ = parts[1].toDouble()
                parts[0] == "LEG_SITTING_Z" -> legSittingZ = parts[1].toDouble()

                parts[0] == "COXA_ATTACH_ANGLE" -> coxaAttachAngle = parts[1].toDouble()
                parts[0] == "FEMUR_ATTACH_ANGLE" -> femurAttachAngle = parts[1].toDouble()
                parts[0] == "TIBIA_ATTACH_ANGLE" -> tibiaAttachAngle = parts[1].toDouble()
            }
        }

        return RobotConfig(
            servos = servos,
            touchSensors = touch,
            sensors = sensors,
            warnings = warns,
            relay = relay,
            modes = modes,
            coxaLen = coxaLen,
            femurLen = femurLen,
            tibiaLen = tibiaLen,
            l1ToR1 = l1ToR1,
            l1ToL3 = l1ToL3,
            l2ToR2 = l2ToR2,
            legConnectionZ = legConnectionZ,
            legSittingZ = legSittingZ,
            coxaAttachAngle = coxaAttachAngle,
            femurAttachAngle = femurAttachAngle,
            tibiaAttachAngle = tibiaAttachAngle
        )
    }
}