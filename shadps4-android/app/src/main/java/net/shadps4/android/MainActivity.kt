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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initAndroidLogging()
            refreshDriversList()
        } catch (_: Throwable) {}

        // Seed showcase PS4 titles when library is empty so the PS4 Orbis Home Screen looks authentic out-of-the-box
        if (gamesList.isEmpty()) {
            gamesList.addAll(
                listOf(
                    GameUiModel(
                        titleId = "CUSA00900",
                        title = "Bloodborne™",
                        appVersion = "01.09",
                        iconPath = null,
                        folderPath = "/sdcard/PS4/CUSA00900",
                        category = "PS4™ Game"
                    ),
                    GameUiModel(
                        titleId = "CUSA03173",
                        title = "Bloodborne™: The Old Hunters",
                        appVersion = "01.09",
                        iconPath = null,
                        folderPath = "/sdcard/PS4/CUSA03173",
                        category = "PS4™ Expansion"
                    ),
                    GameUiModel(
                        titleId = "CUSA00572",
                        title = "Driveclub™",
                        appVersion = "01.28",
                        iconPath = null,
                        folderPath = "/sdcard/PS4/CUSA00572",
                        category = "PS4™ Game"
                    ),
                    GameUiModel(
                        titleId = "CUSA01130",
                        title = "Gravity Rush™ Remastered",
                        appVersion = "01.00",
                        iconPath = null,
                        folderPath = "/sdcard/PS4/CUSA01130",
                        category = "PS4™ Game"
                    )
                )
            )
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
                                gamesList.clear()
                                gamesList.addAll(scanned.map { item -> item.toUiModel() })
                            } else {
                                Toast.makeText(
                                    this,
                                    "No param.sfo found in $resolvedPath (kept demo titles)",
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
                        onGameClick = { game -> launchEmulator(game) },
                        onPickDirectoryClick = { openDocumentTreeLauncher.launch(null) },
                        onDriverSettingsClick = { currentScreen = "drivers" }
                    )
                }
            }
        }
    }

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
        // Typical SAF tree path: /tree/primary:PS4Games -> /storage/emulated/0/PS4Games
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
