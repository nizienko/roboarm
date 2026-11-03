# Roboarm

A robot arm simulator: Kotlin/Ktor backend, React/Three.js frontend.
The backend handles motion, speed and acceleration limits, and inverse kinematics.

## Video

[![Watch the Roboarm demo on YouTube](https://img.youtube.com/vi/GP640ZPVUUQ/hqdefault.jpg)](https://youtu.be/GP640ZPVUUQ)

## Run

With Docker and Docker Compose installed, run from this directory:

```sh
docker compose up --build
```

Open http://localhost:8080. The backend uses `ws://localhost:6593/ws`, so open
the page on the same computer that runs Docker. The first build takes a few minutes.

Stop with `Ctrl+C`, then `docker compose down` to remove the containers.
If Compose is installed separately, use `docker-compose` instead of `docker compose`.

## Controls

Use the joint sliders to move individual axes, or set X/Y/Z and press **Send command**
to move the gripper pivot to a point. Coordinates are in meters, with Z pointing up.
The reachable horizontal radius is 1–2 m; the gripper hand height is 0.05–2.30 m.
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
