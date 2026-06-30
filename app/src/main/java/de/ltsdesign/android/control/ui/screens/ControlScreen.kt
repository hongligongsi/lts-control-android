package de.ltsdesign.android.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ltsdesign.android.control.R
import de.ltsdesign.android.control.data.model.ConnectionStatus
import de.ltsdesign.android.control.data.model.DeviceRunState
import de.ltsdesign.android.control.data.model.RespoolerState
import de.ltsdesign.android.control.ui.ControlViewModel
import de.ltsdesign.android.control.ui.components.AnimatedDeviceIllustration
import de.ltsdesign.android.control.ui.components.StatusCard
import de.ltsdesign.android.control.ui.components.StatusColors

@Composable
fun ControlScreen(
    vm: ControlViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatusGrid(state) }
        item { DevicePanel(state, vm) }
        item { ActionButtons(state, vm) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatusGrid(state: RespoolerState) {
    val connIcon = when (state.connection) {
        ConnectionStatus.CONNECTED -> Icons.Filled.CheckCircle
        ConnectionStatus.CONNECTING -> Icons.Filled.HourglassEmpty
        ConnectionStatus.DISCONNECTED -> Icons.Filled.WifiOff
        ConnectionStatus.ERROR -> Icons.Filled.ErrorOutline
    }
    val connValue = when (state.connection) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.value_connected)
        ConnectionStatus.CONNECTING -> stringResource(R.string.error_connecting)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.value_disconnected)
        ConnectionStatus.ERROR -> state.errorMessage ?: stringResource(R.string.error_unknown)
    }
    val connColor = when (state.connection) {
        ConnectionStatus.CONNECTED -> StatusColors.Connected
        ConnectionStatus.CONNECTING -> StatusColors.Connecting
        ConnectionStatus.DISCONNECTED -> StatusColors.Disconnected
        ConnectionStatus.ERROR -> StatusColors.Error
    }

    val tempValue = when {
        state.chipTempC == null -> stringResource(R.string.value_unknown)
        else -> stringResource(R.string.format_celsius, state.chamberTempC ?: state.chipTempC ?: 0f)
    }
    val tempSubtitle = if (state.chipTempC != null) {
        stringResource(R.string.format_chip_temp, state.chipTempC)
    } else null

    val filamentValue = if (state.filamentLoaded) {
        state.filamentType ?: stringResource(R.string.value_filament_detected)
    } else {
        stringResource(R.string.value_filament_none)
    }
    val filamentColor = if (state.filamentLoaded) StatusColors.Connected else StatusColors.Disconnected

    val fanValue = if (state.coolingFanOn) stringResource(R.string.value_fan_on) else stringResource(R.string.value_fan_off)
    val fanColor = if (state.coolingFanOn) StatusColors.Running else StatusColors.Disconnected

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 上排:Connection + Temperature
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(
                icon = connIcon,
                title = stringResource(R.string.status_connection),
                value = connValue,
                statusColor = connColor,
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                icon = Icons.Filled.Thermostat,
                title = stringResource(R.string.status_temperature),
                value = tempValue,
                statusColor = if (state.chipTempC != null) StatusColors.Running else StatusColors.Disconnected,
                subtitle = tempSubtitle,
                modifier = Modifier.weight(1f),
            )
        }
        // 下排:Filament + Cooling Fan
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(
                icon = Icons.Filled.SettingsRemote,
                title = stringResource(R.string.status_filament),
                value = filamentValue,
                statusColor = filamentColor,
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                icon = Icons.Filled.Air,
                title = stringResource(R.string.status_cooling_fan),
                value = fanValue,
                statusColor = fanColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DevicePanel(state: RespoolerState, vm: ControlViewModel) {
    val spinning = state.runState == DeviceRunState.RUNNING
    val connected = state.connection == ConnectionStatus.CONNECTED

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedDeviceIllustration(
                size = 220.dp,
                spinning = spinning,
                progress = state.progress,
            )
            Text(
                text = state.deviceName
                    ?: stringResource(R.string.value_disconnected),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when {
                    state.firmwareVersion != null -> stringResource(R.string.label_firmware, state.firmwareVersion)
                    else -> stringResource(R.string.value_unknown)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 连接/重连按钮
            if (!connected) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { vm.connect() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Filled.SettingsRemote, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.btn_connect))
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(state: RespoolerState, vm: ControlViewModel) {
    val connected = state.connection == ConnectionStatus.CONNECTED
    val canStart = connected && state.runState == DeviceRunState.IDLE
    val canStop = connected && state.runState == DeviceRunState.RUNNING
    val canPause = connected && state.runState == DeviceRunState.RUNNING
    val canResume = connected && state.runState == DeviceRunState.PAUSED

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Start / Resume
        val startEnabled = canStart || canResume
        val startLabel = if (canResume) stringResource(R.string.btn_resume) else stringResource(R.string.btn_start)
        Button(
            onClick = {
                when (state.runState) {
                    DeviceRunState.IDLE -> vm.start()
                    DeviceRunState.PAUSED -> vm.resume()
                    else -> {}
                }
            },
            enabled = startEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(startLabel)
        }

        // Stop / Pause
        val stopLabel = if (canPause) stringResource(R.string.btn_pause) else stringResource(R.string.btn_stop)
        val stopIcon = if (canPause) Icons.Filled.HourglassEmpty else Icons.Filled.Stop
        OutlinedButton(
            onClick = {
                when (state.runState) {
                    DeviceRunState.RUNNING -> if (canPause) vm.pause() else vm.stop()
                    DeviceRunState.PAUSED -> vm.stop()
                    else -> {}
                }
            },
            enabled = canStop || canPause,
            modifier = Modifier.weight(1f),
        ) {
            Icon(stopIcon, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(stopLabel)
        }
    }

    // 进度条
    if (state.runState == DeviceRunState.RUNNING || state.runState == DeviceRunState.PAUSED) {
        Spacer(Modifier.height(8.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.label_progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.format_percent, (state.progress * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            // 简单进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }
    }
}
