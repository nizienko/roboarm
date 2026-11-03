package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.data.ArmPartToggle

data class DriveSpecification(
    val name: String,
    val unit: String,
    val motion: MoveEngineParameters,
) {
    fun toggle(value: Double, target: Double): ArmPartToggle = ArmPartToggle(
        name = name,
        value = value,
        minValue = motion.minValue,
        maxValue = motion.maxValue,
        targetValue = target,
        unit = unit,
    )
}
