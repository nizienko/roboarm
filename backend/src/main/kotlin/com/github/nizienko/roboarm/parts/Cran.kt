package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Cran(scope: CoroutineScope, override val nextPart: RoboPart? = null): RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        moveToAngle(command.value)
    }

    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angle.asStateFlow()

    private val moveEngine = MoveEngine(
        scope, _angle,
        MoveEngineParameters(maxSpeed = 15.0, maxAcceleration = 10.0, minValue = -120.0, maxValue = 120.0),
    )

    fun moveToAngle(degrees: Double) {
        moveEngine.moveTo(degrees)
    }

    override val localMesh = Mesh(Point3D(-0.5, 0.0, 0.0), Point3D(10.5, 0.0, 0.0))
    override val localPosition: Point3D
        get() = Point3D(10.0, 0.0, -1.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, _angle.value)

    override val toggles: List<ArmPartToggle>
        get() = listOf(ArmPartToggle("Cran Rotation", _angle.value, minValue = -120.0, maxValue = 120.0, targetValue = moveEngine.target.value))

}
