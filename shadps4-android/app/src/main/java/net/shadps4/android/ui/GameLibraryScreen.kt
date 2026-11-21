package net.shadps4.android.ui

import android.graphics.Typeface
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import uniffi.shadps4.GameItem
import java.text.SimpleDateFormat
import java.util.*

// Exact Colors from d4v1-sudo/Ps4-UI main.css
val Ps4UiBackgroundBlue = Color(0xFF0E6CC4)
val Ps4UiWaveCyan = Color(0xFF00AAFF)
val Ps4UiNotificationDark = Color(0xFF222222)
val Ps4UiNotificationBlue = Color(0xFF0091FF)
val Ps4CyanGlow = Color(0xFF00A2FF)

data class GameUiModel(
    val titleId: String,
    val title: String,
    val appVersion: String,
    val iconPath: String?,
    val folderPath: String,
    val category: String = "PS4™ Game",
    val actionLabel: String = "Start",
    val isPlayStationStore: Boolean = false,
    val isSettingsTile: Boolean = false,
    val isScanFolderTile: Boolean = false,
    val isShutdownTile: Boolean = false
)

fun GameItem.toUiModel(): GameUiModel = GameUiModel(
    titleId = this.titleId,
    title = this.title,
    appVersion = this.appVersion,
    iconPath = this.iconPath,
    folderPath = this.folderPath,
    category = if (this.category == "gd") "PS4™ Game" else "System App",
    actionLabel = "Start"
)

@Composable
fun rememberPs4SstFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember {
        try {
            val tf = Typeface.createFromAsset(context.assets, "ps4ui/font/sst.ttf")
            FontFamily(tf)
        } catch (_: Throwable) {
            FontFamily.SansSerif
        }
    }
}

