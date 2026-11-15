# Roboarm

A robot arm simulator: Kotlin/Ktor backend, React/Three.js frontend.
The backend handles motion, speed and acceleration limits, and inverse kinematics.

## Video

[![Watch the Roboarm demo on YouTube](https://img.youtube.com/vi/0KyGskisj0o/hqdefault.jpg)](https://youtu.be/0KyGskisj0o)

## Run

With Docker and Docker Compose installed, run from this directory:

```sh
docker compose up --build
```

Open http://localhost:8080. By default, the frontend connects to `ws://localhost:6593/ws`,
so open the page on the same computer that runs Docker. The first build takes a few minutes.

To use another backend, set its full WebSocket URL (including `/ws`) when building:

```sh
VITE_BACKEND_URL=wss://robot.example.com/ws docker compose up --build
```

The URL must be accessible from the browser. Use `wss://` when serving the frontend
over HTTPS. The value is embedded at build time; changing it requires rebuilding the frontend.

Stop with `Ctrl+C`, then `docker compose down` to remove the containers.
If Compose is installed separately, use `docker-compose` instead of `docker compose`.

## Controls

Use the joint sliders to move individual axes, or set X/Y/Z and press **Send command**
to move the wrist axis to a point. Coordinates are in meters, with Z pointing up.
Wrist rotation and jaw opening remain unchanged.
Reachability depends on the configured beam lengths, lift travel and joint limits.
Unreachable targets are rejected. Drag the scene to orbit the camera; scroll to zoom.

The arm reconnects automatically if the backend restarts. All browser tabs control
the same arm. Motion has no collision detection, and the gripper path need not be straight.

## Local development

Requires JDK 21 and Node.js 24+.

```sh
cd backend
./gradlew run
```

In another terminal:

```sh
cd frontend
npm ci
npm run dev
```

Tests: `./gradlew test` in `backend`; `npm test` and `npm run lint` in `frontend`.

To configure the backend URL locally, copy `frontend/.env.example` to
`frontend/.env.local` and edit `VITE_BACKEND_URL`, then restart Vite. You can also
set it directly: `VITE_BACKEND_URL=ws://localhost:7000/ws npm run dev` from `frontend`.

## Arm configuration

Edit `DefaultArmSpecification.value` in
`backend/src/main/kotlin/com/github/nizienko/roboarm/ArmSpecification.kt`, then restart
the backend (or rebuild with Docker). It defines the two beam dimensions, joint
offsets, and each drive's initial position, limits, maximum speed and acceleration.
Lengths are in meters, angles in degrees; speed and acceleration use the corresponding
units per second and per second squared. Initial positions must be within the limits.
For a rotary axis with physical stops, set `circled = false`; continuous rotation
requires a 360-degree range. The topology remains the same two planar links and lift.

Beam lengths are measured between joint axes and used by both forward and inverse
kinematics. The frontend builds the two beams from server dimensions; motor, mast
and gripper geometry come from `frontend/public/models/roboarm.glb` (source: `roboarm.blend`).
Changing the configuration keeps the same two-link arm topology.
