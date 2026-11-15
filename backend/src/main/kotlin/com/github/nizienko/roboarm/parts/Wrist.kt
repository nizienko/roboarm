package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.ArmGeometrySpecification
import com.github.nizienko.roboarm.DefaultArmSpecification
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

class Wrist(
    scope: CoroutineScope,
    override val nextPart: RoboPart? = null,
    val driveSpecification: DriveSpecification = DefaultArmSpecification.value.wrist,
    geometry: ArmGeometrySpecification = DefaultArmSpecification.value.geometry,
) : RoboPart {
    override val id: String = UUID.randomUUID().toString()

    private val drive = Drive(scope, driveSpecification)
    val angleDegrees = drive.position

    override val toggles: List<ArmPartToggle>
        get() = listOf(drive.toggle)

    override val localPosition = Point3D(geometry.forearm.length, 0.0, geometry.wristHeightOffset)
    override val localRotationDegreesZ: Double
        get() = angleDegrees.value
    override val modelNode: String = "Wrist"

    override fun applyCommand(command: ArmCommand) = drive.moveTo(command.value)
}
