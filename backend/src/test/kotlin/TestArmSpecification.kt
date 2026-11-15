import com.github.nizienko.roboarm.ArmGeometrySpecification
import com.github.nizienko.roboarm.ArmSpecification
import com.github.nizienko.roboarm.BeamDimensions
import com.github.nizienko.roboarm.data.Point3D
import com.github.nizienko.roboarm.parts.DriveSpecification
import com.github.nizienko.roboarm.parts.MoveEngineParameters

val testArmSpecification = ArmSpecification(
    geometry = ArmGeometrySpecification(
        upperArm = BeamDimensions(1.0, 0.2, 0.2),
        forearm = BeamDimensions(1.0, 0.2, 0.2),
        mastOffset = Point3D(0.0, 0.0, 0.2),
        elbowHeightOffset = -0.25,
        wristHeightOffset = -0.05,
        gripperOffset = Point3D(-0.04, 0.0, -0.3),
    ),
    mast = DriveSpecification("Mast Rotation", "degrees", MoveEngineParameters(
        maxSpeed = 45.0, maxAcceleration = 25.0, circled = true,
    )),
    lift = DriveSpecification("Height", "meters", MoveEngineParameters(
        maxSpeed = 0.3, maxAcceleration = 0.5, minValue = 0.5, maxValue = 2.7,
    ), initialValue = 1.5),
    elbow = DriveSpecification("Elbow rotation", "degrees", MoveEngineParameters(
        maxSpeed = 45.0, maxAcceleration = 25.0, minValue = -160.0, maxValue = 160.0,
    )),
    wrist = DriveSpecification("Wrist rotation", "degrees", MoveEngineParameters(
        maxSpeed = 120.0, maxAcceleration = 60.0, circled = true,
    )),
    gripperJaw = DriveSpecification("Gripper opening", "meters", MoveEngineParameters(
        maxSpeed = 0.05, maxAcceleration = 0.1, minValue = 0.0, maxValue = 0.18,
    )),
)
