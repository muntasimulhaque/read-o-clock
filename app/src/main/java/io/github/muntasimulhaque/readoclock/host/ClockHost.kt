package io.github.muntasimulhaque.readoclock.host

import androidx.lifecycle.ViewModel
import io.github.muntasimulhaque.readoclock.core.ClockSetter
import io.github.muntasimulhaque.readoclock.core.ClockTime
import java.util.TimeZone

/**
 * The clock's host: it owns the one mutable thing, the offset from live time,
 * and reads the wall clock (timing lives only in the host, never in the
 * rules). Everything the screen needs is a function call, so no composable
 * ever sees a ViewModel.
 */
class ClockHost : ViewModel() {
    private val setter = ClockSetter()

    fun readingSeconds(): Double = setter.reading(liveSeconds())

    fun setFromMinuteAngle(angle: Double) = setter.setFromMinuteAngle(liveSeconds(), angle)

    fun setFromHourAngle(angle: Double) = setter.setFromHourAngle(liveSeconds(), angle)

    fun moveBy(deltaSeconds: Double) = setter.moveBy(liveSeconds(), deltaSeconds)

    private fun liveSeconds(): Double {
        val nowMillis = System.currentTimeMillis()
        return ClockTime.secondsOfDay(nowMillis, TimeZone.getDefault().getOffset(nowMillis))
    }
}
