package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.Point3D
import java.util.UUID

class Basement(override val nextPart: RoboPart) : RoboPart {
    override val id: String = UUID.randomUUID().toString()
    override val modelNode: String = "basement"
    override val localPosition = Point3D(0.0, 0.0, 0.0)
    override val localRotationDegreesZ = 0.0
    override fun applyCommand(command: ArmCommand) = Unit
}
