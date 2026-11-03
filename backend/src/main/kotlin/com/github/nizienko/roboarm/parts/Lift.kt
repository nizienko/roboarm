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

class Lift(
    scope: CoroutineScope,
    override val nextPart: RoboPart? = null
): RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        setHeight(command.value)
    }

    private val _height = MutableStateFlow(1.5)
    val height: StateFlow<Double> = _height.asStateFlow()

    val driveSpecification = DriveSpecification(
        name = "Height",
        unit = "meters",
        motion = MoveEngineParameters(maxSpeed = 0.3, maxAcceleration = 0.5, minValue = 0.45, maxValue = 2.7),
    )

    private val moveEngine = MoveEngine(scope, _height, driveSpecification.motion)

    fun setHeight(height: Double) {
        moveEngine.moveTo(height)
    }

    override val modelNode: String = "Lift"
    override val toggles: List<ArmPartToggle>
        get() = listOf(driveSpecification.toggle(_height.value, moveEngine.target.value))

    override val localPosition: Point3D
        get() = Point3D(0.0, 0.0, _height.value)
    override val localRotationDegreesZ: Double
        get() = 0.0

}
