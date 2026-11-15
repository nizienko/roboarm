package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.ArmGeometrySpecification
import com.github.nizienko.roboarm.DefaultArmSpecification
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

class Elbow(
    scope: CoroutineScope,
    override val nextPart: RoboPart? = null,
    val driveSpecification: DriveSpecification = DefaultArmSpecification.value.elbow,
    geometry: ArmGeometrySpecification = DefaultArmSpecification.value.geometry,
) : RoboPart {
    override val id: String = UUID.randomUUID().toString()

    private val drive = Drive(scope, driveSpecification)
    val angleDegrees = drive.position

    fun moveToAngle(degrees: Double) = drive.moveTo(degrees)

    override val modelNode: String = "Elbow"
    override val beam = geometry.forearm
    override val localPosition = Point3D(geometry.upperArm.length, 0.0, geometry.elbowHeightOffset)
    override val localRotationDegreesZ: Double
        get() = angleDegrees.value

    override val toggles: List<ArmPartToggle>
        get() = listOf(drive.toggle)

    override fun applyCommand(command: ArmCommand) = drive.moveTo(command.value)
}
