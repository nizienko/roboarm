package com.github.nizienko.roboarm.parts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class Drive(scope: CoroutineScope, private val specification: DriveSpecification) {
    private val positionState = MutableStateFlow(specification.initialValue)
    private val engine = MoveEngine(scope, positionState, specification.motion)
    val position = positionState.asStateFlow()
    val toggle
        get() = specification.toggle(position.value, engine.target.value)

    fun moveTo(value: Double) = engine.moveTo(value)
}