@Composable
fun GameLibraryScreen(
    games: List<GameUiModel>,
    activeDriverName: String = "Default Adreno / System Vulkan",
    isUnlockedInitial: Boolean = false,
    onUnlockScreen: () -> Unit = {},
    onGameClick: (GameUiModel) -> Unit,
    onPickDirectoryClick: () -> Unit,
    onDriverSettingsClick: () -> Unit,
    onShutdownClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sstFont = rememberPs4SstFontFamily()

    var isUnlocked by remember { mutableStateOf(isUnlockedInitial) }
    var showTrophyNotification by remember { mutableStateOf(false) }

    // Cycling news ticker from d4v1-sudo/Ps4-UI script.js timeout()
    val infoMessages = remember(activeDriverName) {
        listOf(
            "God of War Ragnarok now available!",
            "The Last of Us Part II new update",
            "Ghosts'n Goblins demo available now",
            "GPU Driver: $activeDriverName"
        )
    }
    var infoIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            infoIndex = (infoIndex + 1) % infoMessages.size
        }
    }

    // Auto-hide Trophy Toast after 4 seconds like script.js showNotification()
    LaunchedEffect(showTrophyNotification) {
        if (showTrophyNotification) {
            delay(4000)
            showTrophyNotification = false
        }
    }

    // 1. LOCK SCREEN: exact reproduction of <img id="imagem" src="./assets/img/start.png"> + "Press PS button or space"
    if (!isUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ps4UiBackgroundBlue)
                .clickable {
                    Ps4AudioManager.playStartup(context)
                    isUnlocked = true
                    onUnlockScreen()
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "file:///android_asset/ps4ui/img/start.png",
                contentDescription = "Start Screen",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Pulsing prompt at top: 70% from main.css #mensagem
            val infiniteTransition = rememberInfiniteTransition(label = "ps_prompt")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "prompt_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Press PS button or tap screen to start",
                    color = Color.White.copy(alpha = alpha),
                    fontFamily = sstFont,
                    fontSize = 18.sp
                )
            }
        }
        return
    }

    // Build full list of tiles matching d4v1-sudo/Ps4-UI index.html #games
    val allTiles = remember(games) {
        val list = mutableListOf<GameUiModel>()
        // 1. PlayStation Store tile (with 2x4 sub-grid)
        list.add(
            GameUiModel(
                titleId = "STORE",
                title = "Playstation Store",
                appVersion = "1.0",
                iconPath = "file:///android_asset/ps4ui/img/ps.png",
                folderPath = "",
                actionLabel = "Start",
                isPlayStationStore = true
            )
        )
        // 2. What's New tile (opens Scan Folder so user can add real PS4 dumps easily)
        list.add(
            GameUiModel(
                titleId = "WHATS_NEW",
                title = "What's New (Add PS4 Folder)",
                appVersion = "1.0",
                iconPath = "file:///android_asset/ps4ui/img/new.png",
                folderPath = "",
                actionLabel = "Scan",
                isScanFolderTile = true
            )
        )
        // 3. All user/showcase games
        list.addAll(games)
        // 4. Settings tile (opens GPU Turnip Driver Manager)
        list.add(
            GameUiModel(
                titleId = "SETTINGS",
                title = "Settings (Turnip GPU Drivers)",
                appVersion = "1.0",
                iconPath = "file:///android_asset/ps4ui/img/settings.png",
                folderPath = "",
                actionLabel = "Open",
                isSettingsTile = true
            )
        )
        // 5. Turn Off tile (#shutdown)
        list.add(
            GameUiModel(
                titleId = "TURN_OFF",
                title = "Turn Off",
                appVersion = "1.0",
                iconPath = "file:///android_asset/ps4ui/img/off.png",
                folderPath = "",
                actionLabel = "Shutdown",
                isShutdownTile = true
            )
        )
        list
    }

    // Default focus on first game or What's New
    var selectedIndex by remember(allTiles.size) {
        mutableIntStateOf(if (allTiles.size > 2) 2 else 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ps4UiBackgroundBlue)
    ) {
        // 2. DRIFTING ROTATING WAVES (.box > .wave -one, -two, -three from main.css)
        Ps4UiDriftingWavesBackground()

        // 3. TOP HEADER BAR (<header> from index.html: #plus, #info, #textInfo, #friends, #user, #userName, #trophies, #clock)
        Ps4UiHeaderBar(
            sstFont = sstFont,
            infoText = infoMessages[infoIndex],
            trophyCount = games.size,
            onPlusClick = {
                Ps4AudioManager.playReward(context)
                showTrophyNotification = true
            },
            onSettingsClick = {
                Ps4AudioManager.playNavigate(context)
                onDriverSettingsClick()
            },
            onScanClick = {
                Ps4AudioManager.playNavigate(context)
                onPickDirectoryClick()
            }
        )

        // 4. HORIZONTAL GAMES RIBBON (<div id="games"> with .squareGame tiles)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 74.dp),
            verticalArrangement = Arrangement.Top
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    items = allTiles,
                    key = { idx, item -> "${item.titleId}_$idx" }
                ) { index, item ->
                    val isFocused = index == selectedIndex
                    Ps4UiSquareGameTile(
                        item = item,
                        isFocused = isFocused,
                        sstFont = sstFont,
                        onClick = {
                            if (selectedIndex != index) {
                                Ps4AudioManager.playNavigate(context)
                                selectedIndex = index
                            } else {
                                when {
                                    item.isPlayStationStore -> {
                                        Ps4AudioManager.playReward(context)
                                        showTrophyNotification = true
                                    }
                                    item.isScanFolderTile -> {
                                        Ps4AudioManager.playNavigate(context)
                                        onPickDirectoryClick()
                                    }
                                    item.isSettingsTile -> {
                                        Ps4AudioManager.playNavigate(context)
                                        onDriverSettingsClick()
                                    }
                                    item.isShutdownTile -> {
                                        Ps4AudioManager.playShutdown(context)
                                        isUnlocked = false
                                        onShutdownClick()
                                    }
                                    else -> {
                                        Ps4AudioManager.playNavigate(context)
                                        onGameClick(item)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // 5. PLAYSTATION STORE SUB-GRID (<div id="store"> shown when PlayStation Store tile is focused)
            val focusedTile = allTiles.getOrNull(selectedIndex)
            if (focusedTile?.isPlayStationStore == true) {
                Ps4UiStoreSubGrid(
                    onStoreItemClick = { title ->
                        Ps4AudioManager.playNavigate(context)
                        onPickDirectoryClick()
                    }
                )
            }
        }

        // 6. TROPHY ACHIEVEMENT NOTIFICATION POPUP (.notification-container from script.js)
        AnimatedVisibility(
            visible = showTrophyNotification,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp)
        ) {
            Ps4UiTrophyNotificationToast(sstFont = sstFont)
        }
    }
}

/**
 * Exact reproduction of .box { transform: rotate(80deg); }
 * and .wave -one (#0af), .wave -two (black), .wave -three (#0e6cc4) from main.css lines 435-487.
 */
@Composable
fun Ps4UiDriftingWavesBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ps4_drift_waves")

    val rot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_one"
    )
    val rot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_two"
    )
    val rot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_three"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw 3 large rounded drifting wave blobs positioned on the left/bottom as in d4v1-sudo/Ps4-UI
        val waveSize = Size(w * 1.15f, h * 1.95f)
        val radius = CornerRadius(waveSize.width * 0.43f, waveSize.height * 0.43f)
        val centerPivot = Offset(w * 0.18f, h * 0.65f)

        // Wave One (#00AAFF, opacity 0.4)
        withTransform({
            rotate(degrees = rot1 + 80f, pivot = centerPivot)
        }) {
            drawRoundRect(
                color = Ps4UiWaveCyan.copy(alpha = 0.35f),
                topLeft = Offset(centerPivot.x - waveSize.width * 0.5f, centerPivot.y - waveSize.height * 0.48f),
                size = waveSize,
                cornerRadius = radius
            )
        }

        // Wave Two (Black, opacity 0.28)
        withTransform({
            rotate(degrees = rot2 + 80f, pivot = centerPivot)
        }) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(centerPivot.x - waveSize.width * 0.52f, centerPivot.y - waveSize.height * 0.46f),
                size = waveSize,
                cornerRadius = radius
            )
        }

        // Wave Three (#0E6CC4, opacity 0.4)
        withTransform({
            rotate(degrees = rot3 + 80f, pivot = centerPivot)
        }) {
            drawRoundRect(
                color = Ps4UiBackgroundBlue.copy(alpha = 0.4f),
                topLeft = Offset(centerPivot.x - waveSize.width * 0.48f, centerPivot.y - waveSize.height * 0.5f),
                size = waveSize,
                cornerRadius = radius
            )
        }
    }
}

