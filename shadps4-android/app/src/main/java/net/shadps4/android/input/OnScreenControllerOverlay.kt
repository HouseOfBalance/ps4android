package net.shadps4.android.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun OnScreenControllerOverlay(
    onPadStateUpdate: (TouchPadState) -> Unit
) {
    val padState = remember { TouchPadState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // D-Pad (Left side)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 48.dp)
                .size(160.dp)
        ) {
            VirtualJoystick(
                onStickMove = { x, y ->
                    padState.leftStickX = x
                    padState.leftStickY = y
                    onPadStateUpdate(padState)
                }
            )
        }

        // Action Buttons: Square, Triangle, Circle, Cross (Right side)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 48.dp)
                .size(180.dp)
        ) {
            ActionButtonsPad(
                onButtonDown = { mask ->
                    padState.buttons = padState.buttons or mask
                    onPadStateUpdate(padState)
                },
                onButtonUp = { mask ->
                    padState.buttons = padState.buttons and mask.inv()
                    onPadStateUpdate(padState)
                }
            )
        }

        // Shoulder Buttons (L1, L2, R1, R2)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VirtualButton("L2", onButtonDown = {
                    padState.l2Trigger = 1.0f
                    onPadStateUpdate(padState)
                }, onButtonUp = {
                    padState.l2Trigger = 0.0f
                    onPadStateUpdate(padState)
                })
                VirtualButton("L1", onButtonDown = {
                    padState.buttons = padState.buttons or TouchPadState.BTN_L1
                    onPadStateUpdate(padState)
                }, onButtonUp = {
                    padState.buttons = padState.buttons and TouchPadState.BTN_L1.inv()
                    onPadStateUpdate(padState)
                })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VirtualButton("R1", onButtonDown = {
                    padState.buttons = padState.buttons or TouchPadState.BTN_R1
                    onPadStateUpdate(padState)
                }, onButtonUp = {
                    padState.buttons = padState.buttons and TouchPadState.BTN_R1.inv()
                    onPadStateUpdate(padState)
                })
                VirtualButton("R2", onButtonDown = {
                    padState.r2Trigger = 1.0f
                    onPadStateUpdate(padState)
                }, onButtonUp = {
                    padState.r2Trigger = 0.0f
                    onPadStateUpdate(padState)
                })
            }
        }
    }
}

@Composable
fun VirtualJoystick(onStickMove: (Float, Float) -> Unit) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 60f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onStickMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = thumbOffset + dragAmount
                        val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                        thumbOffset = if (dist > maxRadius) {
                            Offset(raw.x / dist * maxRadius, raw.y / dist * maxRadius)
                        } else {
                            raw
                        }
                        onStickMove(thumbOffset.x / maxRadius, thumbOffset.y / maxRadius)
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = maxRadius + 15f,
            center = center,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = 32f,
            center = center + thumbOffset
        )
    }
}

@Composable
fun ActionButtonsPad(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            VirtualButton("△", onButtonDown = { onButtonDown(TouchPadState.BTN_TRIANGLE) }, onButtonUp = { onButtonUp(TouchPadState.BTN_TRIANGLE) })
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            VirtualButton("□", onButtonDown = { onButtonDown(TouchPadState.BTN_SQUARE) }, onButtonUp = { onButtonUp(TouchPadState.BTN_SQUARE) })
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            VirtualButton("○", onButtonDown = { onButtonDown(TouchPadState.BTN_CIRCLE) }, onButtonUp = { onButtonUp(TouchPadState.BTN_CIRCLE) })
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            VirtualButton("✕", onButtonDown = { onButtonDown(TouchPadState.BTN_CROSS) }, onButtonUp = { onButtonUp(TouchPadState.BTN_CROSS) })
        }
    }
}

@Composable
fun VirtualButton(
    label: String,
    onButtonDown: () -> Unit,
    onButtonUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onButtonDown()
                        tryAwaitRelease()
                        onButtonUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White.copy(alpha = 0.25f))
            drawCircle(color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))
        }
        Text(text = label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
