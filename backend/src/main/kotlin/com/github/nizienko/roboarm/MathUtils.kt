package com.github.nizienko.roboarm

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun multiply(
    a: Array<DoubleArray>,
    b: Array<DoubleArray>,
): Array<DoubleArray> {
    require(a.isNotEmpty() && b.isNotEmpty())
    require(a[0].isNotEmpty() && b[0].isNotEmpty())
    require(a.all { it.size == a[0].size })
    require(b.all { it.size == b[0].size })

    // Число столбцов A должно совпадать с числом строк B.
    require(a[0].size == b.size)

    return Array(a.size) { row ->
        DoubleArray(b[0].size) { col ->
            var sum = 0.0

            for (k in b.indices) {
                sum += a[row][k] * b[k][col]
            }

            sum
        }
    }
}

fun RotationDegrees3D.toMatrix(): Array<DoubleArray> {
    val rx = x * PI / 180.0
    val ry = y * PI / 180.0
    val rz = z * PI / 180.0

    val rotationX = arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, cos(rx), -sin(rx)),
        doubleArrayOf(0.0, sin(rx), cos(rx)),
    )

    val rotationY = arrayOf(
        doubleArrayOf(cos(ry), 0.0, sin(ry)),
        doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(-sin(ry), 0.0, cos(ry)),
    )

    val rotationZ = arrayOf(
        doubleArrayOf(cos(rz), -sin(rz), 0.0),
        doubleArrayOf(sin(rz), cos(rz), 0.0),
        doubleArrayOf(0.0, 0.0, 1.0),
    )

    return multiply(rotationZ, multiply(rotationY, rotationX))
}

fun Point3D.rotate(rotation: RotationDegrees3D): Point3D {
    val column = arrayOf(
        doubleArrayOf(x),
        doubleArrayOf(y),
        doubleArrayOf(z),
    )
    val rotated = multiply(rotation.toMatrix(), column)
    return Point3D(rotated[0][0], rotated[1][0], rotated[2][0])
}
