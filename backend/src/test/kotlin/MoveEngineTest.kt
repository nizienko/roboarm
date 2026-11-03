import com.github.nizienko.roboarm.parts.MotionModel
import com.github.nizienko.roboarm.parts.MoveEngine
import com.github.nizienko.roboarm.parts.MoveEngineParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoveEngineTest {
    private val limits = MoveEngineParameters(maxSpeed = 60.0, maxAcceleration = 120.0)

    @Test
    fun acceleratesCruisesAndStopsExactly() {
        val model = MotionModel(0.0, limits)
        model.moveTo(90.0)
        model.advance(0.25)
        assertEquals(3.75, model.position, 1e-9)
        assertEquals(30.0, model.velocity, 1e-9)
        model.advance(0.75)
        assertEquals(45.0, model.position, 1e-9)
        assertEquals(60.0, model.velocity, 1e-9)
        model.advance(0.75)
        assertEquals(86.25, model.position, 1e-9)
        assertEquals(30.0, model.velocity, 1e-9)
        model.advance(0.25)
        assertEquals(90.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun reversesThroughZeroWithoutJumping() {
        val model = MotionModel(40.0, limits)
        model.moveTo(180.0)
        model.advance(0.5)
        assertEquals(55.0, model.position, 1e-9)
        model.moveTo(20.0)
        assertEquals(60.0, model.velocity, 1e-9)
        assertEquals(55.0, model.position, 1e-9)
        model.advance(0.25)
        assertEquals(66.25, model.position, 1e-9)
        assertEquals(30.0, model.velocity, 1e-9)
        model.advance(0.25)
        assertEquals(70.0, model.position, 1e-9)
        assertEquals(0.0, model.velocity, 1e-9)
        model.advance(0.1)
        assertTrue(model.velocity < 0.0)
        model.advance(10.0)
        assertEquals(20.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun overshootsNearbyTargetWhenBrakingDistanceRequiresIt() {
        val model = MotionModel(40.0, limits)
        model.moveTo(180.0)
        model.advance(0.5)
        model.moveTo(60.0)
        model.advance(0.5)
        assertEquals(70.0, model.position, 1e-9)
        model.advance(10.0)
        assertEquals(60.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun reachesFractionalAndVeryCloseTargetsInBothDirections() {
        val model = MotionModel(25.0, limits)
        for (target in listOf(25.04, 25.0400001, 24.99, 0.0, 360.0)) {
            model.moveTo(target)
            model.advance(20.0)
            assertEquals(target, model.position)
            assertEquals(0.0, model.velocity)
        }
    }

    @Test
    fun replanningKeepsLimitsAndEventuallyReachesLatestTarget() {
        val model = MotionModel(180.0, limits)
        val random = Random(123)
        repeat(10_000) {
            if (it % 7 == 0) model.moveTo(random.nextDouble(0.0, 360.0))
            val oldVelocity = model.velocity
            model.advance(0.01)
            assertTrue(abs(model.velocity) <= limits.maxSpeed + 1e-8)
            assertTrue(abs(model.velocity - oldVelocity) <= limits.maxAcceleration * 0.01 + 1e-8)
            assertTrue(model.position in -1e-8..360.00000001)
        }
        model.advance(20.0)
        assertEquals(model.target, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun repeatedReversalsNearHardBoundariesStayInRange() {
        val model = MotionModel(0.0, limits)
        repeat(500) { index ->
            model.moveTo(if (index % 2 == 0) 360.0 else 0.0)
            repeat(17) {
                model.advance(0.013)
                assertTrue(model.position in 0.0..360.0)
            }
        }
        model.advance(20.0)
        assertEquals(0.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun subdividingTimeDoesNotChangeTrajectory() {
        val whole = MotionModel(20.0, limits)
        val split = MotionModel(20.0, limits)
        whole.moveTo(200.0)
        split.moveTo(200.0)
        whole.advance(1.23)
        repeat(123) { split.advance(0.01) }
        assertEquals(whole.position, split.position, 1e-8)
        assertEquals(whole.velocity, split.velocity, 1e-8)
    }

    @Test
    fun circledTakesShortestPathAcrossBoundaryInBothDirections() {
        for ((start, target, direction) in listOf(Triple(330.0, 10.0, 1.0), Triple(10.0, 330.0, -1.0))) {
            val model = MotionModel(start, limits.copy(circled = true))
            model.moveTo(target)
            model.advance(0.5)
            assertEquals(direction * 60.0, model.velocity, 1e-9)
            model.advance(0.25)
            assertEquals(if (direction > 0.0) 359.5833333333333 else 340.4166666666667, model.position, 1e-8)
            model.advance(1.0)
            assertEquals(target, model.position)
            assertEquals(0.0, model.velocity)
        }
    }

    @Test
    fun circledWrapsPositionWithoutClampingOrLosingSpeed() {
        val model = MotionModel(330.0, limits.copy(circled = true))
        model.moveTo(60.0)
        model.advance(1.0)
        assertEquals(15.0, model.position, 1e-9)
        assertEquals(60.0, model.velocity, 1e-9)
        model.moveTo(80.0)
        model.advance(0.1)
        assertEquals(21.0, model.position, 1e-9)
        assertEquals(60.0, model.velocity, 1e-9)
        model.advance(10.0)
        assertEquals(80.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun circledBrakesAcrossBoundaryBeforeReturningToNewTarget() {
        val model = MotionModel(330.0, limits.copy(circled = true))
        model.moveTo(60.0)
        model.advance(0.5)
        model.moveTo(350.0)
        model.advance(0.5)
        assertEquals(0.0, model.position, 1e-9)
        assertEquals(0.0, model.velocity, 1e-9)
        model.advance(10.0)
        assertEquals(350.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun circledUsesConfiguredRangeAndTreatsEndpointsAsEquivalent() {
        val model = MotionModel(170.0, limits.copy(minValue = -180.0, maxValue = 180.0, circled = true))
        model.moveTo(-170.0)
        model.advance(0.5)
        assertTrue(model.position < -170.0)
        assertTrue(model.velocity > 0.0)
        model.advance(10.0)
        assertEquals(-170.0, model.position)
        model.moveTo(-180.0)
        model.advance(10.0)
        model.moveTo(180.0)
        model.advance(0.1)
        assertEquals(180.0, model.position)
        assertEquals(0.0, model.velocity)
    }

    @Test
    fun rejectsInvalidParametersAndTargets() {
        assertFailsWith<IllegalArgumentException> { MoveEngineParameters(maxSpeed = 0.0) }
        assertFailsWith<IllegalArgumentException> { MoveEngineParameters(maxAcceleration = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { MotionModel(-1.0, limits) }
        val scope = CoroutineScope(Job())
        try {
            val engine = MoveEngine(scope, MutableStateFlow(25.0), limits)
            for (target in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 361.0)) {
                assertFailsWith<IllegalArgumentException> { engine.moveTo(target) }
            }
            assertEquals(25.0, engine.target.value)
        } finally {
            scope.cancel()
        }
    }
}
