package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class Gripper(scope: CoroutineScope, override val nextPart: RoboPart? = null) : RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        when (command.toggleName) {
            "Gripper Rotation" -> rotationEngine.moveTo(command.value)
        }
    }
    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angle.asStateFlow()

    private val rotationEngine = MoveEngine(
        scope, _angle,
        MoveEngineParameters(maxSpeed = 120.0, maxAcceleration = 60.0, minValue = 0.0, maxValue = 360.0),
    )


    override val toggles: List<ArmPartToggle>
        get() = listOf(
            ArmPartToggle("Gripper Rotation", _angle.value, maxValue = 360.0, targetValue = rotationEngine.target.value, unit = "degrees"),
        )


    override val localPosition: Point3D = Point3D(1.0, 0.0, -0.05)
    override val localRotationDegreesZ: Double
        get() = _angle.value
    override val modelNode: String = "Gripper"
}
