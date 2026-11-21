package net.shadps4.android

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import net.shadps4.android.input.OnScreenControllerOverlay
import net.shadps4.android.input.TouchPadState
import uniffi.shadps4.startEmulation
import uniffi.shadps4.stopEmulation
import uniffi.shadps4.updateControllerPad
import uniffi.shadps4.updateEmulationSurface

class EmulatorActivity : ComponentActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private var gamePath: String = ""
    private var titleId: String = ""
    private var isEmulationStarted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gamePath = intent.getStringExtra("EXTRA_GAME_PATH") ?: ""
        titleId = intent.getStringExtra("EXTRA_TITLE_ID") ?: "CUSA00000"
        val titleName = intent.getStringExtra("EXTRA_TITLE_NAME") ?: "Game"

        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@EmulatorActivity)
        }

        // Overlay Jetpack Compose Touch Controls directly over Vulkan Surface
        val composeOverlay = ComposeView(this).apply {
            setContent {
                OnScreenControllerOverlay(
                    onPadStateUpdate = { state ->
                        sendPadDataToNative(state)
                    }
                )
            }
        }

        val root = FrameLayout(this).apply {
            addView(surfaceView)
            addView(composeOverlay)
        }

        setContentView(root)
        Toast.makeText(this, "Booting $titleName (:emu process)", Toast.LENGTH_SHORT).show()
    }

    private fun sendPadDataToNative(state: TouchPadState) {
        try {
            updateControllerPad(
                state.buttons,
                state.leftStickX,
                state.leftStickY,
                state.rightStickX,
                state.rightStickY,
                state.l2Trigger,
                state.r2Trigger
            )
        } catch (_: Throwable) {}
    }

    // Physical Gamepad Handling (DualShock 4 / DualSense / Xbox / Type-C Controller)
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and android.view.InputDevice.SOURCE_JOYSTICK) != 0) {
            val lx = event.getAxisValue(MotionEvent.AXIS_X)
            val ly = event.getAxisValue(MotionEvent.AXIS_Y)
            val rx = event.getAxisValue(MotionEvent.AXIS_Z)
            val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
            val l2 = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            val r2 = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

            try {
                updateControllerPad(0, lx, ly, rx, ry, l2, r2)
            } catch (_: Throwable) {}
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Physical button down mapping
        return super.onKeyDown(keyCode, event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val w = surfaceView.width
        val h = surfaceView.height
        // Pass actual ANativeWindow pointer via handwritten JNI
        nativeSetSurface(holder.surface, w, h)

        if (!isEmulationStarted) {
            isEmulationStarted = true
            try {
                startEmulation(gamePath, titleId, null, true)
            } catch (e: Throwable) {
                android.util.Log.e("ShadPS4", "Failed to start emulation", e)
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Recreate Vulkan swapchain with new dimensions without killing emulation
        nativeSetSurface(holder.surface, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Pause present queue and destroy surface only; emulation loop remains alive
        nativeSetSurface(null, 0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only kill emulation process when user actually exits the Activity
        try {
            stopEmulation()
        } catch (e: Throwable) {
            android.util.Log.e("ShadPS4", "Failed to stop emulation", e)
        }
    }

    private external fun nativeSetSurface(surface: Any?, width: Int, height: Int)

    // external fun nativeSetSurface(surface: Any)
    // external fun nativeSurfaceResized(width: Int, height: Int)
    // external fun nativeDestroySurface()

    companion object {
        init {
            // Load unified single shared library containing Rust host + Core + FEXCore
            try {
                System.loadLibrary("shadps4_emu_host")
            } catch (e: UnsatisfiedLinkError) {
                // Expected when native binary is not yet compiled for device
            }
        }
    }
}