/**
 * Exact Header from d4v1-sudo/Ps4-UI index.html lines 24-35:
 * #plus | #info + #textInfo | #friends (+5) | #user + #userName (Player) | #trophies (+6) | #clock
 */
@Composable
fun Ps4UiHeaderBar(
    sstFont: FontFamily,
    infoText: String,
    trophyCount: Int,
    onPlusClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onScanClick: () -> Unit
) {
    var clockText by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clockText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Group: PS Plus Icon + Info Icon + Animated News Ticker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1.1f)
        ) {
            // #plus
            AsyncImage(
                model = "file:///android_asset/ps4ui/img/plus.png",
                contentDescription = "PS Plus",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onPlusClick)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // #info
            AsyncImage(
                model = "file:///android_asset/ps4ui/img/info.png",
                contentDescription = "Info",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onScanClick)
            )

            // #textInfo
            Crossfade(targetState = infoText, label = "text_info") { msg ->
                Text(
                    text = msg,
                    color = Color.White,
                    fontFamily = sstFont,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right Group: #user + #userName | #trophies | #friends | #clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // #user + #userName
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onSettingsClick)
            ) {
                AsyncImage(
                    model = "file:///android_asset/ps4ui/img/user.png",
                    contentDescription = "User",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Player",
                    color = Color.White,
                    fontFamily = sstFont,
                    fontSize = 18.sp
                )
            }

            // #trophies
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onPlusClick)
            ) {
                AsyncImage(
                    model = "file:///android_asset/ps4ui/img/trophy.png",
                    contentDescription = "Trophies",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "+$trophyCount",
                    color = Color.White,
                    fontFamily = sstFont,
                    fontSize = 16.sp
                )
            }

            // #friends
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = "file:///android_asset/ps4ui/img/friends.png",
                    contentDescription = "Friends",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "+5",
                    color = Color.White,
                    fontFamily = sstFont,
                    fontSize = 16.sp
                )
            }

            // #clock
            Text(
                text = clockText,
                color = Color.White,
                fontFamily = sstFont,
                fontSize = 20.sp
            )
        }
    }
}

/**
 * Exact reproduction of .squareGame from main.css lines 180-222:
 * - Unfocused: 11em x 11em square, only .imgGame visible.
 * - Focused: expands to 15em width x 16.5em height with 3px solid white border,
 *   .imgGame takes 85% height, .gameText ("Start" / "Open" / "Shutdown") takes bottom 15% with rgba(255,255,255,.2),
 *   and .gameTitle appears directly to the right of the bottom bar!
 */
