import com.github.nizienko.roboarm.ArmSpecification
import com.github.nizienko.roboarm.BeamDimensions
import com.github.nizienko.roboarm.RoboArmSimulator
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.ArmTargetCommand
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArmSpecificationTest {
    private val defaults = testArmSpecification
    private val custom = defaults.copy(geometry = defaults.geometry.copy(
        upperArm = BeamDimensions(1.5, 0.18, 0.16),
        forearm = BeamDimensions(0.75, 0.14, 0.12),
    ))

    @Test
    fun unequalBeamDimensionsDriveWorldPosesAndSurviveSerialization() {
        val specification = custom.copy(
            mast = custom.mast.copy(initialValue = 90.0),
            elbow = custom.elbow.copy(initialValue = -90.0),
        )
        withSimulator(specification) { simulator ->
            val state = simulator.armStateFlow.value
            val lift = state.parts.single { it.modelNode == "Lift" }
            val elbow = state.parts.single { it.modelNode == "Elbow" }
            val wrist = state.parts.single { it.modelNode == "Wrist" }
            assertEquals(custom.geometry.upperArm, lift.beam)
            assertEquals(custom.geometry.forearm, elbow.beam)
            assertEquals(null, wrist.beam)
            assertEquals(0.0, elbow.position.x, 1e-9)
            assertEquals(-1.5, elbow.position.z, 1e-9)
            assertEquals(0.75, wrist.position.x, 1e-9)
            assertEquals(-1.5, wrist.position.z, 1e-9)
            assertEquals(1.4, wrist.position.y, 1e-9)

            val json = Json { encodeDefaults = true }
            val encoded = json.encodeToString(state)
            assertEquals(state, json.decodeFromString<ArmState>(encoded))
        }
    }

    @Test
    fun inverseKinematicsUsesConfiguredLengthsAndOffsets() {
        withSimulator(custom) { simulator ->
            assertTrue(simulator.reportTarget(ArmTargetCommand(Point3D(2.25, 0.0, 1.2))))
            assertFalse(simulator.reportTarget(ArmTargetCommand(Point3D(2.26, 0.0, 1.2))))
            assertTrue(simulator.reportTarget(ArmTargetCommand(Point3D(1.5, 0.75, 1.2))))
            val parts = simulator.armStateFlow.value.parts
            assertEquals(0.0, parts.single { it.modelNode == "Mast" }.toggles.single().targetValue, 1e-9)
            assertEquals(90.0, parts.single { it.modelNode == "Elbow" }.toggles.single().targetValue, 1e-9)
            assertEquals(1.3, parts.single { it.modelNode == "Lift" }.toggles.single().targetValue, 1e-9)
        }
        withSimulator(defaults) { simulator ->
            assertFalse(simulator.reportTarget(ArmTargetCommand(Point3D(2.25, 0.0, 1.2))))
        }
        withSimulator(custom.copy(geometry = custom.geometry.copy(wristHeightOffset = -0.15))) { simulator ->
            assertTrue(simulator.reportTarget(ArmTargetCommand(Point3D(2.25, 0.0, 1.2))))
            assertEquals(1.4, simulator.armStateFlow.value.parts.single {
                it.modelNode == "Lift"
            }.toggles.single().targetValue, 1e-9)
        }
    }

    @Test
    fun configuredInitialPoseAndJointLimitsApplyToCommandsAndIK() {
        val specification = custom.copy(elbow = custom.elbow.copy(
            initialValue = 20.0,
            motion = custom.elbow.motion.copy(minValue = 10.0, maxValue = 80.0, maxSpeed = 15.0, maxAcceleration = 10.0),
        ))
        withSimulator(specification) { simulator ->
            val elbow = simulator.armStateFlow.value.parts.single { it.modelNode == "Elbow" }
            val toggle = elbow.toggles.single()
            assertEquals(20.0, toggle.value)
            assertEquals(20.0, toggle.targetValue)
            assertEquals(10.0, toggle.minValue)
            assertEquals(80.0, toggle.maxValue)
            simulator.reportCommand(ArmCommand(elbow.id, toggle.name, 90.0))
            assertEquals(20.0, simulator.armStateFlow.value.parts.single {
                it.modelNode == "Elbow"
            }.toggles.single().targetValue)
            assertFalse(simulator.reportTarget(ArmTargetCommand(Point3D(1.5, 0.75, 1.2))))
            simulator.reportCommand(ArmCommand(elbow.id, toggle.name, 70.0))
            assertEquals(70.0, simulator.armStateFlow.value.parts.single {
                it.modelNode == "Elbow"
            }.toggles.single().targetValue)
        }
    }

    @Test
    fun invalidGeometryAndDriveConfigurationsFailBeforeSimulation() {
        for (invalid in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { custom.geometry.upperArm.copy(length = invalid) }
            assertFailsWith<IllegalArgumentException> { custom.geometry.upperArm.copy(width = invalid) }
            assertFailsWith<IllegalArgumentException> { custom.geometry.upperArm.copy(height = invalid) }
        }
        assertFailsWith<IllegalArgumentException> { defaults.lift.copy(initialValue = 0.0) }
        assertFailsWith<IllegalArgumentException> {
            defaults.mast.copy(motion = defaults.mast.motion.copy(maxValue = 180.0))
        }
        // A physical stop is a bounded axis, not a shorter rotation period.
        defaults.mast.copy(motion = defaults.mast.motion.copy(maxValue = 180.0, circled = false))
    }

    private fun withSimulator(specification: ArmSpecification, block: (RoboArmSimulator) -> Unit) {
        val scope = CoroutineScope(Job())
        try {
            block(RoboArmSimulator(scope, specification))
        } finally {
            scope.cancel()
        }
    }
}
