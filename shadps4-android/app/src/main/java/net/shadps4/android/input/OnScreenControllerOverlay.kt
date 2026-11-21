package net.shadps4.android.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// Official Sony DualShock 4 Symbol Colors
private val Ds4TriangleGreen = Color(0xFF26C6DA)
private val Ds4CircleRed = Color(0xFFFF5252)
private val Ds4CrossBlue = Color(0xFF448AFF)
private val Ds4SquarePink = Color(0xFFE040FB)
private val Ds4LightbarBlue = Color(0xFF0091EA)

@Composable
fun OnScreenControllerOverlay(
    onPadStateUpdate: (TouchPadState) -> Unit
) {
    val padState = remember { TouchPadState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        // 1. TOP ROW: L2/L1 Shoulder Triggers + DualShock 4 Touchpad & Share/Options + R1/R2
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Bumpers: L2 & L1
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Ds4ShoulderButton(
                    label = "L2",
                    isTrigger = true,
                    onDown = {
                        padState.l2Trigger = 1.0f
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.l2Trigger = 0.0f
                        onPadStateUpdate(padState)
                    }
                )
                Ds4ShoulderButton(
                    label = "L1",
                    isTrigger = false,
                    onDown = {
                        padState.buttons = padState.buttons or TouchPadState.BTN_L1
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.buttons = padState.buttons and TouchPadState.BTN_L1.inv()
                        onPadStateUpdate(padState)
                    }
                )
            }

            // Center: SHARE  |  DS4 Lightbar Touchpad  |  OPTIONS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Ds4PillButton(
                    label = "SHARE",
                    onDown = {},
                    onUp = {}
                )

                Ds4CenterTouchpad(
                    onDown = {
                        padState.buttons = padState.buttons or TouchPadState.BTN_TOUCHPAD
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.buttons = padState.buttons and TouchPadState.BTN_TOUCHPAD.inv()
                        onPadStateUpdate(padState)
                    }
                )

                Ds4PillButton(
                    label = "OPTIONS",
                    onDown = {
                        padState.buttons = padState.buttons or TouchPadState.BTN_OPTIONS
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.buttons = padState.buttons and TouchPadState.BTN_OPTIONS.inv()
                        onPadStateUpdate(padState)
                    }
                )
            }

            // Right Bumpers: R1 & R2
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Ds4ShoulderButton(
                    label = "R1",
                    isTrigger = false,
                    onDown = {
                        padState.buttons = padState.buttons or TouchPadState.BTN_R1
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.buttons = padState.buttons and TouchPadState.BTN_R1.inv()
                        onPadStateUpdate(padState)
                    }
                )
                Ds4ShoulderButton(
                    label = "R2",
                    isTrigger = true,
                    onDown = {
                        padState.r2Trigger = 1.0f
                        onPadStateUpdate(padState)
                    },
                    onUp = {
                        padState.r2Trigger = 0.0f
                        onPadStateUpdate(padState)
                    }
                )
            }
        }

        // 2. LEFT CONTROLS: Classic Sony Segmented D-Pad + Left Analog Stick (L3)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // D-Pad Cluster
            Ds4DpadCluster(
                onButtonDown = { mask ->
                    padState.buttons = padState.buttons or mask
                    onPadStateUpdate(padState)
                },
                onButtonUp = { mask ->
                    padState.buttons = padState.buttons and mask.inv()
                    onPadStateUpdate(padState)
                }
            )

            // Left Analog Stick
            Ds4AnalogStick(
                label = "L3",
                onStickMove = { x, y ->
                    padState.leftStickX = x
                    padState.leftStickY = y
                    onPadStateUpdate(padState)
                }
            )
        }

        // 3. RIGHT CONTROLS: Right Analog Stick (R3) + Sacred Sony Symbols (△ ○ ✕ □)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Right Analog Stick
            Ds4AnalogStick(
                label = "R3",
                onStickMove = { x, y ->
                    padState.rightStickX = x
                    padState.rightStickY = y
                    onPadStateUpdate(padState)
                }
            )

            // Sacred Symbols Cluster
            Ds4ActionButtonsCluster(
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
    }
}