@Composable
fun Ps4UiSquareGameTile(
    item: GameUiModel,
    isFocused: Boolean,
    sstFont: FontFamily,
    onClick: () -> Unit
) {
    val tileWidth by animateDpAsState(
        targetValue = if (isFocused) 172.dp else 124.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "tile_w"
    )
    val tileHeight by animateDpAsState(
        targetValue = if (isFocused) 198.dp else 124.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "tile_h"
    )

    val isSmallIconTile = item.isSettingsTile || item.isShutdownTile

    Box(modifier = Modifier.wrapContentSize()) {
        Column(
            modifier = Modifier
                .width(tileWidth)
                .height(tileHeight)
                .then(
                    if (isFocused) Modifier.border(3.dp, Color.White)
                    else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            // .imgGame (85% height when focused, 100% when unfocused)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFocused) 0.84f else 1f)
                    .background(
                        if (isSmallIconTile || item.isPlayStationStore || item.isScanFolderTile)
                            Color(0xFF0A58A8).copy(alpha = 0.55f)
                        else Color(0xFF072A57)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.iconPath != null) {
                    AsyncImage(
                        model = item.iconPath,
                        contentDescription = item.title,
                        contentScale = if (isSmallIconTile) ContentScale.Fit else ContentScale.Crop,
                        modifier = if (isSmallIconTile) Modifier.fillMaxSize(0.45f) else Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = item.titleId,
                        color = Color.White,
                        fontFamily = sstFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // .gameText ("Start" / "Open" / "Shutdown" bar at bottom of focused tile)
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.16f)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.actionLabel,
                        color = Color.White,
                        fontFamily = sstFont,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // .gameTitle: positioned to the bottom-right side of the expanded tile (transform: translate(13.2em, -1.5em))
        if (isFocused) {
            Box(
                modifier = Modifier
                    .offset(x = tileWidth + 14.dp, y = tileHeight - 32.dp)
                    .widthIn(min = 240.dp, max = 520.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontFamily = sstFont,
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

/**
 * Exact reproduction of <div id="store"> sub-grid from index.html lines 43-52
 * showing God of War, Tomb Raider, Call of Duty Infinite Warfare, Mafia III, Destiny, Battlefield 1, Horizon, Persona 5.
 */
@Composable
fun Ps4UiStoreSubGrid(
    onStoreItemClick: (String) -> Unit
) {
    val storeItems = remember {
        listOf(
            "God of War" to "file:///android_asset/ps4ui/img/god.png",
            "Rise of the Tomb Raider" to "file:///android_asset/ps4ui/img/tomb.png",
            "Call of Duty: Infinite Warfare" to "file:///android_asset/ps4ui/img/if.png",
            "Mafia III" to "file:///android_asset/ps4ui/img/mafia.png",
            "Destiny: The Collection" to "file:///android_asset/ps4ui/img/destiny.png",
            "Battlefield 1" to "file:///android_asset/ps4ui/img/b1.png",
            "Horizon Zero Dawn" to "file:///android_asset/ps4ui/img/horizon.png",
            "Persona 5" to "file:///android_asset/ps4ui/img/per.png"
        )
    }

    LazyRow(
        contentPadding = PaddingValues(start = 242.dp, end = 48.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(storeItems) { _, (title, imgUri) ->
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.25f))
                    .clickable { onStoreItemClick(title) }
            ) {
                AsyncImage(
                    model = imgUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Exact reproduction of .notification-container from main.css lines 226-288 & script.js showNotification():
 * Dark #222 box, rounded 8px, pulsing #0091FF circle with icon.png, "Hello World!" / "First Achievement."
 */
@Composable
fun Ps4UiTrophyNotificationToast(sstFont: FontFamily) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trophy")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Ps4UiNotificationDark)
            .padding(16.dp)
            .widthIn(max = 360.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Ps4UiNotificationBlue),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "file:///android_asset/ps4ui/img/icon.png",
                contentDescription = "Trophy Icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "Hello World!",
                color = Color.White,
                fontFamily = sstFont,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "First Achievement.",
                color = Color(0xFFEEEEEE),
                fontFamily = sstFont,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Shared PS4 Controller Prompt helper used in DriverManagerScreen.
 */
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
