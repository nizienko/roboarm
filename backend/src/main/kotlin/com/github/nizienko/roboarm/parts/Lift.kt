package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.ArmGeometrySpecification
import com.github.nizienko.roboarm.DefaultArmSpecification
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

class Lift(
    scope: CoroutineScope,
    override val nextPart: RoboPart? = null,
    val driveSpecification: DriveSpecification = DefaultArmSpecification.value.lift,
    geometry: ArmGeometrySpecification = DefaultArmSpecification.value.geometry,
) : RoboPart {
    override val id: String = UUID.randomUUID().toString()

    private val drive = Drive(scope, driveSpecification)
    val height = drive.position

    fun setHeight(height: Double) = drive.moveTo(height)

    override val modelNode: String = "Lift"
    override val beam = geometry.upperArm
    override val toggles: List<ArmPartToggle>
        get() = listOf(drive.toggle)

    override val localPosition: Point3D
        get() = Point3D(0.0, 0.0, height.value)
    override val localRotationDegreesZ: Double
        get() = 0.0

    override fun applyCommand(command: ArmCommand) = drive.moveTo(command.value)
}
