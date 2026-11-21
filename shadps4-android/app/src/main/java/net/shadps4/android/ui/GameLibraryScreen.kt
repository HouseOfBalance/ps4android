package net.shadps4.android.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import uniffi.shadps4.GameItem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

// Authentic PlayStation 4 Orbis OS Colors
val Ps4DeepNavy = Color(0xFF030B1E)
val Ps4ElectricBlue = Color(0xFF00439C)
val Ps4CyanGlow = Color(0xFF00A2FF)
val Ps4GoldTrophy = Color(0xFFE5B842)
val Ps4PlusYellow = Color(0xFFFFD800)
val Ps4GlassWhite = Color(0x24FFFFFF)

data class GameUiModel(
    val titleId: String,
    val title: String,
    val appVersion: String,
    val iconPath: String?,
    val folderPath: String,
    val category: String = "Game Disc"
)

fun GameItem.toUiModel(): GameUiModel = GameUiModel(
    titleId = this.titleId,
    title = this.title,
    appVersion = this.appVersion,
    iconPath = this.iconPath,
    folderPath = this.folderPath,
    category = if (this.category == "gd") "PS4™ Game" else "System App"
)

@Composable
fun GameLibraryScreen(
    games: List<GameUiModel>,
    activeDriverName: String = "Default Adreno / System Vulkan",
    onGameClick: (GameUiModel) -> Unit,
    onPickDirectoryClick: () -> Unit,
    onDriverSettingsClick: () -> Unit
) {
    // Include built-in PS4 system tiles alongside scanned games
    val allEntries = remember(games) {
        val list = mutableListOf<Ps4XmbTile>()
        games.forEach { g ->
            list.add(Ps4XmbTile.Game(g))
        }
        // Always include classic PS4 System Library / Add Games tile & GPU Driver Toolbox tile
        list.add(Ps4XmbTile.AddGameFolder)
        list.add(Ps4XmbTile.GpuDriverToolbox)
        list
    }

    var selectedIndex by remember(allEntries.size) { mutableIntStateOf(0) }
    val currentTile = allEntries.getOrNull(selectedIndex) ?: Ps4XmbTile.AddGameFolder

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Dynamic PS4 Orbis Flow Wave Background (Optimized for 120Hz on Snapdragon 685)
        Ps4DynamicWaveBackground()

        // 2. Backdrop Artwork Overlay if selected item is a game with icon
        if (currentTile is Ps4XmbTile.Game && currentTile.game.iconPath != null) {
            AsyncImage(
                model = currentTile.game.iconPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.16f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Ps4DeepNavy.copy(alpha = 0.55f),
                                Color.Transparent,
                                Ps4DeepNavy.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
        }

        // 3. Main PS4 Home Screen Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: PS4 Function Area (Notifications, Profile, Trophies, GPU Driver, Clock)
            Ps4TopFunctionBar(
                gameCount = games.size,
                activeDriverName = activeDriverName,
                onScanFolderClick = onPickDirectoryClick,
                onDriverManagerClick = onDriverSettingsClick
            )

            // CENTER BAR: PS4 Content Area (Horizontal Carousel + Live Game Info Panel)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(175.dp)
                ) {
                    itemsIndexed(
                        items = allEntries,
                        key = { idx, item -> item.uniqueKey(idx) }
                    ) { index, tile ->
                        val isSelected = index == selectedIndex
                        Ps4CarouselTileItem(
                            tile = tile,
                            isSelected = isSelected,
                            onSelect = {
                                if (selectedIndex == index) {
                                    when (tile) {
                                        is Ps4XmbTile.Game -> onGameClick(tile.game)
                                        is Ps4XmbTile.AddGameFolder -> onPickDirectoryClick()
                                        is Ps4XmbTile.GpuDriverToolbox -> onDriverSettingsClick()
                                    }
                                } else {
                                    selectedIndex = index
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selected Item Detail Sub-Panel (Authentic PS4 Start Bar + Title + Metadata)
                Ps4SelectedItemDetailPanel(
                    tile = currentTile,
                    activeDriverName = activeDriverName,
                    onPrimaryAction = {
                        when (currentTile) {
                            is Ps4XmbTile.Game -> onGameClick(currentTile.game)
                            is Ps4XmbTile.AddGameFolder -> onPickDirectoryClick()
                            is Ps4XmbTile.GpuDriverToolbox -> onDriverSettingsClick()
                        }
                    }
                )
            }

            // BOTTOM BAR: Authentic PS4 DualShock 4 Button Prompts
            Ps4BottomButtonLegend(
                onStartClick = {
                    when (currentTile) {
                        is Ps4XmbTile.Game -> onGameClick(currentTile.game)
                        is Ps4XmbTile.AddGameFolder -> onPickDirectoryClick()
                        is Ps4XmbTile.GpuDriverToolbox -> onDriverSettingsClick()
                    }
                },
                onOptionsClick = onDriverSettingsClick,
                onScanClick = onPickDirectoryClick
            )
        }
    }
}

sealed class Ps4XmbTile {
    data class Game(val game: GameUiModel) : Ps4XmbTile()
    data object AddGameFolder : Ps4XmbTile()
    data object GpuDriverToolbox : Ps4XmbTile()

    fun uniqueKey(idx: Int): String = when (this) {
        is Game -> "game_${game.titleId}_$idx"
        is AddGameFolder -> "sys_add_folder"
        is GpuDriverToolbox -> "sys_gpu_driver"
    }
}

@Composable
fun Ps4DynamicWaveBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ps4_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF020B26),
                Color(0xFF002D72),
                Color(0xFF00439C),
                Color(0xFF03102E)
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        val w = size.width
        val h = size.height

        // Ambient radial light glow in center-right like PS4 Orbis OS
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Ps4CyanGlow.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.7f, h * 0.35f),
                radius = w * 0.55f
            ),
            center = Offset(w * 0.7f, h * 0.35f),
            radius = w * 0.55f
        )

        // Draw 2 lightweight Silk Ribbon Waves (Fast math for Snapdragon 685 120Hz)
        for (waveIdx in 0..1) {
            val path = Path()
            val yBase = h * (0.52f + waveIdx * 0.14f)
            val amp = h * (0.07f - waveIdx * 0.02f)
            val freq = if (waveIdx == 0) 1.5f else 2.1f
            val offsetPhase = phase + waveIdx * 1.4f

            path.moveTo(0f, yBase)
            val steps = 20
            val stepX = w / steps
            for (i in 1..steps) {
                val x = i * stepX
                val angle = (x / w) * freq * 3.14159f + offsetPhase
                val y = yBase + sin(angle) * amp
                path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = Color.White.copy(alpha = if (waveIdx == 0) 0.14f else 0.08f),
                style = Stroke(width = if (waveIdx == 0) 3.dp.toPx() else 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun Ps4TopFunctionBar(
    gameCount: Int,
    activeDriverName: String,
    onScanFolderClick: () -> Unit,
    onDriverManagerClick: () -> Unit
) {
    val currentTime = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: PlayStation Plus Badge + User Profile + Status Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PS Logo Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Ps4CyanGlow.copy(alpha = 0.2f))
                    .border(1.dp, Ps4CyanGlow.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PS4",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            // User Avatar & Online Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Ps4ElectricBlue)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Text(
                    text = "HouseOfBalance",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // PS Plus Golden Cross Icon
            Text(
                text = "✚",
                color = Ps4PlusYellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Trophy Level Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🏆", fontSize = 13.sp)
                Text(
                    text = "$gameCount Titles",
                    color = Ps4GoldTrophy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right: Quick Function Icons (Scan Folder, Turnip Driver, Clock)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Driver indicator chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onDriverManagerClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⚙ GPU: $activeDriverName",
                    color = Color(0xFFB3E5FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Add Game Folder Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onScanFolderClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "＋ Add Games",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Clock
            Text(
                text = currentTime,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun Ps4CarouselTileItem(
    tile: Ps4XmbTile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.18f else 0.92f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "tile_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
        animationSpec = tween(180),
        label = "tile_border"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .padding(horizontal = if (isSelected) 10.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (tile) {
                        is Ps4XmbTile.Game -> Color(0xFF0A1931)
                        is Ps4XmbTile.AddGameFolder -> Color(0xFF003680)
                        is Ps4XmbTile.GpuDriverToolbox -> Color(0xFF082046)
                    }
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSelect
                ),
            contentAlignment = Alignment.Center
        ) {
            when (tile) {
                is Ps4XmbTile.Game -> {
                    if (tile.game.iconPath != null) {
                        AsyncImage(
                            model = tile.game.iconPath,
                            contentDescription = tile.game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Sleek PS4 Disc Placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💿",
                                fontSize = 34.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tile.game.titleId,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom PS4 Cyan Selection Glow Bar
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Ps4CyanGlow)
                        )
                    }
                }

                is Ps4XmbTile.AddGameFolder -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("📁", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Library",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scan Folder",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                is Ps4XmbTile.GpuDriverToolbox -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("⚙️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Turnip GPU",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drivers",
                            color = Ps4CyanGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Ps4SelectedItemDetailPanel(
    tile: Ps4XmbTile,
    activeDriverName: String,
    onPrimaryAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Authentic PS4 "Start" Action Button below selected square
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(3.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
            ) {
                Text(
                    text = when (tile) {
                        is Ps4XmbTile.Game -> "Start"
                        is Ps4XmbTile.AddGameFolder -> "Select Directory"
                        is Ps4XmbTile.GpuDriverToolbox -> "Configure"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Large PS4 Typography Title & Subtitle
            Column {
                Text(
                    text = when (tile) {
                        is Ps4XmbTile.Game -> tile.game.title
                        is Ps4XmbTile.AddGameFolder -> "Add PS4 Game Directory (CUSA / PKG)"
                        is Ps4XmbTile.GpuDriverToolbox -> "Adreno Turnip / Mesa Vulkan Driver Manager"
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when (tile) {
                        is Ps4XmbTile.Game -> "${tile.game.category}  |  ${tile.game.titleId}  |  Version ${tile.game.appVersion}  |  FEXCore TSO Enabled"
                        is Ps4XmbTile.AddGameFolder -> "Mount game dump folder containing sce_sys/param.sfo and eboot.bin"
                        is Ps4XmbTile.GpuDriverToolbox -> "Active Driver: $activeDriverName • Custom KGSL Linker Namespace"
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun Ps4BottomButtonLegend(
    onStartClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cross (X) - Enter / Start
        Ps4ControllerPrompt(
            symbol = "✕",
            symbolColor = Color(0xFF64B5F6),
            label = "Enter",
            onClick = onStartClick
        )
        Spacer(modifier = Modifier.width(22.dp))

        // Triangle - Scan Game Folder
        Ps4ControllerPrompt(
            symbol = "△",
            symbolColor = Color(0xFF4DB6AC),
            label = "Add Folder",
            onClick = onScanClick
        )
        Spacer(modifier = Modifier.width(22.dp))

        // OPTIONS - GPU Driver Manager
        Ps4ControllerPrompt(
            symbol = "OPTIONS",
            symbolColor = Color.White,
            label = "GPU Drivers",
            isPill = true,
            onClick = onOptionsClick
        )
    }
}

@Composable
fun Ps4ControllerPrompt(
    symbol: String,
    symbolColor: Color,
    label: String,
    isPill: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (isPill) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = symbol,
                    color = symbolColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, symbolColor.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = symbolColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
