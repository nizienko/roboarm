package com.github.nizienko.roboarm.data

import kotlinx.serialization.Serializable

@Serializable
data class ArmPartToggle(
    val name: String,
    val value: Double,
    val minValue: Double = 0.0,
    val maxValue: Double = 180.0,
    val targetValue: Double = value,
    val unit: String,
)

@Serializable
data class Point3D(val x: Double, val y: Double, val z: Double)

/** Absolute position in Y-up scene meters; angle around backend Z (scene Y), in degrees. */
@Serializable
data class ArmPart(
    val id: String,
    val modelNode: String,
    val position: Point3D,
    val rotationDegreesZ: Double,
    val toggles: List<ArmPartToggle>,
)

@Serializable
data class ArmState(
    val time: Long = System.currentTimeMillis(),
    val parts: List<ArmPart>,
)

@Serializable
data class ArmCommand(val partId: String, val toggleName: String, val value: Double)
