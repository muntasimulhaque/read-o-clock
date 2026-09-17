package io.github.muntasimulhaque.readoclock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import io.github.muntasimulhaque.readoclock.host.ClockHost
import io.github.muntasimulhaque.readoclock.host.TickPlayer
import io.github.muntasimulhaque.readoclock.ui.ClockScreen

class MainActivity : ComponentActivity() {

    private val host: ClockHost by lazy {
        ViewModelProvider(this)[ClockHost::class.java]
    }

    private val ticker by lazy { TickPlayer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepBarsHidden()
        // A clock you are teaching with must not go dark mid-lesson. The view
        // flag needs no permission.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            ClockScreen(
                reading = host::readingSeconds,
                onMinuteDrag = host::setFromMinuteAngle,
                onHourDrag = host::setFromHourAngle,
                onMoveBy = host::moveBy,
                onTick = ticker::tick,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        keepBarsHidden()
    }

    override fun onDestroy() {
        ticker.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) keepBarsHidden()
    }

    // The clock owns the screen: no status bar, no navigation bar, nothing
    // but the dial, and the bars only flash back transiently on a swipe.
    private fun keepBarsHidden() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
