package com.example.mycontrollerapp.core

import android.util.Log

class RobotPoseBuilder(
    private val config: RobotConfig,
    private val mapper: ServoMapper
) {
    private val tag = "RobotPoseBuilder"

    private val servoByName: Map<String, ServoDef> by lazy {
        config.servos.associateBy { it.name }
    }

    private val ikSolver = LegIkSolver()

    fun buildCenterAllFromConfig(): IntArray {
        val values = IntArray(18) { 1500 }

        for (def in config.servos) {
            val pin = parsePinIndex(def.pin) ?: continue
            if (pin in 0..17) {
                values[pin] = ((def.minus45 + def.plus45) / 2.0).toInt()
            }
        }

        return values
    }

    fun buildBlockAllFromConfig(): IntArray {
        val values = buildCenterAllFromConfig()

        val modeBlock = config.modes["MODE_BLOCK"]
            ?: error("MODE_BLOCK not found in config")

        val geometry = LegIkSolver.Geometry(
            coxaLen = config.coxaLen,
            femurLen = config.femurLen,
            tibiaLen = config.tibiaLen,
            legConnectionZ = config.legConnectionZ,
            legSittingZ = config.legSittingZ,
            coxaAttachAngle = config.coxaAttachAngle,
            femurAttachAngle = config.femurAttachAngle,
            tibiaAttachAngle = config.tibiaAttachAngle
        )

        val mode = LegIkSolver.ModeBlockParams(
            legRadius = modeBlock.legRadius,
            elongation = modeBlock.elongation,
            bodyLift = modeBlock.bodyLift
        )

        val legs = listOf(
            LegIkSolver.LegSetup("L1", "L11", "L12", "L13", 30.0, false),
            LegIkSolver.LegSetup("L2", "L21", "L22", "L23", 0.0, false),
            LegIkSolver.LegSetup("L3", "L31", "L32", "L33", -30.0, false),
            LegIkSolver.LegSetup("R1", "R11", "R12", "R13", -30.0, true),
            LegIkSolver.LegSetup("R2", "R21", "R22", "R23", 0.0, true),
            LegIkSolver.LegSetup("R3", "R31", "R32", "R33", 30.0, true)
        )

        for (leg in legs) {
            val angles = ikSolver.solveBlockPoseLeg(leg, geometry, mode)

            applyServoAngle(values, leg.coxaServo, angles.coxaDeg)
            applyServoAngle(values, leg.femurServo, angles.femurDeg)
            applyServoAngle(values, leg.tibiaServo, angles.tibiaDeg)

            Log.d(
                tag,
                "${leg.legName}: deg=(${fmt(angles.coxaDeg)}, ${fmt(angles.femurDeg)}, ${fmt(angles.tibiaDeg)})"
            )
        }

        return values
    }

    private fun applyServoAngle(values: IntArray, servoName: String, angleDeg: Double) {
        val def = servoByName[servoName] ?: return
        val pin = parsePinIndex(def.pin) ?: return
        if (pin !in 0..17) return
        values[pin] = mapper.angleDegToBoardValue(def, angleDeg)
    }

    private fun parsePinIndex(pin: String): Int? {
        if (!pin.startsWith("P")) return null
        return pin.drop(1).toIntOrNull()
    }

    private fun fmt(v: Double): String = String.format("%.1f", v)
}