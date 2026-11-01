import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.RotationDegrees3D
import com.github.nizienko.roboarm.rotate
import com.github.nizienko.roboarm.multiply
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MathUtilsTest {

    @Test
    fun zeroRotationPreservesPoint() {
        val point = Point3D(2.0, 5.0, 0.0)
        assertPoint(point, point.rotate(RotationDegrees3D(0.0, 0.0, 0.0)))
    }

    @Test
    fun rotatesAroundEachAxis() {
        val point = Point3D(2.0, 5.0, 3.0)
        assertPoint(Point3D(2.0, -3.0, 5.0), point.rotate(RotationDegrees3D(90.0, 0.0, 0.0)))
        assertPoint(Point3D(3.0, 5.0, -2.0), point.rotate(RotationDegrees3D(0.0, 90.0, 0.0)))
        assertPoint(Point3D(-5.0, 2.0, 3.0), point.rotate(RotationDegrees3D(0.0, 0.0, 90.0)))
    }

    @Test
    fun appliesXThenYThenZ() {
        // (2, 5, 3) -> (2, -3, 5) -> (5, -3, -2) -> (3, 5, -2)
        assertPoint(
            Point3D(3.0, 5.0, -2.0),
            Point3D(2.0, 5.0, 3.0).rotate(RotationDegrees3D(90.0, 90.0, 90.0)),
        )
    }

    @Test
    fun negativeAnglesRotateInOppositeDirection() {
        val point = Point3D(2.0, 5.0, 3.0)
        assertPoint(Point3D(2.0, 3.0, -5.0), point.rotate(RotationDegrees3D(-90.0, 0.0, 0.0)))
        assertPoint(Point3D(-3.0, 5.0, 2.0), point.rotate(RotationDegrees3D(0.0, -90.0, 0.0)))
        assertPoint(Point3D(5.0, -2.0, 3.0), point.rotate(RotationDegrees3D(0.0, 0.0, -90.0)))
    }

    @Test
    fun rotatesByFortyFiveDegrees() {
        assertPoint(
            Point3D(sqrt(2.0), sqrt(2.0), 0.0),
            Point3D(2.0, 0.0, 0.0).rotate(RotationDegrees3D(0.0, 0.0, 45.0)),
        )
    }

    @Test
    fun fullTurnsPreservePoint() {
        val point = Point3D(-2.0, 5.0, -3.0)
        assertPoint(point, point.rotate(RotationDegrees3D(360.0, -720.0, 1080.0)))
    }

    @Test
    fun originStaysFixed() {
        val origin = Point3D(0.0, 0.0, 0.0)
        assertPoint(origin, origin.rotate(RotationDegrees3D(23.5, -71.2, 142.8)))
    }

    @Test
    fun pointsOnRotationAxisStayFixed() {
        assertPoint(Point3D(4.0, 0.0, 0.0), Point3D(4.0, 0.0, 0.0).rotate(RotationDegrees3D(37.5, 0.0, 0.0)))
        assertPoint(Point3D(0.0, -4.0, 0.0), Point3D(0.0, -4.0, 0.0).rotate(RotationDegrees3D(0.0, 37.5, 0.0)))
        assertPoint(Point3D(0.0, 0.0, 4.0), Point3D(0.0, 0.0, 4.0).rotate(RotationDegrees3D(0.0, 0.0, 37.5)))
    }

    @Test
    fun rotationPreservesDistanceFromOrigin() {
        val point = Point3D(-2.5, 5.25, 3.75)
        val rotated = point.rotate(RotationDegrees3D(23.5, -71.2, 142.8))
        fun squaredLength(p: Point3D) = p.x * p.x + p.y * p.y + p.z * p.z
        assertEquals(squaredLength(point), squaredLength(rotated), 1e-9)
    }

    @Test
    fun inverseRotationsInReverseOrderRestorePoint() {
        val point = Point3D(-2.5, 5.25, 3.75)
        // Undo Z, then Y, then X; negating all three angles at once is not the inverse.
        val restored = point.rotate(RotationDegrees3D(23.5, -71.2, 142.8))
            .rotate(RotationDegrees3D(0.0, 0.0, -142.8))
            .rotate(RotationDegrees3D(0.0, 71.2, 0.0))
            .rotate(RotationDegrees3D(-23.5, 0.0, 0.0))
        assertPoint(point, restored)
    }

    @Test
    fun multipliesRectangularMatrices() {
        val result = multiply(
            arrayOf(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(4.0, 5.0, 6.0)),
            arrayOf(doubleArrayOf(7.0, 8.0), doubleArrayOf(9.0, 10.0), doubleArrayOf(11.0, 12.0)),
        )
        assertEquals(2, result.size)
        assertEquals(listOf(58.0, 64.0), result[0].toList())
        assertEquals(listOf(139.0, 154.0), result[1].toList())
    }

    @Test
    fun rejectsInvalidMatrices() {
        val valid = arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val invalid = listOf(
            emptyArray<DoubleArray>(),
            arrayOf(doubleArrayOf()),
            arrayOf(doubleArrayOf(1.0), doubleArrayOf(2.0, 3.0)),
            arrayOf(doubleArrayOf(1.0)),
        )
        for (matrix in invalid) {
            assertFailsWith<IllegalArgumentException> { multiply(matrix, valid) }
            assertFailsWith<IllegalArgumentException> { multiply(valid, matrix) }
        }
    }

    private fun assertPoint(expected: Point3D, actual: Point3D) {
        assertEquals(expected.x, actual.x, 1e-9)
        assertEquals(expected.y, actual.y, 1e-9)
        assertEquals(expected.z, actual.z, 1e-9)
    }
}
