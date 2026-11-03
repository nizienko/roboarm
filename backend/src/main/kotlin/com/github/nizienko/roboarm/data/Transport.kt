package com.github.nizienko.roboarm.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

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
data class ArmCommand(val partId: String, val toggleName: String, val value: Double) : ArmRequest

@Serializable(with = ArmRequestSerializer::class)
sealed interface ArmRequest

/** X/Y: gripper pivot in backend meters (Z-up). Z: lift height, applied directly. */
@Serializable
data class ArmTargetCommand(val target: Point3D) : ArmRequest

object ArmRequestSerializer : JsonContentPolymorphicSerializer<ArmRequest>(ArmRequest::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ArmRequest> =
        if ("target" in element.jsonObject) ArmTargetCommand.serializer() else ArmCommand.serializer()
}

@Serializable
data class ArmCommandError(val error: String)
