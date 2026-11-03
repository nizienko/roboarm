import com.github.nizienko.roboarm.solveInverseKinematics
import com.github.nizienko.roboarm.parts.MoveEngineParameters
import com.github.nizienko.roboarm.data.Point3D
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class InverseKinematicsTest {
    private val mastMotion = MoveEngineParameters(circled = true)
    private val elbowMotion = MoveEngineParameters(minValue = -120.0, maxValue = 120.0)

    private fun solve(x: Double, y: Double, mast: Double = 0.0, elbow: Double = 0.0) =
        solveInverseKinematics(Point3D(x, y, 1.5), 1.0, 1.0, mastMotion, elbowMotion, mast, elbow)

    @Test
    fun solutionsReproduceTargetsAcrossAllQuadrantsAndJointBoundaries() {
        for (mast in listOf(0.0, 45.0, 135.0, 225.0, 315.0, 360.0)) {
            for (elbow in listOf(-120.0, -90.0, 0.0, 90.0, 120.0)) {
                val a = Math.toRadians(mast)
                val b = Math.toRadians(mast + elbow)
                val x = cos(a) + cos(b)
                val y = sin(a) + sin(b)
                val solution = assertNotNull(solve(x, y, mast, elbow))
                val solvedA = Math.toRadians(solution.mast)
                val solvedB = Math.toRadians(solution.mast + solution.elbow)
                assertEquals(x, cos(solvedA) + cos(solvedB), 1e-8)
                assertEquals(y, sin(solvedA) + sin(solvedB), 1e-8)
            }
        }
    }

    @Test
    fun selectsClosestElbowConfiguration() {
        val positive = assertNotNull(solve(1.0, 1.0, 0.0, 80.0))
        assertEquals(0.0, positive.mast, 1e-9)
        assertEquals(90.0, positive.elbow, 1e-9)
        val negative = assertNotNull(solve(1.0, 1.0, 85.0, -80.0))
        assertEquals(90.0, negative.mast, 1e-9)
        assertEquals(-90.0, negative.elbow, 1e-9)
    }

    @Test
    fun selectsShortestMovementAcrossMastSeamInBothDirections() {
        for (direction in listOf(-1.0, 1.0)) {
            val a = Math.toRadians(10.0 * direction)
            val b = Math.toRadians(100.0 * direction)
            val current = if (direction > 0.0) 350.0 else 10.0
            val solution = assertNotNull(solve(cos(a) + cos(b), sin(a) + sin(b), current, 0.0))
            assertEquals(if (direction > 0.0) 10.0 else 350.0, solution.mast, 1e-9)
            assertEquals(90.0 * direction, solution.elbow, 1e-9)
        }
    }

    @Test
    fun limitedMastDoesNotTreatEndpointsAsConnected() {
        val a = Math.toRadians(10.0)
        val b = Math.toRadians(100.0)
        val solution = assertNotNull(solveInverseKinematics(
            Point3D(cos(a) + cos(b), sin(a) + sin(b), 1.5), 1.0, 1.0,
            mastMotion.copy(circled = false), elbowMotion, 350.0, 0.0,
        ))
        assertEquals(100.0, solution.mast, 1e-9)
        assertEquals(-90.0, solution.elbow, 1e-9)
    }

    @Test
    fun rejectsUnreachableAndNonFiniteTargets() {
        for ((x, y) in listOf(0.0 to 0.0, 0.5 to 0.0, 2.01 to 0.0,
            Double.NaN to 1.0, 1.0 to Double.POSITIVE_INFINITY, Double.MAX_VALUE to 0.0)) {
            assertNull(solve(x, y))
        }
    }
}
