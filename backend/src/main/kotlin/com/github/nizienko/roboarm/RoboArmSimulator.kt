package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.parts.Gripper
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Mast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RoboArmSimulator(private val coroutineScope: CoroutineScope) {

    private val gripper = Gripper()
    private val cran = Cran(gripper)
    private val lift = Lift(cran)
    private val mast = Mast(lift)

    private val partsById = generateSequence<RoboPart>(mast) { it.nextPart }.associateBy { it.id }

    private fun calcState(): ArmState = ArmState(parts = mast.toArmParts())

    private val _armStateFlow = MutableStateFlow(calcState())
    val armStateFlow: StateFlow<ArmState> = _armStateFlow.asStateFlow()

    init {
        coroutineScope.launch {
            combine(
                lift.height,
                mast.angleDegrees,
                cran.angleDegrees,
                gripper.angleDegrees,
                gripper.gripExpand,
            ) { _, _, _, _, _ ->
                calcState()
            }.collect {
                _armStateFlow.emit(it)
            }
        }
    }

    fun reportCommand(command: ArmCommand) {
        val part = partsById[command.partId] ?: return
        val toggle = part.toggles.find { it.name == command.toggleName } ?: return
        if (!command.value.isFinite() || command.value !in toggle.minValue..toggle.maxValue) return

        part.applyCommand(command)
        _armStateFlow.value = calcState()
    }
}

data class RotationDegrees3D(val x: Double, val y: Double, val z: Double)

interface RoboPart {
    val id: String
    fun applyCommand(command: ArmCommand)

    val localPosition: Point3D
    val localRotation: RotationDegrees3D
    val localMesh: Mesh
    val toggles: List<ArmPartToggle>
        get() = emptyList()
    val nextPart: RoboPart?
}
