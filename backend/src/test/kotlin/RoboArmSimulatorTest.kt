import com.github.nizienko.roboarm.RoboArmSimulator
import com.github.nizienko.roboarm.toArmParts
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmTargetCommand
import com.github.nizienko.roboarm.data.ArmRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoboArmSimulatorTest {
    @Test
    fun protocolAcceptsBothJointAndTargetCommands() {
        val joint = ArmCommand("mast", "Mast Rotation", 90.0)
        val target = ArmTargetCommand(Point3D(1.0, 1.0, 1.5))
        assertEquals(joint, Json.decodeFromString<ArmRequest>("""{"partId":"mast","toggleName":"Mast Rotation","value":90.0}"""))
        assertEquals(target, Json.decodeFromString<ArmRequest>("""{"target":{"x":1.0,"y":1.0,"z":1.5}}"""))
        assertEquals(target, Json.decodeFromString<ArmRequest>(Json.encodeToString<ArmRequest>(target)))
    }

    @Test
    fun targetMovesGripperToXYAndSetsLiftHeightDirectly() = withSimulator { simulator ->
        assertTrue(simulator.reportTarget(ArmTargetCommand(Point3D(1.0, 1.0, 1.2))))
        val commanded = simulator.armStateFlow.value.parts
        assertEquals(1.2, commanded.single { it.modelNode == "Lift" }.toggles.single().targetValue)
        assertEquals(0.0, commanded.single { it.modelNode == "Gripper" }.toggles.single().targetValue)
        assertEquals(0.0, commanded.single { it.modelNode == "Hand" }.toggles.single().targetValue)
        val state = withTimeout(15_000) {
            simulator.armStateFlow.first {
                it.parts.flatMap { part -> part.toggles }.all { toggle -> toggle.value == toggle.targetValue }
            }
        }
        // Telemetry maps backend (x, y, z) to scene (x, z, -y).
        val gripper = state.parts.single { it.modelNode == "Gripper" }
        assertEquals(1.0, gripper.position.x, 1e-8)
        assertEquals(-1.0, gripper.position.z, 1e-8)
        assertEquals(1.2, state.parts.single { it.modelNode == "Lift" }.toggles.single().value)
    }

    @Test
    fun invalidTargetDoesNotPartiallyApplyAnyJoint() = withSimulator { simulator ->
        val before = simulator.armStateFlow.value.parts
        for (point in listOf(Point3D(3.0, 0.0, 1.0), Point3D(0.0, 0.0, 1.0),
            Point3D(1.0, 1.0, 0.64), Point3D(1.0, 1.0, 2.71),
            Point3D(1.0, 1.0, Double.NaN), Point3D(Double.NaN, 0.0, 1.0))) {
            assertFalse(simulator.reportTarget(ArmTargetCommand(point)))
            assertEquals(before, simulator.armStateFlow.value.parts)
        }
        for (height in listOf(0.65, 2.7)) {
            assertTrue(simulator.reportTarget(ArmTargetCommand(Point3D(2.0, 0.0, height))))
            assertEquals(height, simulator.armStateFlow.value.parts.single { it.modelNode == "Lift" }.toggles.single().targetValue)
        }
    }

    @Test
    fun commandsUpdateActualAndTargetJointValues() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        simulator.reportCommand(ArmCommand(parts.single { it.modelNode == "Mast" }.id, "Mast Rotation", 90.0))
        simulator.reportCommand(ArmCommand(parts.single { it.modelNode == "Lift" }.id, "Height", 1.0))
        simulator.reportCommand(ArmCommand(parts.single { it.modelNode == "Cran" }.id, "Cran Rotation", -15.0))

        val state = withTimeout(20_000) {
            simulator.armStateFlow.first {
                it.parts.flatMap { part -> part.toggles }.all { toggle -> toggle.value == toggle.targetValue }
            }
        }
        assertEquals(90.0, state.parts.single { it.modelNode == "Mast" }.toggles.single().value)
        assertEquals(1.0, state.parts.single { it.modelNode == "Lift" }.toggles.single().value)
        assertEquals(-15.0, state.parts.single { it.modelNode == "Cran" }.toggles.single().value)
        assertEquals(parts.map { it.id }, state.parts.map { it.id })
    }

    @Test
    fun gripperRotationAndHandExpansionMoveIndependently() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        val gripper = parts.single { it.modelNode == "Gripper" }
        val hand = parts.single { it.modelNode == "Hand" }
        assertEquals("Gripper Rotation", gripper.toggles.single().name)
        assertEquals("Hand Expand", hand.toggles.single().name)
        simulator.reportCommand(ArmCommand(gripper.id, "Gripper Rotation", 3.0))
        simulator.reportCommand(ArmCommand(hand.id, "Hand Expand", 0.1))

        val commanded = simulator.armStateFlow.value.parts
        val rotation = commanded.single { it.modelNode == "Gripper" }.toggles.single()
        val opening = commanded.single { it.modelNode == "Hand" }.toggles.single()
        assertEquals(3.0, rotation.targetValue)
        assertEquals(0.1, opening.targetValue)
        assertNotEquals(3.0, rotation.value)
        assertNotEquals(0.1, opening.value)

        val state = withTimeout(5_000) {
            simulator.armStateFlow.first {
                it.parts.filter { part -> part.modelNode in listOf("Gripper", "Hand") }
                    .flatMap { part -> part.toggles }.all { toggle -> toggle.value == toggle.targetValue }
            }
        }
        assertEquals(3.0, state.parts.single { it.modelNode == "Gripper" }.toggles.single().value)
        assertEquals(0.1, state.parts.single { it.modelNode == "Hand" }.toggles.single().value)
        assertEquals(0.0, state.parts.single { it.modelNode == "Mast" }.toggles.single().value)
        assertEquals(0.0, state.parts.single { it.modelNode == "Cran" }.toggles.single().value)
    }

    @Test
    fun invalidCommandsDoNotChangeState() = withSimulator { simulator ->
        val initial = simulator.armStateFlow.value
        val parts = initial.parts
        listOf(
            ArmCommand("unknown-part", "Mast Rotation", 45.0),
            ArmCommand("", "Height", 10.0),
            ArmCommand(parts.single { it.modelNode == "Mast" }.id, "Height", 10.0),
            ArmCommand(parts.single { it.modelNode == "Cran" }.id, "Mast Rotation", 10.0),
            ArmCommand(parts.single { it.modelNode == "Mast" }.id, "Mast Rotation", -1.0),
            ArmCommand(parts.single { it.modelNode == "Mast" }.id, "Mast Rotation", 361.0),
            ArmCommand(parts.single { it.modelNode == "Cran" }.id, "Cran Rotation", -121.0),
            ArmCommand(parts.single { it.modelNode == "Cran" }.id, "Cran Rotation", 121.0),
            ArmCommand(parts.single { it.modelNode == "Lift" }.id, "Height", 31.0),
            ArmCommand(parts.single { it.modelNode == "Lift" }.id, "Height", Double.NaN),
            ArmCommand(parts.single { it.modelNode == "Lift" }.id, "Height", Double.POSITIVE_INFINITY),
        ).forEach(simulator::reportCommand)
        assertEquals(initial.parts, simulator.armStateFlow.value.parts)
    }

    @Test
    fun commandsAcceptRangeBoundaries() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        for (part in parts) {
            for (toggle in part.toggles) {
                for (value in listOf(toggle.maxValue, toggle.minValue)) {
                    simulator.reportCommand(ArmCommand(part.id, toggle.name, value))
                    val updated = simulator.armStateFlow.value.parts.single { it.id == part.id }
                    assertEquals(value, updated.toggles.single { it.name == toggle.name }.targetValue)
                }
            }
        }
    }

    @Test
    fun partIdsAreUniqueEvenForPartsOfTheSameType() {
        val scope = CoroutineScope(Job())
        try {
            val tip = Cran(scope)
            val root = Cran(scope, tip)
            val parts = root.toArmParts()
            assertEquals(2, parts.map { it.id }.toSet().size)
            assertEquals(listOf(root.id, tip.id), parts.map { it.id })
            assertEquals(parts, root.toArmParts())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun telemetryContainsAbsolutePosesForEveryRenderedPart() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        assertEquals(listOf("basement", "Mast", "Lift", "Cran", "Gripper", "Hand"), parts.map { it.modelNode })
        assertEquals(Point3D(0.0, 0.0, -0.0), parts[0].position)
        assertEquals(0.2, parts[1].position.y, 1e-9)
        assertEquals(1.7, parts[2].position.y, 1e-9)
        assertEquals(1.0, parts[4].position.x - parts[3].position.x, 1e-9)
        assertEquals(-0.05, parts[4].position.y - parts[3].position.y, 1e-9)
        assertEquals(-0.04, parts[5].position.x - parts[4].position.x, 1e-9)
        assertEquals("meters", parts[2].toggles.single().unit)
        assertEquals("degrees", parts[3].toggles.single().unit)
    }

    @Test
    fun wireContractContainsWorldPosesWithoutClientMotionRules() = withSimulator { simulator ->
        val json = Json { encodeDefaults = true }
        val state = simulator.armStateFlow.value
        val encoded = json.encodeToString(state)
        for (oldField in listOf("startPoint", "endPoint", "mesh", "motion", "referenceValue", "parentModelNode")) {
            assertFalse(encoded.contains("\"$oldField\""))
        }
        assertTrue(encoded.contains("rotationDegreesZ"))
        assertEquals(state, json.decodeFromString<ArmState>(encoded))
    }

    @Test
    fun movingParentChangesDescendantWorldPosesWithoutChangingTheirReadings() = withSimulator { simulator ->
        val initial = simulator.armStateFlow.value.parts
        val mast = initial.single { it.modelNode == "Mast" }
        simulator.reportCommand(ArmCommand(mast.id, "Mast Rotation", 3.0))
        val state = withTimeout(5_000) {
            simulator.armStateFlow.first { it.parts.single { part -> part.modelNode == "Mast" }.toggles.single().value == 3.0 }
        }
        assertEquals(initial[0], state.parts[0])
        for (node in listOf("Cran", "Gripper", "Hand")) {
            val before = initial.single { it.modelNode == node }
            val after = state.parts.single { it.modelNode == node }
            assertNotEquals(before.position, after.position)
            assertNotEquals(before.rotationDegreesZ, after.rotationDegreesZ)
            assertEquals(before.toggles, after.toggles)
        }
    }

    private fun withSimulator(block: suspend (RoboArmSimulator) -> Unit) = runBlocking {
        val scope = CoroutineScope(Job())
        try {
            block(RoboArmSimulator(scope))
        } finally {
            scope.cancel()
        }
    }
}
