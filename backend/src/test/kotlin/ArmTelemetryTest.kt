import com.github.nizienko.roboarm.RoboPart
import com.github.nizienko.roboarm.toArmParts
import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.Point3D
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ArmTelemetryTest {
    private data class Part(
        override val id: String,
        override val localPosition: Point3D = Point3D(0.0, 0.0, 0.0),
        override val localRotationDegreesZ: Double = 0.0,
        override val nextPart: RoboPart? = null,
    ) : RoboPart {
        override val modelNode = id
        override fun applyCommand(command: ArmCommand) = Unit
    }

    @Test
    fun composesLocalOffsetsAndZAnglesOnTheServer() {
        val tip = Part("tip", Point3D(0.0, 2.0, 0.0), -90.0)
        val child = Part("child", Point3D(2.0, 0.0, 0.0), 90.0, tip)
        val root = Part("root", Point3D(1.0, 2.0, 3.0), 90.0, child)
        val parts = root.toArmParts()
        assertPoint(Point3D(1.0, 3.0, -2.0), parts[0].position)
        assertPoint(Point3D(1.0, 3.0, -4.0), parts[1].position)
        assertPoint(Point3D(1.0, 3.0, -2.0), parts[2].position)
        assertEquals(listOf(90.0, 180.0, 90.0), parts.map { it.rotationDegreesZ })
    }

    @Test
    fun backendLocationIsAuthoritativeForTheWholeSubchain() {
        val tip = Part("tip", Point3D(1.0, 0.0, 0.0))
        val first = Part("root", nextPart = tip).toArmParts()
        val moved = Part("root", localPosition = Point3D(2.0, 3.0, 4.0), nextPart = tip).toArmParts()
        assertPoint(Point3D(2.0, 4.0, -3.0), moved[0].position)
        assertPoint(Point3D(3.0, 4.0, -3.0), moved[1].position)
        assertEquals(first[1].rotationDegreesZ, moved[1].rotationDegreesZ)
    }

    @Test
    fun preservesSignedAnglesAndFullTurns() {
        for (angle in listOf(0.0, -90.0, 180.0, 360.0, 720.0)) {
            val child = Part("child", Point3D(1.0, 0.0, 0.0), -angle)
            val parts = Part("root", localRotationDegreesZ = angle, nextPart = child).toArmParts()
            assertEquals(angle, parts[0].rotationDegreesZ)
            assertEquals(0.0, parts[1].rotationDegreesZ)
            val radians = Math.toRadians(angle)
            assertPoint(Point3D(kotlin.math.cos(radians), 0.0, -kotlin.math.sin(radians)), parts[1].position)
        }
    }

    private fun assertPoint(expected: Point3D, actual: Point3D) {
        assertEquals(expected.x, actual.x, 1e-9)
        assertEquals(expected.y, actual.y, 1e-9)
        assertEquals(expected.z, actual.z, 1e-9)
    }
}
