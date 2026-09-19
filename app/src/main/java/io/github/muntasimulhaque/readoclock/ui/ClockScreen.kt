package io.github.muntasimulhaque.readoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.muntasimulhaque.readoclock.R
import io.github.muntasimulhaque.readoclock.core.ClockHand
import io.github.muntasimulhaque.readoclock.core.ClockLayout
import io.github.muntasimulhaque.readoclock.core.ClockTime
import io.github.muntasimulhaque.readoclock.core.HandPick
import io.github.muntasimulhaque.readoclock.core.SpokenTime
import io.github.muntasimulhaque.readoclock.core.TickSchedule
import kotlinx.coroutines.delay
import kotlin.math.round

/**
 * One frame of the clock. The angles are quantized so the app sleeps between
 * real changes and only wakes for the tick, the bounce and the finger.
 */
private data class ClockFrame(
    val seconds: Double,
    val hour: Double,
    val minute: Double,
    val second: Double,
)

private fun frameOf(secondsOfDay: Double) = ClockFrame(
    seconds = secondsOfDay,
    hour = quantize(ClockTime.hourAngleDegrees(secondsOfDay)),
    minute = quantize(ClockTime.minuteAngleDegrees(secondsOfDay)),
    second = quantize(ClockTime.displayedSecondAngleDegrees(secondsOfDay)),
)

private fun quantize(degrees: Double): Double = round(degrees * 100.0) / 100.0

/**
 * The whole app: one dial. No buttons, no chrome. The screen drives the frame
 * clock, the host holds the time, and the rules live in core.
 */
@Composable
fun ClockScreen(
    reading: () -> Double,
    onMinuteDrag: (Double) -> Unit,
    onHourDrag: (Double) -> Unit,
    onMoveBy: (Double) -> Unit,
    onTick: () -> Unit,
) {
    val frame = remember { mutableStateOf(frameOf(reading())) }
    // The frame loop lives for the whole composition, so it must read the
    // latest reading function rather than the one it was born with.
    val currentReading by rememberUpdatedState(reading)
    val currentTick by rememberUpdatedState(onTick)
    val dragging = remember { mutableStateOf(false) }
    val schedule = remember { TickSchedule() }
    val refresh = { frame.value = frameOf(currentReading()) }
    // A jump is a reading the movement did not step to: a resume, a TalkBack
    // move, or the whole minute a released hand settles onto. It is marked
    // with a counter rather than armed here, because the tick collector below
    // is the only place the schedule is read, and the counter alone says a
    // jump happened even when the reading comes out identical to the last
    // one. Arming at the jump instead would race the collector, and arming
    // only when the reading changed would leave a jump unarmed and swallow
    // the next real tick.
    val jumpCount = remember { mutableStateOf(0) }
    val jump = {
        refresh()
        jumpCount.value++
    }

    // The tick is the movement's, not the finger's: one click when the
    // running second hand crosses a whole second. A drag turns the gear
    // train by hand and stays silent, and so does a jump. TickSchedule holds
    // that rule and the tests around it, including the stalled frame that
    // used to swallow a tick whole.
    LaunchedEffect(Unit) {
        var armedJump = jumpCount.value
        snapshotFlow { frame.value.seconds to jumpCount.value }.collect { (seconds, jump) ->
            if (jump != armedJump) {
                armedJump = jump
                schedule.arm(seconds)
            } else if (schedule.ticked(seconds, dragging.value)) {
                currentTick()
            }
        }
    }

    // A wake at every second boundary, and a short burst of frames through
    // the quartz bounce; between ticks the app sleeps. Dragging refreshes
    // straight from the finger, and the loop runs only while the app is
    // resumed.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            jump()
            while (true) {
                refresh()
                val fraction = frame.value.seconds % 1.0
                val waitMillis = if (fraction < ClockTime.QuartzTickSeconds + 0.02) {
                    8L
                } else {
                    (((1.0 - fraction) * 1000.0).toLong() - 4L).coerceIn(8L, 1000L)
                }
                delay(waitMillis)
            }
        }
    }

    val clockName = stringResource(R.string.clock)
    val stateText = stringResource(R.string.clock_state, SpokenTime.spoken(frame.value.seconds))
    val minuteForward = stringResource(R.string.move_minute_forward)
    val minuteBack = stringResource(R.string.move_minute_back)
    val hourForward = stringResource(R.string.move_hour_forward)
    val hourBack = stringResource(R.string.move_hour_back)

    // A jump: move the clock and re-arm, so the move is heard as the new
    // time and never as a tick.
    val moveBy = { delta: Double -> onMoveBy(delta); jump(); true }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ClockColors.field)
            .semantics(mergeDescendants = true) {
                contentDescription = clockName
                stateDescription = stateText
                customActions = listOf(
                    // A move is a jump, not the running movement, so it arms
                    // the schedule rather than ticking: a TalkBack user hears
                    // the new time, not a click for the hands being carried.
                    CustomAccessibilityAction(minuteForward) { moveBy(60.0) },
                    CustomAccessibilityAction(minuteBack) { moveBy(-60.0) },
                    CustomAccessibilityAction(hourForward) { moveBy(3600.0) },
                    CustomAccessibilityAction(hourBack) { moveBy(-3600.0) },
                )
            },
    ) {
        val radius = ClockLayout
            .caseRadius(constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble())
            .toFloat()
        val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
        ClockDial(
            hour = frame.value.hour,
            minute = frame.value.minute,
            second = frame.value.second,
            radius = radius,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(radius) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // The finger must land on a hand, as on a real
                        // clock: a touch on the dial, on the case, or off
                        // the clock holds nothing and is passed on.
                        val hand = HandPick.nearest(
                            pointX = (down.position.x - center.x).toDouble() / radius,
                            pointY = (down.position.y - center.y).toDouble() / radius,
                            hourAngle = ClockTime.hourAngleDegrees(currentReading()),
                            minuteAngle = ClockTime.minuteAngleDegrees(currentReading()),
                        ) ?: return@awaitEachGesture
                        // A poke is not a drag either: the hand only moves
                        // once the finger has crossed touch slop, so a tap,
                        // however jittery, never jumps a hand across the
                        // dial. The hand was picked at the down point.
                        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture
                        fun moveHand(position: Offset) {
                            val angle = ClockTime.angleDegrees(
                                x = position.x.toDouble(),
                                y = position.y.toDouble(),
                                centerX = center.x.toDouble(),
                                centerY = center.y.toDouble(),
                            )
                            when (hand) {
                                ClockHand.Minute -> onMinuteDrag(angle)
                                ClockHand.Hour -> onHourDrag(angle)
                            }
                            refresh()
                        }
                        dragging.value = true
                        try {
                            moveHand(dragStart.position)
                            drag(down.id) { change ->
                                moveHand(change.position)
                                change.consume()
                            }
                        } finally {
                            dragging.value = false
                            // The finger let go, and the host settles the hand
                            // onto a whole minute when it was close to one.
                            // That settle is a jump, not a step.
                            jump()
                        }
                    }
                },
        )
    }
}
