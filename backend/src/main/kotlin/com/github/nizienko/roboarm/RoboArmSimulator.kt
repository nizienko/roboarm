package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds



class RoboArmService(private val coroutineScope: CoroutineScope) {

    private val _armStateFlow = MutableStateFlow(ArmState())
    val armStateFlow: SharedFlow<ArmState> = _armStateFlow.asSharedFlow()

    init {
        coroutineScope.launch {
            while (true) {
                _armStateFlow.value = ArmState()
                delay(1000.milliseconds)
            }
        }
    }

    fun reportCommand(command: ArmCommand) {

    }
}