package com.github.nizienko.roboarm.data

import kotlinx.serialization.Serializable


@Serializable
data class ArmPartToggle(
    val name: String,
    val value: Double,
    val minValue: Double = 0.0,
    val maxValue: Double = 180.0
)

@Serializable
data class Mesh(
    val startPoint: Point3D,
    val endPoint: Point3D
)

@Serializable
data class Point3D(val x: Double, val y: Double, val z: Double)



@Serializable
data class ArmPart(
    val id: String,
    val toggles: List<ArmPartToggle>,
    val mesh: Mesh
)

@Serializable
data class ArmState(
    val time: Long = System.currentTimeMillis(),
    val parts: List<ArmPart>
)

@Serializable
data class ArmCommand(val partId: String, val toggleName: String, val value: Double)
