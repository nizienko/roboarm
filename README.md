# Roboarm

## WebSocket commands

Connect to `ws://localhost:6593/ws`. Existing joint commands remain supported:

```json
{"partId":"<part id from telemetry>","toggleName":"Mast Rotation","value":90}
```

Move to a target with inverse kinematics:

```json
{"target":{"x":1,"y":1,"z":1.5}}
```

Coordinates are meters in the backend's Z-up coordinate system. X/Y specify the
Gripper pivot (the end of the two 1 m horizontal links). Z sets the Lift Height
directly, from 0.65 to 2.7 m; it is not the absolute world height of the gripper.
Telemetry uses scene coordinates `(x, z, -y)`.

The backend solves both elbow configurations, observes the Mast (0–360°) and
Cran (−120–120°) limits, and chooses the solution with the smallest sum of angle
changes from the current pose. The reachable X/Y radius is 1–2 m. Gripper rotation
and hand opening keep their current targets. Motion uses the existing joint speed
and acceleration limits; the tip's path is not constrained to a straight line.

Unreachable targets or invalid heights leave all targets unchanged and return:

```json
{"error":"Target is unreachable or outside joint limits"}
```

The frontend's **Target point** form sends this command and displays rejection
messages. Manual joint sliders remain available.
