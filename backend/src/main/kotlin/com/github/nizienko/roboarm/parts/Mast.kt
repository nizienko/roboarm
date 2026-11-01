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
        get() = listOf(ArmPartToggle("Rotation", _angle.value, maxValue = 360.0))

    override val localPosition: Point3D = Point3D(0.0, 0.0, 0.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, _angle.value)
}