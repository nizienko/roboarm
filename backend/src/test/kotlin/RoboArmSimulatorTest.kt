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
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import java.util.UUID

class RoboArmSimulatorTest {
    @Test
    fun commandsUpdateGeometryAndControlValues() = withSimulator { simulator ->
        val parts = simulator.armStateFlow.value.parts
        simulator.reportCommand(ArmCommand(parts[0].id, "Mast Rotation", 90.0))
        simulator.reportCommand(ArmCommand(parts[1].id, "Height", 20.0))
        simulator.reportCommand(ArmCommand(parts[2].id, "Cran Rotation", -15.0))

        val state = simulator.armStateFlow.value
        assertEquals(90.0, state.parts[0].toggles.single().value)
        assertEquals(20.0, state.parts[1].toggles.single().value)
        assertEquals(-15.0, state.parts[2].toggles.single().value)
        assertEquals(parts.map { it.id }, state.parts.map { it.id })
        assertPoint(Point3D(0.0, 19.0, 19.0), state.parts[2].mesh.endPoint)
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
            ArmCommand(parts[2].id, "Cran Rotation", -31.0),
            ArmCommand(parts[2].id, "Cran Rotation", 31.0),
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
            val toggle = part.toggles.single()
            for (value in listOf(toggle.maxValue, toggle.minValue)) {
                simulator.reportCommand(ArmCommand(part.id, toggle.name, value))
                val updated = simulator.armStateFlow.value.parts.single { it.id == part.id }
                assertEquals(value, updated.toggles.single().value)
            }
        }
    }

    @Test
    fun partIdsAreUniqueEvenForPartsOfTheSameType() {
        val tip = Cran()
        val root = Cran(tip)
        val parts = root.toArmParts()
        assertEquals(2, parts.map { it.id }.toSet().size)
        assertEquals(listOf(root.id, tip.id), parts.map { it.id })
        assertEquals(parts, root.toArmParts())
    }

    @Test
    fun initialStateContainsAllThreeParts() = withSimulator { simulator ->
        val state = simulator.armStateFlow.value
        assertEquals(3, state.parts.size)
        assertPoint(Point3D(0.0, 0.0, 0.0), state.parts[0].mesh.startPoint)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(11.0, 0.0, 5.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(19.0, 0.0, 4.0), state.parts[2].mesh.endPoint)
    }

    @Test
    fun mastRotationRotatesCranAroundVerticalAxis() {
        val state = stateAt(height = 5.0, angleDegrees = 90.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(0.0, 9.0, 4.0), state.parts[2].mesh.startPoint)
        assertPoint(Point3D(0.0, 19.0, 4.0), state.parts[2].mesh.endPoint)
        assertEquals(90.0, state.parts[0].toggles.single().value)
    }

    @Test
    fun liftHeightMovesEntireCranWithoutChangingMast() {
        val state = stateAt(height = 20.0, angleDegrees = 180.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(-11.0, 0.0, 20.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(-19.0, 0.0, 19.0), state.parts[2].mesh.endPoint)
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

    private fun stateAt(height: Double, angleDegrees: Double): ArmState {
        val lift = Lift(Cran()).apply { setHeight(height) }
        val mast = Mast(lift).apply { moveToAngle(angleDegrees) }
        return ArmState(parts = mast.toArmParts())
    }

    private fun assertPoint(expected: Point3D, actual: Point3D) {
        assertEquals(expected.x, actual.x, 1e-9)
        assertEquals(expected.y, actual.y, 1e-9)
        assertEquals(expected.z, actual.z, 1e-9)
    }

    private fun withSimulator(block: (RoboArmSimulator) -> Unit) {
        val scope = CoroutineScope(Job())
        try {
            block(RoboArmSimulator(scope))
        } finally {
            scope.cancel()
        }
    }
}
