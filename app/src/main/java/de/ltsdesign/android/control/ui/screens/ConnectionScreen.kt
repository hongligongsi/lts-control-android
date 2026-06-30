package de.ltsdesign.android.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ltsdesign.android.control.R

/**
 * Connection - 设备扫描/配对(占位 UI,真实 BLE 接入需用户授权 + 扫描)
 */
data class PairedDevice(
    val address: String,
    val name: String,
    val firmware: String,
    val isCurrentlyConnected: Boolean = false,
)

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    onConnect: (PairedDevice) -> Unit = {},
    onUnpair: (PairedDevice) -> Unit = {},
) {
    // Mock 已配对设备列表(iOS LTS Control 应该会自动同步)
    val devices = listOf(
        PairedDevice(
            address = "AA:BB:CC:11:22:33",
            name = "LTS-Respooler-Pro",
            firmware = "1.2.1",
            isCurrentlyConnected = true,
        ),
        PairedDevice(
            address = "AA:BB:CC:44:55:66",
            name = "LTS-Respooler-Lab",
            firmware = "1.1.9",
            isCurrentlyConnected = false,
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.connection_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { /* TODO: re-scan */ }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.connection_scan))
            }
        }

        Button(
            onClick = { /* TODO: start BLE scan */ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null)
            Box(Modifier.padding(horizontal = 6.dp)) {
                Text(stringResource(R.string.connection_scan))
            }
        }

        Text(
            text = stringResource(R.string.connection_ble_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.connection_paired),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(devices) { device ->
                DeviceRow(
                    device = device,
                    onConnect = { onConnect(device) },
                    onUnpair = { onUnpair(device) },
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: PairedDevice,
    onConnect: () -> Unit,
    onUnpair: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${device.address} · FW ${device.firmware}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (device.isCurrentlyConnected) {
                Text(
                    text = stringResource(R.string.value_connected),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                TextButton(onClick = onConnect) {
                    Text(stringResource(R.string.btn_connect))
                }
            }
            TextButton(onClick = onUnpair) {
                Text(stringResource(R.string.connection_unpair))
            }
        }
    }
}
