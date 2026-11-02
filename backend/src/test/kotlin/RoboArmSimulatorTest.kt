import com.github.nizienko.roboarm.RoboArmSimulator
import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.toArmParts
import com.github.nizienko.roboarm.data.Mesh
import com.github.nizienko.roboarm.parts.Mast
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.Point3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlin.math.sin
import kotlin.math.cos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import java.util.UUID

class RoboArmSimulatorTest {
    @Test
    fun commandsUpdateGeometryAndControlValues() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        simulator.reportCommand(ArmCommand(parts[0].id, "Mast Rotation", 90.0))
        simulator.reportCommand(ArmCommand(parts[1].id, "Height", 20.0))
        simulator.reportCommand(ArmCommand(parts[2].id, "Cran Rotation", -15.0))

        val state = withTimeout(20_000) {
            simulator.armStateFlow.first {
                it.parts.take(3).flatMap { part -> part.toggles }.all { toggle -> toggle.value == toggle.targetValue }
            }
        }
        assertEquals(90.0, state.parts[0].toggles.single().value)
        assertEquals(20.0, state.parts[1].toggles.single().value)
        assertEquals(-15.0, state.parts[2].toggles.single().value)
        assertEquals(parts.map { it.id }, state.parts.map { it.id })
        assertPoint(Point3D(10.5 * sin(Math.toRadians(15.0)), 11.0 + 10.5 * cos(Math.toRadians(15.0)), 19.0), state.parts[2].mesh.endPoint)
    }

    @Test
    fun gripperRotationAndExpansionMoveIndependently() = withSimulator { simulator ->
        val gripper = simulator.armStateFlow.value.parts.last()
        simulator.reportCommand(ArmCommand(gripper.id, "Gripper Rotation", 3.0))
        simulator.reportCommand(ArmCommand(gripper.id, "Gripper Expand", 4.5))

        val commanded = simulator.armStateFlow.value.parts.last().toggles
        assertEquals(3.0, commanded[0].targetValue)
        assertEquals(4.5, commanded[1].targetValue)
        assertNotEquals(3.0, commanded[0].value)
        assertNotEquals(4.5, commanded[1].value)

        val state = withTimeout(5_000) {
            simulator.armStateFlow.first {
                it.parts.last().toggles.all { toggle -> toggle.value == toggle.targetValue }
            }
        }
        assertEquals(listOf(3.0, 4.5), state.parts.last().toggles.map { it.value })
        assertEquals(0.0, state.parts[0].toggles.single().value)
        assertEquals(0.0, state.parts[2].toggles.single().value)
    }

    @Test
    fun invalidCommandsDoNotChangeState() = withSimulator { simulator ->
        val initial = simulator.armStateFlow.value
        val parts = initial.parts
        listOf(
            ArmCommand("unknown-part", "Mast Rotation", 45.0),
            ArmCommand("", "Height", 10.0),
            ArmCommand(parts[0].id, "Height", 10.0),
            ArmCommand(parts[2].id, "Mast Rotation", 10.0),
            ArmCommand(parts[0].id, "Mast Rotation", -1.0),
            ArmCommand(parts[0].id, "Mast Rotation", 361.0),
            ArmCommand(parts[2].id, "Cran Rotation", -121.0),
            ArmCommand(parts[2].id, "Cran Rotation", 121.0),
            ArmCommand(parts[1].id, "Height", 31.0),
            ArmCommand(parts[1].id, "Height", Double.NaN),
            ArmCommand(parts[1].id, "Height", Double.POSITIVE_INFINITY),
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
    fun initialStateContainsAllFourParts() = withSimulator { simulator ->
        val state = simulator.armStateFlow.value
        assertEquals(4, state.parts.size)
        assertPoint(Point3D(0.0, 0.0, 0.0), state.parts[0].mesh.startPoint)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(11.5, 0.0, 25.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(21.5, 0.0, 24.0), state.parts[2].mesh.endPoint)
    }

    @Test
    fun mastRotationRotatesCranAroundVerticalAxis() {
        val state = stateAt(height = 5.0, angleDegrees = 90.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(0.0, 10.5, 4.0), state.parts[2].mesh.startPoint)
        assertPoint(Point3D(0.0, 21.5, 4.0), state.parts[2].mesh.endPoint)
        assertEquals(90.0, state.parts[0].toggles.single().value)
    }

    @Test
    fun liftHeightMovesEntireCranWithoutChangingMast() {
        val state = stateAt(height = 20.0, angleDegrees = 180.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(-11.5, 0.0, 20.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(-21.5, 0.0, 19.0), state.parts[2].mesh.endPoint)
        assertEquals(20.0, state.parts[1].toggles.single().value)
    }

    @Test
    fun accumulatesTranslationsAndNonCommutingRotationsThroughAllParents() {
        val tip = TestPart(
            localPosition = Point3D(0.0, 2.0, 0.0),
            localRotation = RotationDegrees3D(0.0, 90.0, 0.0),
        )
        val child = TestPart(
            localPosition = Point3D(2.0, 0.0, 0.0),
            localRotation = RotationDegrees3D(90.0, 0.0, 0.0),
            nextPart = tip,
        )
        val root = TestPart(
            localPosition = Point3D(1.0, 2.0, 3.0),
            localRotation = RotationDegrees3D(0.0, 0.0, 90.0),
            nextPart = child,
        )
        val parts = root.toArmParts()
        assertEquals(3, parts.size)
        assertPoint(Point3D(1.0, 2.0, 3.0), parts[0].mesh.startPoint)
        assertPoint(Point3D(1.0, 4.0, 3.0), parts[1].mesh.startPoint)
        assertPoint(Point3D(1.0, 4.0, 5.0), parts[2].mesh.startPoint)
        assertPoint(Point3D(1.0, 5.0, 5.0), parts[2].mesh.endPoint)
    }

    private data class TestPart(
        override val localPosition: Point3D,
        override val localRotation: RotationDegrees3D,
        override val nextPart: RoboPart? = null,
    ) : RoboPart {
        override val id: String = UUID.randomUUID().toString()
        override fun applyCommand(command: ArmCommand) = Unit
        override val localMesh = Mesh(Point3D(0.0, 0.0, 0.0), Point3D(0.0, 0.0, 1.0))
    }

    private fun stateAt(height: Double, angleDegrees: Double): ArmState = runBlocking {
        val scope = CoroutineScope(Job())
        try {
            val lift = Lift(scope, Cran(scope)).apply { setHeight(height) }
            val mast = Mast(scope, lift).apply { moveToAngle(angleDegrees) }
            withTimeout(20_000) {
                lift.height.first { it == height }
                mast.angleDegrees.first { it == angleDegrees }
            }
            ArmState(parts = mast.toArmParts())
        } finally {
            scope.cancel()
        }
    }

    private fun assertPoint(expected: Point3D, actual: Point3D) {
        assertEquals(expected.x, actual.x, 1e-9)
        assertEquals(expected.y, actual.y, 1e-9)
        assertEquals(expected.z, actual.z, 1e-9)
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