@Composable
fun Ds4CenterTouchpad(
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown()
                        tryAwaitRelease()
                        pressed = false
                        onUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Matte black/translucent touchpad body
            drawRoundRect(
                color = Color.Black.copy(alpha = if (pressed) 0.55f else 0.35f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Top glowing Blue DualShock 4 Lightbar strip
            drawRoundRect(
                color = Ds4LightbarBlue.copy(alpha = if (pressed) 1.0f else 0.8f),
                topLeft = Offset(14.dp.toPx(), 2.dp.toPx()),
                size = Size(size.width - 28.dp.toPx(), 3.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
        Text(
            text = "TOUCHPAD",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun Ds4PillButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(26.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown()
                        tryAwaitRelease()
                        pressed = false
                        onUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.Black.copy(alpha = if (pressed) 0.6f else 0.35f),
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = if (pressed) 0.8f else 0.35f),
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Ds4ShoulderButton(
    label: String,
    isTrigger: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(68.dp)
            .height(if (isTrigger) 42.dp else 36.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown()
                        tryAwaitRelease()
                        pressed = false
                        onUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cr = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            drawRoundRect(
                color = if (pressed) Ds4LightbarBlue.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.38f),
                cornerRadius = cr
            )
            drawRoundRect(
                color = if (pressed) Ds4LightbarBlue else Color.White.copy(alpha = 0.4f),
                cornerRadius = cr,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Ds4DpadCluster(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    Box(modifier = Modifier.size(150.dp)) {
        // UP
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            Ds4DpadKey("▲", onDown = { onButtonDown(TouchPadState.BTN_DPAD_UP) }, onUp = { onButtonUp(TouchPadState.BTN_DPAD_UP) })
        }
        // LEFT
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            Ds4DpadKey("◀", onDown = { onButtonDown(TouchPadState.BTN_DPAD_LEFT) }, onUp = { onButtonUp(TouchPadState.BTN_DPAD_LEFT) })
        }
        // RIGHT
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Ds4DpadKey("▶", onDown = { onButtonDown(TouchPadState.BTN_DPAD_RIGHT) }, onUp = { onButtonUp(TouchPadState.BTN_DPAD_RIGHT) })
        }
        // DOWN
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Ds4DpadKey("▼", onDown = { onButtonDown(TouchPadState.BTN_DPAD_DOWN) }, onUp = { onButtonUp(TouchPadState.BTN_DPAD_DOWN) })
        }
    }
}

@Composable
fun Ds4DpadKey(
    arrow: String,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown()
                        tryAwaitRelease()
                        pressed = false
                        onUp()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cr = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            drawRoundRect(
                color = if (pressed) Ds4LightbarBlue.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.42f),
                cornerRadius = cr
            )
            drawRoundRect(
                color = if (pressed) Color.White else Color.White.copy(alpha = 0.35f),
                cornerRadius = cr,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Text(
            text = arrow,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Ds4ActionButtonsCluster(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    Box(modifier = Modifier.size(164.dp)) {
        // TRIANGLE (Top - Green/Teal)
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            Ds4SymbolButton(
                symbolType = Ds4Symbol.TRIANGLE,
                accentColor = Ds4TriangleGreen,
                onDown = { onButtonDown(TouchPadState.BTN_TRIANGLE) },
                onUp = { onButtonUp(TouchPadState.BTN_TRIANGLE) }
            )
        }
        // SQUARE (Left - Pink/Magenta)
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            Ds4SymbolButton(
                symbolType = Ds4Symbol.SQUARE,
                accentColor = Ds4SquarePink,
                onDown = { onButtonDown(TouchPadState.BTN_SQUARE) },
                onUp = { onButtonUp(TouchPadState.BTN_SQUARE) }
            )
        }
        // CIRCLE (Right - Red/Orange)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Ds4SymbolButton(
                symbolType = Ds4Symbol.CIRCLE,
                accentColor = Ds4CircleRed,
                onDown = { onButtonDown(TouchPadState.BTN_CIRCLE) },
                onUp = { onButtonUp(TouchPadState.BTN_CIRCLE) }
            )
        }
        // CROSS (Bottom - Blue)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            Ds4SymbolButton(
                symbolType = Ds4Symbol.CROSS,
                accentColor = Ds4CrossBlue,
                onDown = { onButtonDown(TouchPadState.BTN_CROSS) },
                onUp = { onButtonUp(TouchPadState.BTN_CROSS) }
            )
        }
    }
}

