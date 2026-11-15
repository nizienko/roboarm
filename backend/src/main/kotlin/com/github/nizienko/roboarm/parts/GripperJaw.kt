package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.ArmGeometrySpecification
import com.github.nizienko.roboarm.DefaultArmSpecification
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

class GripperJaw(
    scope: CoroutineScope,
    driveSpecification: DriveSpecification = DefaultArmSpecification.value.gripperJaw,
    private val geometry: ArmGeometrySpecification = DefaultArmSpecification.value.geometry,
) : RoboPart {
    override val id: String = UUID.randomUUID().toString()
    private val drive = Drive(scope, driveSpecification)
    val opening = drive.position

    override val toggles: List<ArmPartToggle>
        get() = listOf(drive.toggle)

    override val modelNode: String = "GripperJaw"
    override val nextPart: RoboPart? = null
    override val localPosition: Point3D
        get() = geometry.gripperOffset.copy(x = geometry.gripperOffset.x + opening.value)
    override val localRotationDegreesZ = 0.0

    override fun applyCommand(command: ArmCommand) = drive.moveTo(command.value)
}
