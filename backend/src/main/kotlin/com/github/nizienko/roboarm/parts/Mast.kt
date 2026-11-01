package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Mast(override val nextPart: RoboPart? = null): RoboPart {
    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: StateFlow<Double> = _angle.asStateFlow()

    fun moveToAngle(degrees: Double) {
        // todo: add speed here
        _angle.value = degrees
    }

    override val localMesh = Mesh(Point3D(0.0, 0.0, 0.0), Point3D(0.0, 0.0, 30.0))
    override val toggles: List<ArmPartToggle>
        get() = listOf(ArmPartToggle("Rotation", _angle.value))

    override val localPosition: Point3D = Point3D(0.0, 0.0, 0.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, _angle.value)
}

class Lift(override val nextPart: RoboPart? = null): RoboPart {
    private val _height = MutableStateFlow(5.0)
    val height: StateFlow<Double> = _height.asStateFlow()

    fun setHeight(height: Double) {
        _height.value = height
    }

    // The local frame is at the top of the lift; its mesh extends back to the parent.
    override val localMesh: Mesh
        get() = Mesh(Point3D(0.0, 0.0, -_height.value), Point3D(0.0, 0.0, 0.0))
    override val toggles: List<ArmPartToggle>
        get() = listOf(ArmPartToggle("Height", _height.value, minValue = 0.0, maxValue = 30.0))

    override val localPosition: Point3D
        get() = Point3D(0.0, 0.0, _height.value)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}

class Cran(override val nextPart: RoboPart? = null): RoboPart {
    override val localMesh = Mesh(Point3D(-10.0, 0.0, 0.0), Point3D(0.0, 0.0, 0.0))
    override val localPosition: Point3D
        get() = Point3D(10.0, 0.0, 0.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}
