package net.shadps4.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uniffi.shadps4.DriverItem

data class DriverUiModel(
    val name: String,
    val description: String,
    val author: String,
    val driverVersion: String,
    val isSelected: Boolean
)

fun DriverItem.toUiModel(isSelected: Boolean = false): DriverUiModel = DriverUiModel(
    name = this.name,
    description = this.description,
    author = this.author,
    driverVersion = this.driverVersion,
    isSelected = isSelected
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverManagerScreen(
    drivers: List<DriverUiModel>,
    onSelectDriver: (DriverUiModel) -> Unit,
    onInstallZipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPU Driver Manager (Turnip / Mesa)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    Button(onClick = onInstallZipClick) {
                        Text("Install .ZIP")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mesa Turnip Drivers (Adreno 7xx/8xx)", fontWeight = FontWeight.Bold)
                        Text(
                            "Custom drivers provide proper Vulkan extensions (GPL, custom border color) and resolve shader compiler bugs on Snapdragon.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(drivers) { driver ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDriver(driver) },
                    colors = if (driver.isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    else CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(driver.name, fontWeight = FontWeight.Bold)
                            Text("Version: ${driver.driverVersion} • By ${driver.author}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(driver.description, style = MaterialTheme.typography.bodySmall)
                        }
                        RadioButton(
                            selected = driver.isSelected,
                            onClick = { onSelectDriver(driver) }
                        )
                    }
                }
            }
        }
    }
}
