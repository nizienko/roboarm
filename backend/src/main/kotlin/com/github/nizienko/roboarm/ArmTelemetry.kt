package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPart
import com.github.nizienko.roboarm.data.Point3D

internal fun RoboPart.toArmParts(): List<ArmPart> = buildList {
    var part: RoboPart? = this@toArmParts
    var worldPosition = Point3D(0.0, 0.0, 0.0)
    var worldRotationDegreesZ = 0.0
    while (part != null) {
        val current = part

        worldPosition += current.localPosition.rotateZ(worldRotationDegreesZ)
        worldRotationDegreesZ += current.localRotationDegreesZ
        add(ArmPart(
            id = current.id,
            modelNode = current.modelNode,
            position = Point3D(worldPosition.x, worldPosition.z, -worldPosition.y),
            rotationDegreesZ = worldRotationDegreesZ,
            toggles = current.toggles,
        ))
        part = current.nextPart
    }
}
