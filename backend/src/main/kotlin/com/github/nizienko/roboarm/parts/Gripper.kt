package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class Gripper(override val nextPart: RoboPart? = null) : RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        when (command.toggleName) {
            "Gripper Rotation" -> _angle.value = command.value
            "Gripper Expand" -> _gripExpand.value = command.value
        }
    }
    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angle.asStateFlow()

    private val _gripExpand = MutableStateFlow(5.0)
    val gripExpand: StateFlow<Double> = _gripExpand.asStateFlow()


    override val toggles: List<ArmPartToggle>
        get() = listOf(
            ArmPartToggle("Gripper Rotation", _angle.value, maxValue = 360.0),
            ArmPartToggle("Gripper Expand", _gripExpand.value, maxValue = 6.0)
        )


    override val localPosition: Point3D = Point3D(10.0, 0.0, -1.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, _angle.value)
    override val localMesh: Mesh
        get() = Mesh(Point3D(-1.0 - _gripExpand.value/2.0, 0.0, 0.0), Point3D(1.0 + _gripExpand.value/2.0, 0.0, 0.0))
}