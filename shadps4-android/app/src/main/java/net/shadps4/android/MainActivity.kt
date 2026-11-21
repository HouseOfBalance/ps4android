package net.shadps4.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import net.shadps4.android.ui.*
import uniffi.shadps4.getInstalledDrivers
import uniffi.shadps4.initAndroidLogging
import uniffi.shadps4.installDriver
import uniffi.shadps4.scanGameDirectory
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val gamesList = mutableStateListOf<GameUiModel>()
    private val driversList = mutableStateListOf<DriverUiModel>()
    private var selectedDriverName by mutableStateOf("Default Adreno / System Vulkan")
    private var isPs4Unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initAndroidLogging()
            refreshDriversList()
        } catch (_: Throwable) {}

        // Seed exact games and cover art from d4v1-sudo/Ps4-UI index.html
        if (gamesList.isEmpty()) {
            gamesList.addAll(getDefaultPs4UiGames())
        }

        setContent {
            var currentScreen by remember { mutableStateOf("library") }

            BackHandler(enabled = currentScreen != "library") {
                currentScreen = "library"
            }

            // SAF Document Tree Folder Picker
            val openDocumentTreeLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                uri?.let {
                    try {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Throwable) {}

                    val resolvedPath = resolveSafTreeUriToFilePath(it)
                    Toast.makeText(this, "Scanning PS4 Directory: $resolvedPath", Toast.LENGTH_SHORT).show()

                    Thread {
                        val scanned = try {
                            scanGameDirectory(resolvedPath)
                        } catch (_: Throwable) {
                            emptyList()
                        }
                        runOnUiThread {
                            if (scanned.isNotEmpty()) {
                                // Prepend real scanned PS4 dumps right at the front of the carousel
                                val realGames = scanned.map { item -> item.toUiModel() }
                                gamesList.clear()
                                gamesList.addAll(realGames)
                                gamesList.addAll(getDefaultPs4UiGames())
                                Toast.makeText(
                                    this,
                                    "Loaded ${realGames.size} PS4 Game(s)!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "No param.sfo found in $resolvedPath",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.start()
                }
            }

            // Driver ZIP File Picker
            val openZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    Toast.makeText(this, "Installing Turnip Driver Package...", Toast.LENGTH_SHORT).show()
                    Thread {
                        val tempZip = File(cacheDir, "temp_driver.zip")
                        try {
                            contentResolver.openInputStream(it)?.use { input ->
                                FileOutputStream(tempZip).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            val ok = installDriver(filesDir.absolutePath, tempZip.absolutePath)
                            runOnUiThread {
                                if (ok) {
                                    refreshDriversList()
                                    Toast.makeText(this, "Turnip Driver Installed!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Invalid Driver ZIP (missing meta.json)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Throwable) {
                            runOnUiThread {
                                Toast.makeText(this, "Driver install error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            tempZip.delete()
                        }
                    }.start()
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                if (currentScreen == "drivers") {
                    DriverManagerScreen(
                        drivers = driversList,
                        onSelectDriver = { clickedDriver ->
                            val updated = driversList.map { d ->
                                d.copy(isSelected = d.name == clickedDriver.name && clickedDriver.isSelected.not())
                            }
                            driversList.clear()
                            driversList.addAll(updated)
                            selectedDriverName = driversList.firstOrNull { it.isSelected }?.name
                                ?: "Default Adreno / System Vulkan"
                        },
                        onInstallZipClick = { openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        onBackClick = { currentScreen = "library" }
                    )
                } else {
                    GameLibraryScreen(
                        games = gamesList,
                        activeDriverName = selectedDriverName,
                        isUnlockedInitial = isPs4Unlocked,
                        onUnlockScreen = { isPs4Unlocked = true },
                        onGameClick = { game -> launchEmulator(game) },
                        onPickDirectoryClick = { openDocumentTreeLauncher.launch(null) },
                        onDriverSettingsClick = { currentScreen = "drivers" },
                        onShutdownClick = { isPs4Unlocked = false }
                    )
                }
            }
        }
    }

    /**
     * Exact 11 Game & App tiles from d4v1-sudo/Ps4-UI index.html lines 63-138
     * using the authentic PNG assets bundled in assets/ps4ui/img/.
     */
    private fun getDefaultPs4UiGames(): List<GameUiModel> = listOf(
        GameUiModel(
            titleId = "CUSA07820",
            title = "The Last Of Us Part II",
            appVersion = "01.09",
            iconPath = "file:///android_asset/ps4ui/img/last.png",
            folderPath = "/sdcard/PS4/CUSA07820"
        ),
        GameUiModel(
            titleId = "CUSA08344",
            title = "Detroit: Become Human",
            appVersion = "01.08",
            iconPath = "file:///android_asset/ps4ui/img/detroit.png",
            folderPath = "/sdcard/PS4/CUSA08344"
        ),
        GameUiModel(
            titleId = "CUSA02299",
            title = "Spider-Man",
            appVersion = "01.19",
            iconPath = "file:///android_asset/ps4ui/img/spider.png",
            folderPath = "/sdcard/PS4/CUSA02299"
        ),
        GameUiModel(
            titleId = "CUSA02429",
            title = "Battlefield 1",
            appVersion = "01.15",
            iconPath = "file:///android_asset/ps4ui/img/battle.png",
            folderPath = "/sdcard/PS4/CUSA02429"
        ),
        GameUiModel(
            titleId = "CUSA11260",
            title = "Death Stranding",
            appVersion = "01.12",
            iconPath = "file:///android_asset/ps4ui/img/death.png",
            folderPath = "/sdcard/PS4/CUSA11260"
        ),
        GameUiModel(
            titleId = "CUSA03041",
            title = "Red Dead Redemption 2",
            appVersion = "01.31",
            iconPath = "file:///android_asset/ps4ui/img/red.png",
            folderPath = "/sdcard/PS4/CUSA03041"
        ),
        GameUiModel(
            titleId = "CUSA28863",
            title = "Elden Ring",
            appVersion = "01.10",
            iconPath = "file:///android_asset/ps4ui/img/elden.png",
            folderPath = "/sdcard/PS4/CUSA28863"
        ),
        GameUiModel(
            titleId = "CUSA02624",
            title = "Call Of Duty: Black Ops III",
            appVersion = "01.33",
            iconPath = "file:///android_asset/ps4ui/img/cod.png",
            folderPath = "/sdcard/PS4/CUSA02624"
        ),
        GameUiModel(
            titleId = "CUSA12539",
            title = "Jedi: Fallen Order",
            appVersion = "01.12",
            iconPath = "file:///android_asset/ps4ui/img/jedi.png",
            folderPath = "/sdcard/PS4/CUSA12539"
        ),
        GameUiModel(
            titleId = "CUSA31873",
            title = "Fifa 23",
            appVersion = "01.21",
            iconPath = "file:///android_asset/ps4ui/img/fifa.png",
            folderPath = "/sdcard/PS4/CUSA31873"
        ),
        GameUiModel(
            titleId = "CUSA01015",
            title = "Youtube",
            appVersion = "02.05",
            iconPath = "file:///android_asset/ps4ui/img/yt.png",
            folderPath = "/sdcard/PS4/CUSA01015",
            actionLabel = "Open"
        )
    )

    private fun refreshDriversList() {
        try {
            val installed = getInstalledDrivers(filesDir.absolutePath)
            driversList.clear()
            driversList.addAll(installed.mapIndexed { idx, item -> item.toUiModel(isSelected = idx == 0) })
            driversList.firstOrNull { it.isSelected }?.let {
                selectedDriverName = it.name
            }
        } catch (_: Throwable) {}
    }

    private fun resolveSafTreeUriToFilePath(uri: Uri): String {
        val rawPath = uri.path ?: return "/sdcard"
        return if (rawPath.contains("primary:")) {
            val rel = rawPath.substringAfter("primary:")
            "/storage/emulated/0/$rel"
        } else {
            rawPath
        }
    }

    private fun launchEmulator(game: GameUiModel) {
        val intent = Intent(this, EmulatorActivity::class.java).apply {
            putExtra("EXTRA_GAME_PATH", game.folderPath)
            putExtra("EXTRA_TITLE_ID", game.titleId)
            putExtra("EXTRA_TITLE_NAME", game.title)
        }
        startActivity(intent)
    }

    companion object {
        init {
            try {
                System.loadLibrary("shadps4_emu_host")
            } catch (_: UnsatisfiedLinkError) {}
        }
    }
}
