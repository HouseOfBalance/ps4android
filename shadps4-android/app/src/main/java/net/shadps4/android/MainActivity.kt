package net.shadps4.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import net.shadps4.android.ui.*

class MainActivity : ComponentActivity() {

    private val gamesList = mutableStateListOf<GameUiModel>()
    private val driversList = mutableStateListOf<DriverUiModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initAndroidLogging()
        } catch (_: Throwable) {}

        setContent {
            var currentScreen by remember { mutableStateOf("library") }

            // SAF Document Tree Folder Picker
            val openDocumentTreeLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri: Uri? ->
                uri?.let {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Toast.makeText(this, "Scanning: ${it.path}", Toast.LENGTH_SHORT).show()
                    // Background thread scanning
                    Thread {
                        val path = it.path ?: return@Thread
                        val scanned = scanGameDirectory(path)
                        runOnUiThread {
                            gamesList.clear()
                            gamesList.addAll(scanned.map { item -> item.toUiModel() })
                        }
                    }.start()
                }
            }

            // Driver ZIP File Picker
            val openZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    Toast.makeText(this, "Installing driver ZIP...", Toast.LENGTH_SHORT).show()
                }
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                if (currentScreen == "drivers") {
                    DriverManagerScreen(
                        drivers = driversList,
                        onSelectDriver = { /* Select driver */ },
                        onInstallZipClick = { openZipLauncher.launch(arrayOf("application/zip")) },
                        onBackClick = { currentScreen = "library" }
                    )
                } else {
                    GameLibraryScreen(
                        games = gamesList,
                        onGameClick = { game -> launchEmulator(game) },
                        onPickDirectoryClick = { openDocumentTreeLauncher.launch(null) },
                        onDriverSettingsClick = { currentScreen = "drivers" }
                    )
                }
            }
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
}
