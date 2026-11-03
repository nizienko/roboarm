package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmPartToggle
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmTargetCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.Base
import com.github.nizienko.roboarm.parts.Elbow
import com.github.nizienko.roboarm.parts.Wrist
import com.github.nizienko.roboarm.parts.GripperJaw
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Mast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RoboArmSimulator(private val coroutineScope: CoroutineScope) {

    private val gripperJaw = GripperJaw(coroutineScope)
    private val wrist = Wrist(coroutineScope, gripperJaw)
    private val elbow = Elbow(coroutineScope, wrist)
    private val lift = Lift(coroutineScope, elbow)
    private val mast = Mast(coroutineScope, lift)

    private val base = Base(mast)

    private val partsById = generateSequence<RoboPart>(base) { it.nextPart }.associateBy { it.id }

    private fun calcState(): ArmState = ArmState(parts = base.toArmParts())

    private val _armStateFlow = MutableStateFlow(calcState())
    val armStateFlow: StateFlow<ArmState> = _armStateFlow.asStateFlow()

    init {
        coroutineScope.launch {
            combine(
                lift.height,
                mast.angleDegrees,
                elbow.angleDegrees,
                wrist.angleDegrees,
                gripperJaw.opening,
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
        val heightOffset = base.localPosition.z + mast.localPosition.z +
            elbow.localPosition.z + wrist.localPosition.z + gripperJaw.localPosition.z
        val liftHeight = target.z - heightOffset
        if (!liftHeight.isFinite() || liftHeight < height.minValue - 1e-9 ||
            liftHeight > height.maxValue + 1e-9) return false
        val angles = solveInverseKinematics(
            target, elbow.localPosition.x, wrist.localPosition.x,
            mast.driveSpecification.motion, elbow.driveSpecification.motion,
            mast.angleDegrees.value, elbow.angleDegrees.value,
        ) ?: return false
        mast.moveToAngle(angles.mast)
        elbow.moveToAngle(angles.elbow)
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
