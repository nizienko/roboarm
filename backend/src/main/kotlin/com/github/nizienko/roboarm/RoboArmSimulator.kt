package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmTargetCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.Basement
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.parts.Gripper
import com.github.nizienko.roboarm.parts.Hand
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Mast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RoboArmSimulator(private val coroutineScope: CoroutineScope) {

    private val hand = Hand(coroutineScope)
    private val gripper = Gripper(coroutineScope, hand)
    private val cran = Cran(coroutineScope, gripper)
    private val lift = Lift(coroutineScope, cran)
    private val mast = Mast(coroutineScope, lift)

    private val basement = Basement(mast)

    private val partsById = generateSequence<RoboPart>(basement) { it.nextPart }.associateBy { it.id }

    private fun calcState(): ArmState = ArmState(parts = basement.toArmParts())

    private val _armStateFlow = MutableStateFlow(calcState())
    val armStateFlow: StateFlow<ArmState> = _armStateFlow.asStateFlow()

    init {
        coroutineScope.launch {
            combine(
                lift.height,
                mast.angleDegrees,
                cran.angleDegrees,
                gripper.angleDegrees,
                hand.opening,
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

    fun reportTarget(command: ArmTargetCommand): Boolean {
        val target = command.target
        val height = lift.driveSpecification.motion
        // The command specifies the gripper pivot in world coordinates, not lift travel.
        val heightOffset = basement.localPosition.z + mast.localPosition.z +
            cran.localPosition.z + gripper.localPosition.z + hand.localPosition.z
        val liftHeight = target.z - heightOffset
        if (!liftHeight.isFinite() || liftHeight < height.minValue - 1e-9 ||
            liftHeight > height.maxValue + 1e-9) return false
        val angles = solveInverseKinematics(
            target, cran.localPosition.x, gripper.localPosition.x,
            mast.driveSpecification.motion, cran.driveSpecification.motion,
            mast.angleDegrees.value, cran.angleDegrees.value,
        ) ?: return false
        mast.moveToAngle(angles.mast)
        cran.moveToAngle(angles.cran)
        lift.setHeight(liftHeight.coerceIn(height.range))
        _armStateFlow.value = calcState()
        return true
    }
}

interface RoboPart {
    val id: String
    fun applyCommand(command: ArmCommand)

    val localPosition: Point3D
    /** Rotation relative to the parent around local Z, in degrees. */
    val localRotationDegreesZ: Double
    val modelNode: String
    val toggles: List<ArmPartToggle>
        get() = emptyList()
    val nextPart: RoboPart?
}
