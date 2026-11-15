package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.DriveSpecification
import com.github.nizienko.roboarm.parts.MoveEngineParameters
import kotlinx.serialization.Serializable

/** Meters. Length is the distance between joint axes, not the exposed mesh length. */
@Serializable
data class BeamDimensions(val length: Double, val width: Double, val height: Double) {
    init {
        require(listOf(length, width, height).all { it.isFinite() && it > 0.0 }) {
            "Beam dimensions must be finite and positive"
        }
    }
}

/** Backend coordinates: meters, Z up, horizontal links along local +X. */
data class ArmGeometrySpecification(
    val upperArm: BeamDimensions,
    val forearm: BeamDimensions,
    val mastOffset: Point3D,
    val elbowHeightOffset: Double,
    val wristHeightOffset: Double,
    val gripperOffset: Point3D,
) {
    init {
        require(listOf(
            mastOffset.x, mastOffset.y, mastOffset.z,
            elbowHeightOffset, wristHeightOffset,
            gripperOffset.x, gripperOffset.y, gripperOffset.z,
        ).all { it.isFinite() }) { "Geometry offsets must be finite" }
        // The analytical IK solver assumes the lift axis is at world X/Y = 0.
        require(mastOffset.x == 0.0 && mastOffset.y == 0.0) {
            "The mast must be centered on the base axis"
        }
    }
}

data class ArmSpecification(
    val geometry: ArmGeometrySpecification,
    val mast: DriveSpecification,
    val lift: DriveSpecification,
    val elbow: DriveSpecification,
    val wrist: DriveSpecification,
    val gripperJaw: DriveSpecification,
)

/** Edit defaults here; each simulator can also receive its own specification. */
object DefaultArmSpecification {
    val value = ArmSpecification(
        geometry = ArmGeometrySpecification(
            upperArm = BeamDimensions(length = 1.0, width = 0.2, height = 0.2),
            forearm = BeamDimensions(length = 1.4, width = 0.2, height = 0.2),
            mastOffset = Point3D(0.0, 0.0, 0.2),
            elbowHeightOffset = -0.25,
            wristHeightOffset = -0.1,
            gripperOffset = Point3D(-0.04, 0.0, -0.3),
        ),
        mast = DriveSpecification(
            name = "Mast Rotation",
            unit = "degrees",
            motion = MoveEngineParameters(
                maxSpeed = 45.0, maxAcceleration = 25.0,
                minValue = 0.0, maxValue = 360.0, circled = true,
            ),
        ),
        lift = DriveSpecification(
            name = "Height",
            unit = "meters",
            motion = MoveEngineParameters(
                maxSpeed = 0.3, maxAcceleration = 0.5, minValue = 0.5, maxValue = 2.7,
            ),
            initialValue = 1.5,
        ),
        elbow = DriveSpecification(
            name = "Elbow rotation",
            unit = "degrees",
            motion = MoveEngineParameters(
                maxSpeed = 45.0, maxAcceleration = 25.0, minValue = -160.0, maxValue = 160.0,
            ),
        ),
        wrist = DriveSpecification(
            name = "Wrist rotation",
            unit = "degrees",
            motion = MoveEngineParameters(
                maxSpeed = 120.0, maxAcceleration = 60.0,
                minValue = 0.0, maxValue = 360.0, circled = true,
            ),
        ),
        gripperJaw = DriveSpecification(
            name = "Gripper opening",
            unit = "meters",
            motion = MoveEngineParameters(
                maxSpeed = 0.05, maxAcceleration = 0.1, minValue = 0.0, maxValue = 0.18,
            ),
        ),
    )
}
