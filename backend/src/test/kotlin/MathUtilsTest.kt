import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.rotateZ
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MathUtilsTest {
    @Test
    fun rotatesInXYPlaneAndPreservesHeight() {
        val point = Point3D(2.0, 5.0, 3.0)
        assertPoint(Point3D(-5.0, 2.0, 3.0), point.rotateZ(90.0))
        assertPoint(Point3D(5.0, -2.0, 3.0), point.rotateZ(-90.0))
        assertPoint(Point3D(-2.0, -5.0, 3.0), point.rotateZ(180.0))
        assertPoint(point, point.rotateZ(0.0))
        assertPoint(point, point.rotateZ(720.0))
    }

    @Test
    fun preservesDistanceAndCanBeReversed() {
        val point = Point3D(-2.5, 5.25, 3.75)
        val rotated = point.rotateZ(37.5)
        fun squaredLength(p: Point3D) = p.x * p.x + p.y * p.y + p.z * p.z
        assertEquals(squaredLength(point), squaredLength(rotated), 1e-9)
        assertPoint(point, rotated.rotateZ(-37.5))
        assertPoint(Point3D(0.0, 0.0, 4.0), Point3D(0.0, 0.0, 4.0).rotateZ(37.5))
    }

    private fun assertPoint(expected: Point3D, actual: Point3D) {
        assertEquals(expected.x, actual.x, 1e-9)
        assertEquals(expected.y, actual.y, 1e-9)
        assertEquals(expected.z, actual.z, 1e-9)
    }
}
