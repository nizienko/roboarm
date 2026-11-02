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

    private val moveEngine: MoveEngine = MoveEngine(
        scope, _height,
        MoveEngineParameters(maxSpeed = 0.3, maxAcceleration = 0.5, minValue = 0.65, maxValue = 2.7),
    )

    fun setHeight(height: Double) {
        moveEngine.moveTo(height)
    }

    override val modelNode: String = "Lift"
    override val toggles: List<ArmPartToggle>
        get() = listOf(
            ArmPartToggle(
                "Height",
                _height.value,
                minValue = 0.65,
                maxValue = 2.7,
                targetValue = moveEngine.target.value,
                unit = "meters",
            ),
        )

    override val localPosition: Point3D
        get() = Point3D(0.0, 0.0, _height.value)
    override val localRotationDegreesZ: Double
        get() = 0.0

}
