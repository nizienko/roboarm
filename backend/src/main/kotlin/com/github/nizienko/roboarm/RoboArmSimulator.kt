package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmState
import com.github.nizienko.roboarm.parts.Cran
import com.github.nizienko.roboarm.parts.Lift
import com.github.nizienko.roboarm.parts.Mast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds



class RoboArmSimulator(private val coroutineScope: CoroutineScope) {

    private val _armStateFlow = MutableStateFlow(ArmState())
    val armStateFlow: SharedFlow<ArmState> = _armStateFlow.asSharedFlow()

    val robot = Mast(Lift(Cran()))

    init {
        coroutineScope.launch {
            while (true) {
                _armStateFlow.value = ArmState()
                delay(1000.milliseconds)
            }
        }
    }

    fun reportCommand(command: ArmCommand) {
        println(command)
    }
}

data class Point3D(val x: Double, val y: Double, val z: Double)
data class RotationDegrees3D(val x: Double, val y: Double, val z: Double)
interface RoboPart {
    val localPosition: Point3D
    val localRotation: RotationDegrees3D
    val nextPart: RoboPart?
}

