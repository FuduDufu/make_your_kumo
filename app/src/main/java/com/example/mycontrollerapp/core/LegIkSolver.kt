package com.example.mycontrollerapp.core

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LegIkSolver {

    data class Geometry(
        val coxaLen: Double,
        val femurLen: Double,
        val tibiaLen: Double,
        val legConnectionZ: Double,
        val legSittingZ: Double,
        val coxaAttachAngle: Double,
        val femurAttachAngle: Double,
        val tibiaAttachAngle: Double
    )

    data class ModeBlockParams(
        val legRadius: Double,
        val elongation: Double,
        val bodyLift: Double
    )

    data class LegSetup(
        val legName: String,
        val coxaServo: String,
        val femurServo: String,
        val tibiaServo: String,
        val worldAngleDeg: Double,
        val invertFemurTibia: Boolean
    )

    data class LegAngles(
        val coxaDeg: Double,
        val femurDeg: Double,
        val tibiaDeg: Double
    )

    fun solveBlockPoseLeg(
        leg: LegSetup,
        geometry: Geometry,
        mode: ModeBlockParams
    ): LegAngles {
        val r = mode.legRadius * mode.elongation
        val ang = Math.toRadians(leg.worldAngleDeg)

        val x = r * cos(ang)
        val y = r * sin(ang)
        val z = geometry.legSittingZ - mode.bodyLift

        val coxaWorldDeg = Math.toDegrees(atan2(y, x))

        val horizontal = sqrt(x * x + y * y)
        val px = horizontal - geometry.coxaLen
        val pz = z - geometry.legConnectionZ

        var d = sqrt(px * px + pz * pz)
        d = clamp(d, 1.0, geometry.femurLen + geometry.tibiaLen - 1e-6)

        val cosKnee = clamp(
            (geometry.femurLen.pow(2) + geometry.tibiaLen.pow(2) - d.pow(2)) /
                    (2.0 * geometry.femurLen * geometry.tibiaLen),
            -1.0,
            1.0
        )
        val kneeInnerDeg = Math.toDegrees(acos(cosKnee))

        val cosFemur = clamp(
            (geometry.femurLen.pow(2) + d.pow(2) - geometry.tibiaLen.pow(2)) /
                    (2.0 * geometry.femurLen * d),
            -1.0,
            1.0
        )
        val femurPartDeg = Math.toDegrees(acos(cosFemur))
        val lineDeg = Math.toDegrees(atan2(pz, px))

        var coxaServoDeg = coxaWorldDeg - geometry.coxaAttachAngle
        var femurServoDeg = (lineDeg + femurPartDeg) - geometry.femurAttachAngle
        var tibiaServoDeg = (180.0 - kneeInnerDeg) - geometry.tibiaAttachAngle

        if (leg.invertFemurTibia) {
            femurServoDeg = -femurServoDeg
            tibiaServoDeg = -tibiaServoDeg
        }

        coxaServoDeg = clamp(coxaServoDeg, -45.0, 45.0)
        femurServoDeg = clamp(femurServoDeg, -45.0, 45.0)
        tibiaServoDeg = clamp(tibiaServoDeg, -45.0, 45.0)

        return LegAngles(
            coxaDeg = coxaServoDeg,
            femurDeg = femurServoDeg,
            tibiaDeg = tibiaServoDeg
        )
    }

    private fun clamp(x: Double, a: Double, b: Double): Double {
        return maxOf(a, minOf(x, b))
    }
}