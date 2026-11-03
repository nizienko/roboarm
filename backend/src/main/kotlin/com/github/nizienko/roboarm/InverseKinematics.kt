package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.MoveEngineParameters
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal data class JointAngles(val mast: Double, val cran: Double)

/** Two planar links; try both elbow configurations within the actual joint limits. */
internal fun solveInverseKinematics(
    target: Point3D,
    firstLength: Double,
    secondLength: Double,
    mastMotion: MoveEngineParameters,
    cranMotion: MoveEngineParameters,
    currentMast: Double,
    currentCran: Double,
): JointAngles? {
    val mastLimits = mastMotion.range
    val cranLimits = cranMotion.range
    if (!target.x.isFinite() || !target.y.isFinite()) return null
    val cosine = (target.x * target.x + target.y * target.y - firstLength * firstLength -
        secondLength * secondLength) / (2.0 * firstLength * secondLength)
    if (!cosine.isFinite() || cosine < -1.0 - 1e-10 || cosine > 1.0 + 1e-10) return null
    val elbow = acos(cosine.coerceIn(-1.0, 1.0))
    return listOf(elbow, -elbow).flatMap { angle ->
        val cran = Math.toDegrees(angle)
        val mast = Math.toDegrees(atan2(target.y, target.x) -
            atan2(secondLength * sin(angle), firstLength + secondLength * cos(angle)))
        (-1..1).mapNotNull { turn ->
            val shoulder = mast + turn * 360.0
            if (shoulder >= mastLimits.start - 1e-9 && shoulder <= mastLimits.endInclusive + 1e-9 &&
                cran >= cranLimits.start - 1e-9 && cran <= cranLimits.endInclusive + 1e-9) {
                JointAngles(shoulder.coerceIn(mastLimits), cran.coerceIn(cranLimits))
            } else null
        }
    }.minByOrNull { abs(mastMotion.displacement(currentMast, it.mast)) +
        abs(cranMotion.displacement(currentCran, it.cran)) }
}
