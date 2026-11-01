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

class RoboArmSimulatorTest {
    @Test
    fun commandsUpdateGeometryAndControlValues() = withSimulator { simulator ->
        simulator.reportCommand(ArmCommand(0, "Rotation", 90.0))
        simulator.reportCommand(ArmCommand(1, "Height", 20.0))

        val state = simulator.armStateFlow.value
        assertEquals(90.0, state.parts[0].toggles.single().value)
        assertEquals(20.0, state.parts[1].toggles.single().value)
        assertPoint(Point3D(0.0, 10.0, 20.0), state.parts[2].mesh.endPoint)
    }

    @Test
    fun invalidCommandsDoNotChangeState() = withSimulator { simulator ->
        val initial = simulator.armStateFlow.value
        listOf(
            ArmCommand(-1, "Rotation", 45.0),
            ArmCommand(99, "Height", 10.0),
            ArmCommand(0, "Height", 10.0),
            ArmCommand(2, "Rotation", 45.0),
            ArmCommand(0, "Rotation", -1.0),
            ArmCommand(0, "Rotation", 181.0),
            ArmCommand(1, "Height", 31.0),
            ArmCommand(1, "Height", Double.NaN),
            ArmCommand(1, "Height", Double.POSITIVE_INFINITY),
        ).forEach(simulator::reportCommand)
        assertEquals(initial.parts, simulator.armStateFlow.value.parts)
    }

    @Test
    fun commandsAcceptRangeBoundaries() = withSimulator { simulator ->
        simulator.reportCommand(ArmCommand(0, "Rotation", 180.0))
        simulator.reportCommand(ArmCommand(1, "Height", 30.0))
        assertPoint(Point3D(-10.0, 0.0, 30.0), simulator.armStateFlow.value.parts[2].mesh.endPoint)
        simulator.reportCommand(ArmCommand(0, "Rotation", 0.0))
        simulator.reportCommand(ArmCommand(1, "Height", 0.0))
        assertPoint(Point3D(10.0, 0.0, 0.0), simulator.armStateFlow.value.parts[2].mesh.endPoint)
    }

    @Test
    fun initialStateContainsAllThreeParts() = withSimulator { simulator ->
        val state = simulator.armStateFlow.value
        assertEquals(3, state.parts.size)
        assertPoint(Point3D(0.0, 0.0, 0.0), state.parts[0].mesh.startPoint)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(0.0, 0.0, 5.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(10.0, 0.0, 5.0), state.parts[2].mesh.endPoint)
        assertConnected(state)
    }

    @Test
    fun mastRotationRotatesCranAroundVerticalAxis() {
        val state = stateAt(height = 5.0, angleDegrees = 90.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(0.0, 0.0, 5.0), state.parts[2].mesh.startPoint)
        assertPoint(Point3D(0.0, 10.0, 5.0), state.parts[2].mesh.endPoint)
        assertEquals(90.0, state.parts[0].toggles.single().value)
        assertConnected(state)
    }

    @Test
    fun liftHeightMovesEntireCranWithoutChangingMast() {
        val state = stateAt(height = 20.0, angleDegrees = 180.0)
        assertPoint(Point3D(0.0, 0.0, 30.0), state.parts[0].mesh.endPoint)
        assertPoint(Point3D(0.0, 0.0, 20.0), state.parts[1].mesh.endPoint)
        assertPoint(Point3D(-10.0, 0.0, 20.0), state.parts[2].mesh.endPoint)
        assertEquals(20.0, state.parts[1].toggles.single().value)
        assertConnected(state)
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
        override val localMesh = Mesh(Point3D(0.0, 0.0, 0.0), Point3D(0.0, 0.0, 1.0))
    }

    private fun stateAt(height: Double, angleDegrees: Double): ArmState {
        val lift = Lift(Cran()).apply { setHeight(height) }
        val mast = Mast(lift).apply { moveToAngle(angleDegrees) }
        return ArmState(parts = mast.toArmParts())
    }

    private fun assertConnected(state: ArmState) {
        assertPoint(state.parts[0].mesh.startPoint, state.parts[1].mesh.startPoint)
        assertPoint(state.parts[1].mesh.endPoint, state.parts[2].mesh.startPoint)
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
