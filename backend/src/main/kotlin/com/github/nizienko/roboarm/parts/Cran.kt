package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D

class Cran(override val nextPart: RoboPart? = null): RoboPart {
    override val localMesh = Mesh(Point3D(-10.0, 0.0, 0.0), Point3D(0.0, 0.0, 0.0))
    override val localPosition: Point3D
        get() = Point3D(10.0, 0.0, 0.0)
    override val localRotation: RotationDegrees3D
        get() = RotationDegrees3D(0.0, 0.0, 0.0)

}
