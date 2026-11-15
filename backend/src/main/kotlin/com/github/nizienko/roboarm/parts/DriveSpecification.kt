package com.github.nizienko.roboarm.parts

import com.github.nizienko.roboarm.data.ArmPartToggle

data class DriveSpecification(
    val name: String,
    val unit: String,
    val motion: MoveEngineParameters,
    val initialValue: Double = 0.0,
) {
    init {
        require(initialValue.isFinite() && initialValue in motion.range) {
            "Initial value for $name must be within its motion limits"
        }
        require(!motion.circled || (unit == "degrees" && motion.maxValue - motion.minValue == 360.0)) {
            "Continuous rotation for $name requires a 360-degree range"
        }
    }

    fun toggle(value: Double, target: Double): ArmPartToggle = ArmPartToggle(
        name = name,
        value = value,
        minValue = motion.minValue,
        maxValue = motion.maxValue,
        targetValue = target,
        unit = unit,
    )
}
