package net.shadps4.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun DriverManagerScreen(
    drivers: List<DriverUiModel>,
    onSelectDriver: (DriverUiModel) -> Unit,
    onInstallZipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF011640),
                        Color(0xFF002E7A),
                        Color(0xFF011233)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // PS4 Settings Header Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("⚙️", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Settings  ›  Graphics & Custom GPU Drivers",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Adreno Turnip / Mesa Vulkan Loader",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }

                    // Install .ZIP Button (PS4 System Style)
                    Button(
                        onClick = onInstallZipClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ps4CyanGlow.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.border(1.dp, Ps4CyanGlow, RoundedCornerShape(3.dp))
                    ) {
                        Text(
                            text = "＋ Install Driver Package (.ZIP)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
            }

            // Center List: PS4 System Settings Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Default System Driver Option
                item(key = "sys_default_driver") {
                    val anyCustomSelected = drivers.any { it.isSelected }
                    Ps4DriverSettingRow(
                        title = "System Default Vulkan Driver (Qualcomm / OEM)",
                        subtitle = "Built-in Android hardware driver • Recommended for Adreno 610 (Snapdragon 685) stability check",
                        version = "Native Bionic",
                        isSelected = !anyCustomSelected,
                        onClick = {
                            // Deselect custom drivers
                            drivers.firstOrNull { it.isSelected }?.let { onSelectDriver(it.copy(isSelected = false)) }
                        }
                    )
                }

                items(
                    items = drivers,
                    key = { it.name }
                ) { driver ->
                    Ps4DriverSettingRow(
                        title = driver.name,
                        subtitle = "${driver.description}  •  Author: ${driver.author}",
                        version = "Vulkan ${driver.driverVersion}",
                        isSelected = driver.isSelected,
                        onClick = { onSelectDriver(driver) }
                    )
                }
            }

            // Bottom PS4 Controller Legend
            Column {
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "libadrenotools isolated namespace  •  Supports Turnip KGSL & BCn Texture Decompression",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Ps4ControllerPrompt(
                            symbol = "✕",
                            symbolColor = Color(0xFF64B5F6),
                            label = "Apply Driver",
                            onClick = {}
                        )
                        Ps4ControllerPrompt(
                            symbol = "○",
                            symbolColor = Color(0xFFEF5350),
                            label = "Back",
                            onClick = onBackClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Ps4DriverSettingRow(
    title: String,
    subtitle: String,
    version: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.16f)
                else Color.Black.copy(alpha = 0.22f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Active status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Ps4CyanGlow else Color.White.copy(alpha = 0.2f))
            )

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(if (isSelected) Ps4CyanGlow.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isSelected) "ACTIVE • $version" else version,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
