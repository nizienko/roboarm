package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Elbow(scope: CoroutineScope, override val nextPart: RoboPart? = null): RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        moveToAngle(command.value)
    }

    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angle.asStateFlow()

    val driveSpecification = DriveSpecification(
        name = "Elbow rotation",
        unit = "degrees",
        motion = MoveEngineParameters(maxSpeed = 45.0, maxAcceleration = 25.0, minValue = -160.0, maxValue = 160.0),
    )

    private val moveEngine = MoveEngine(scope, _angle, driveSpecification.motion)

    fun moveToAngle(degrees: Double) {
        moveEngine.moveTo(degrees)
    }

    override val modelNode: String = "Elbow"
    override val localPosition: Point3D
        get() = Point3D(1.0, 0.0, -0.25)
    override val localRotationDegreesZ: Double
        get() = _angle.value

    override val toggles: List<ArmPartToggle>
        get() = listOf(driveSpecification.toggle(_angle.value, moveEngine.target.value))

}
