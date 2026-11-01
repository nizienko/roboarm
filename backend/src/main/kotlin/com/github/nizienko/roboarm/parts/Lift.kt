package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Lift(override val nextPart: RoboPart? = null): RoboPart {
    override val id: String = UUID.randomUUID().toString()

    override fun applyCommand(command: ArmCommand) {
        setHeight(command.value)
    }

    private val _height = MutableStateFlow(25.0)
    val height: StateFlow<Double> = _height.asStateFlow()

    fun setHeight(height: Double) {
        _height.value = height
    }

    override val localMesh: Mesh
        get() = Mesh(Point3D(-0.5, 0.0, 0.0), Point3D(10.5, 0.0, 0.0))
    override val toggles: List<ArmPartToggle>
        get() = listOf(ArmPartToggle("Height", _height.value, minValue = 5.0, maxValue = 30.0))

    override val localPosition: Point3D
        get() = Point3D(1.0, 0.0, _height.value)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}