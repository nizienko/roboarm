package com.github.nizienko.roboarm.parts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

data class MoveEngineParameters(
    val maxSpeed: Double = 60.0,
    val maxAcceleration: Double = 120.0,
    val minValue: Double = 0.0,
    val maxValue: Double = 360.0,
    val circled: Boolean = false,
) {
    val range: ClosedFloatingPointRange<Double>
        get() = minValue..maxValue

    fun displacement(from: Double, to: Double): Double {
        var distance = to - from
        if (circled) {
            val period = maxValue - minValue
            distance %= period
            if (distance > period / 2.0) distance -= period
            if (distance < -period / 2.0) distance += period
        }
        return distance
    }

    init {
        require(maxSpeed.isFinite() && maxSpeed > 0.0)
        require(maxAcceleration.isFinite() && maxAcceleration > 0.0)
        require(minValue.isFinite() && maxValue.isFinite() && minValue < maxValue)
    }
}

class MoveEngine(
    scope: CoroutineScope,
    private val valueStateFlow: MutableStateFlow<Double>,
    parameters: MoveEngineParameters = MoveEngineParameters(),
) {
    private val model = MotionModel(valueStateFlow.value, parameters)
    private val limits = parameters
    private val _target = MutableStateFlow(valueStateFlow.value)
    val target: StateFlow<Double> = _target.asStateFlow()

    init {
        scope.launch {
            var lastTick = TimeSource.Monotonic.markNow()
            while (isActive) {
                delay(10.milliseconds)
                val now = TimeSource.Monotonic.markNow()
                val dt = (now - lastTick).inWholeNanoseconds / 1_000_000_000.0
                lastTick = now
                // Advance the old trajectory up to this tick before accepting a new goal.
                model.advance(dt)
                model.moveTo(_target.value)
                valueStateFlow.value = model.position
            }
        }
    }

    fun moveTo(targetValue: Double) {
        require(targetValue.isFinite() && targetValue in limits.minValue..limits.maxValue)
        _target.value = targetValue
    }
}

internal class MotionModel(initialPosition: Double, private val limits: MoveEngineParameters) {
    // Plan in continuous coordinates so crossing the seam does not change velocity.
    private var trajectoryPosition = initialPosition
    private var trajectoryTarget = initialPosition
    val position: Double
        get() = if (limits.circled && segments.isNotEmpty()) {
            val period = limits.maxValue - limits.minValue
            val offset = (trajectoryPosition - limits.minValue) % period
            limits.minValue + if (offset < 0.0) offset + period else offset
        } else {
            trajectoryPosition
        }
    var velocity: Double = 0.0
        private set
    var target: Double = initialPosition
        private set

    private data class Segment(val acceleration: Double, var remaining: Double)
    private val segments = ArrayDeque<Segment>()

    init {
        require(initialPosition.isFinite() && initialPosition in limits.minValue..limits.maxValue)
    }

    fun moveTo(value: Double) {
        require(value.isFinite() && value in limits.minValue..limits.maxValue)
        if (value == target) return
        target = value
        val distance = limits.displacement(position, value)
        trajectoryTarget = if (limits.circled) trajectoryPosition + distance else value
        segments.clear()
        val a = limits.maxAcceleration
        val stoppingPosition = trajectoryPosition + velocity * abs(velocity) / (2.0 * a)
        val direction = sign(trajectoryTarget - stoppingPosition)

        if (direction == 0.0 || direction * velocity < 0.0) {
            // The target is behind the stopping point: brake before returning.
            addSegment(-sign(velocity) * a, abs(velocity) / a)
            planFromRest(stoppingPosition)
        } else {
            plan(trajectoryPosition, direction * velocity, direction)
        }
    }

    private fun planFromRest(start: Double) {
        val direction = sign(trajectoryTarget - start)
        if (direction != 0.0) plan(start, 0.0, direction)
    }

    private fun plan(start: Double, speed: Double, direction: Double) {
        val a = limits.maxAcceleration
        val distance = abs(trajectoryTarget - start)
        val peakSpeed = min(limits.maxSpeed, sqrt(a * distance + speed * speed / 2.0))
        val acceleratingDistance = (peakSpeed * peakSpeed - speed * speed) / (2.0 * a)
        val brakingDistance = peakSpeed * peakSpeed / (2.0 * a)
        addSegment(direction * a, (peakSpeed - speed) / a)
        if (peakSpeed > 0.0) {
            addSegment(0.0, (distance - acceleratingDistance - brakingDistance) / peakSpeed)
        }
        addSegment(-direction * a, peakSpeed / a)
    }

    private fun addSegment(acceleration: Double, duration: Double) {
        if (duration > 0.0) segments.addLast(Segment(acceleration, duration))
    }

    fun advance(dtSeconds: Double) {
        require(dtSeconds.isFinite() && dtSeconds >= 0.0)
        var timeLeft = dtSeconds
        while (timeLeft > 0.0 && segments.isNotEmpty()) {
            val segment = segments.first()
            val step = min(timeLeft, segment.remaining)
            trajectoryPosition += velocity * step + segment.acceleration * step * step / 2.0
            velocity += segment.acceleration * step
            timeLeft -= step
            segment.remaining -= step
            if (segment.remaining <= 0.0) segments.removeFirst()
        }
        // Planned extrema stay within limits; discard roundoff at a hard boundary.
        if (!limits.circled) {
            trajectoryPosition = trajectoryPosition.coerceIn(limits.minValue, limits.maxValue)
        }
        if (segments.isEmpty()) {
            // Every planned profile ends at rest; remove accumulated floating-point error.
            trajectoryPosition = target
            velocity = 0.0
        }
    }
}
