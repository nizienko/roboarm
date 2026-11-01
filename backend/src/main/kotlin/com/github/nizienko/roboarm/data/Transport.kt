package com.github.nizienko.roboarm.data

import kotlinx.serialization.Serializable

/*

 */

@Serializable
data class ArmState(
    val time: Long = System.currentTimeMillis(),
)

@Serializable
data class ArmCommand(val text: String)