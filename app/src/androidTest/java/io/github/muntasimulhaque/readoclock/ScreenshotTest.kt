package io.github.muntasimulhaque.readoclock

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.readoclock.ui.ClockScreen
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Compose renders of the shipped dial, for the Play Store listing and
 * for the human drift check after UI changes (AGENTS.md, Build).
 *
 * These are ordinary state renders, not a live playthrough: the one screen is
 * handed straight to its composable inside a bare activity, which is only
 * possible because no composable in this app takes a ViewModel. The harness
 * deliberately avoids the compose test rule and everything under it: no
 * touch injection and no semantics queries are needed to render and copy
 * pixels, and dropping that machinery keeps these captures working on
 * whatever framework image the app targets, forever.
 *
 * The PNGs are written to the directory the instrumentation reports as
 * additional test output; the screenshots workflow
 * (.github/workflows/screenshots.yml) pulls them off the emulator and
 * prefixes each with the form factor.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private fun resolveOutDir(): File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (path != null) {
            val dir = File(path)
            if (dir.isDirectory || dir.mkdirs()) return dir
            // Cold-booted emulators can lag mounting shared storage; fall
            // back rather than fail.
        }
        return File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath,
        ).apply { mkdirs() }
    }

    /** One activity hosts every scene: each is a state change pushed into it. */
    private val render = mutableStateOf<@Composable () -> Unit>({})

    private fun launch(): ActivityScenario<ComponentActivity> {
        // Right after a cold boot the package manager can briefly refuse to
        // resolve; a short retry absorbs it without masking real breakage.
        var lastError: RuntimeException? = null
        repeat(3) { attempt ->
            try {
                val scenario = ActivityScenario.launch(ComponentActivity::class.java)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                    val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    activity.setContent { render.value() }
                }
                settle()
                return scenario
            } catch (e: RuntimeException) {
                lastError = e
                Thread.sleep(5000L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("could not launch the host activity")
    }

    private fun push(block: @Composable () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { render.value = block }
        settle()
    }

    /** Drain the main thread, then give animations a beat to land. */
    private fun settle() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
        Thread.sleep(SETTLE_MS)
    }

    /**
     * The six store captures, one per teaching state: the hero, an o'clock,
     * half past, quarter to, straight down, quarter past. Each scene pins the
     * second hand just after its tick has settled, so the dial is quiet in
     * the still.
     */
    @Test
    fun captureStoreScreenshots() {
        val outDir = resolveOutDir()
        val scenario = launch()
        shot(scenario, outDir, "01_hero_ten_past_ten", at(10, 10))
        shot(scenario, outDir, "02_one_oclock", at(1, 0))
        shot(scenario, outDir, "03_half_past_three", at(3, 30))
        shot(scenario, outDir, "04_quarter_to_nine", at(8, 45))
        shot(scenario, outDir, "05_six_oclock", at(6, 0))
        shot(scenario, outDir, "06_quarter_past_nine", at(9, 15))
        scenario.close()
    }

    private fun at(hour: Int, minute: Int): Double = hour * 3600.0 + minute * 60.0 + 0.5

    private fun shot(
        scenario: ActivityScenario<ComponentActivity>,
        outDir: File,
        name: String,
        secondsOfDay: Double,
    ) {
        push {
            ClockScreen(
                reading = { secondsOfDay },
                onMinuteDrag = {},
                onHourDrag = {},
                onMoveBy = {},
            )
        }
        lateinit var bitmap: Bitmap
        scenario.onActivity { activity -> bitmap = captureWindow(activity) }
        File(outDir, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun captureWindow(activity: ComponentActivity): Bitmap {
        val decor = activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        PixelCopy.request(activity.window, bitmap, { result ->
            if (result != PixelCopy.SUCCESS) {
                // Software draw as the fallback path; static scenes render fine.
                decor.draw(android.graphics.Canvas(bitmap))
            }
            latch.countDown()
        }, Handler(Looper.getMainLooper()))
        latch.await(10, TimeUnit.SECONDS)
        return bitmap
    }

    private companion object {
        const val SETTLE_MS = 600L
    }
}
