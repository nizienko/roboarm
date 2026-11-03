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

class Hand(scope: CoroutineScope) : RoboPart {
    override val id: String = UUID.randomUUID().toString()
    private val _opening = MutableStateFlow(0.0)
    val opening: StateFlow<Double> = _opening.asStateFlow()

    val driveSpecification = DriveSpecification(
        name = "Hand Expand",
        unit = "meters",
        motion = MoveEngineParameters(maxSpeed = 0.05, maxAcceleration = 0.1, minValue = 0.0, maxValue = 0.18),
    )

    private val openingEngine = MoveEngine(scope, _opening, driveSpecification.motion)

    override val toggles: List<ArmPartToggle>
        get() = listOf(driveSpecification.toggle(_opening.value, openingEngine.target.value))

    override val modelNode: String = "Hand"
    override val nextPart: RoboPart? = null
    override val localPosition: Point3D
        get() = Point3D(-0.04 + opening.value, 0.0, -0.3)
    override val localRotationDegreesZ = 0.0
    override fun applyCommand(command: ArmCommand) {
        when (command.toggleName) {
            driveSpecification.name -> openingEngine.moveTo(command.value)
        }
    }
}