enum class Ds4Symbol { TRIANGLE, CIRCLE, CROSS, SQUARE }

@Composable
fun Ds4SymbolButton(
    symbolType: Ds4Symbol,
    accentColor: Color,
    size: Dp = 54.dp,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onDown()
                        tryAwaitRelease()
                        pressed = false
                        onUp()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Glossy dark button cap
            drawCircle(
                color = if (pressed) accentColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.48f),
                radius = radius,
                center = center
            )
            // Outer rim
            drawCircle(
                color = if (pressed) accentColor else Color.White.copy(alpha = 0.35f),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Draw crisp geometric Sony sacred symbol
            val symStroke = 2.6.dp.toPx()
            val r = radius * 0.44f

            when (symbolType) {
                Ds4Symbol.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(center.x, center.y - r)
                        lineTo(center.x - r * 0.92f, center.y + r * 0.72f)
                        lineTo(center.x + r * 0.92f, center.y + r * 0.72f)
                        close()
                    }
                    drawPath(path = path, color = accentColor, style = Stroke(width = symStroke))
                }

                Ds4Symbol.CIRCLE -> {
                    drawCircle(
                        color = accentColor,
                        radius = r * 0.9f,
                        center = center,
                        style = Stroke(width = symStroke)
                    )
                }

                Ds4Symbol.CROSS -> {
                    val d = r * 0.78f
                    drawLine(
                        color = accentColor,
                        start = Offset(center.x - d, center.y - d),
                        end = Offset(center.x + d, center.y + d),
                        strokeWidth = symStroke
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(center.x + d, center.y - d),
                        end = Offset(center.x - d, center.y + d),
                        strokeWidth = symStroke
                    )
                }

                Ds4Symbol.SQUARE -> {
                    val side = r * 1.56f
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                        size = Size(side, side),
                        style = Stroke(width = symStroke)
                    )
                }
            }
        }
    }
}

@Composable
fun Ds4AnalogStick(
    label: String,
    onStickMove: (Float, Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 46f

    // Fast Squared-Distance Math for 120Hz responsiveness on Snapdragon 685
    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
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
                            val distSq = raw.x * raw.x + raw.y * raw.y
                            if (distSq > maxRadius * maxRadius) {
                                val dist = sqrt(distSq)
                                thumbOffset = Offset(raw.x / dist * maxRadius, raw.y / dist * maxRadius)
                            } else {
                                thumbOffset = raw
                            }
                            onStickMove(thumbOffset.x / maxRadius, thumbOffset.y / maxRadius)
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer Analog Base Ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = maxRadius + 14f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = maxRadius + 14f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // DualShock 4 Concave Thumbstick Cap
            val stickCenter = center + thumbOffset
            drawCircle(
                color = Color(0xFF151922).copy(alpha = 0.78f),
                radius = 28.dp.toPx(),
                center = stickCenter
            )
            drawCircle(
                color = Ds4LightbarBlue.copy(alpha = 0.75f),
                radius = 28.dp.toPx(),
                center = stickCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            // Inner grip ridge ring
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = 18.dp.toPx(),
                center = stickCenter,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}
