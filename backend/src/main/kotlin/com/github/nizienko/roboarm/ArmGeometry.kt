package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPart
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D

internal fun RoboPart.toArmParts(): List<ArmPart> = buildList {
    var part: RoboPart? = this@toArmParts
    var worldPosition = Point3D(0.0, 0.0, 0.0)
    var worldRotation = RotationDegrees3D(0.0, 0.0, 0.0).toMatrix()

    while (part != null) {
        val current = part
        // T_world_child = T_world_parent * T_parent_child.
        worldPosition += current.localPosition.rotate(worldRotation)
        worldRotation = multiply(worldRotation, current.localRotation.toMatrix())

        val mesh = current.localMesh
        add(ArmPart(
            id = current.id,
            toggles = current.toggles,
            mesh = Mesh(
                startPoint = worldPosition + mesh.startPoint.rotate(worldRotation),
                endPoint = worldPosition + mesh.endPoint.rotate(worldRotation),
            ),
        ))
        part = current.nextPart
    }
}
