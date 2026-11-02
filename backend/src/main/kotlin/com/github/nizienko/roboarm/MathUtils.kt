package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.Point3D
import kotlin.math.cos
import kotlin.math.sin

fun Point3D.rotateZ(degrees: Double): Point3D {
    val radians = Math.toRadians(degrees)
    val c = cos(radians)
    val s = sin(radians)
    return Point3D(x * c - y * s, x * s + y * c, z)
}

operator fun Point3D.plus(other: Point3D): Point3D = Point3D(x + other.x, y + other.y, z + other.z)
