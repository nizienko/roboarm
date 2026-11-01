package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.Point3D
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class Mast(override val nextPart: RoboPart? = null): RoboPart {
    private val _angle = MutableStateFlow(0.0)
    val angleDegrees: SharedFlow<Double> = _angle.asSharedFlow()

    fun moveToAngle(degrees: Double) {
        // todo: add speed here
        _angle.value = degrees
    }

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

    override val localPosition: Point3D
        get() = Point3D(0.0, 0.0, _height.value)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}

class Cran(override val nextPart: RoboPart? = null): RoboPart {
    override val localPosition: Point3D
        get() = Point3D(10.0, 0.0, 0.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}
