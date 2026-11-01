package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Mast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RoboArmSimulator(private val coroutineScope: CoroutineScope) {

    private val cran = Cran()
    private val lift = Lift(cran)
    private val mast = Mast(lift)

    private fun calcState(): ArmState = ArmState(parts = mast.toArmParts())

    private val _armStateFlow = MutableStateFlow(calcState())
    val armStateFlow: StateFlow<ArmState> = _armStateFlow.asStateFlow()

    init {
        coroutineScope.launch {
            combine(lift.height, mast.angleDegrees) { _, _ ->
                calcState()
            }.collect {
                _armStateFlow.emit(it)
            }
        }
    }

    fun reportCommand(command: ArmCommand) {
        val part = generateSequence<RoboPart>(mast) { it.nextPart }
            .toList().getOrNull(command.partIndex) ?: return
        val toggle = part.toggles.find { it.name == command.toggleName } ?: return
        if (!command.value.isFinite() || command.value !in toggle.minValue..toggle.maxValue) return

        when (part) {
            is Mast -> part.moveToAngle(command.value)
            is Lift -> part.setHeight(command.value)
            else -> return
        }
        _armStateFlow.value = calcState()
    }
}

data class RotationDegrees3D(val x: Double, val y: Double, val z: Double)

interface RoboPart {
    val localPosition: Point3D
    val localRotation: RotationDegrees3D
    val localMesh: Mesh
    val toggles: List<ArmPartToggle>
        get() = emptyList()
    val nextPart: RoboPart?
}
