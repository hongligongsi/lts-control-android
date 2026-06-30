package de.ltsdesign.android.control.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.ltsdesign.android.control.R
import de.ltsdesign.android.control.i18n.AppViewModel
import de.ltsdesign.android.control.i18n.LocaleHelper

/**
 * Settings — 按 LTS Web Control index.html 的分组顺序排版
 *
 * ── 速度 / LED / 转账金额 / 耗材传感器 / 完成音效 ──
 * ───────────────── (pro 分隔) ─────────────────
 * 伺服校准 / 步距 / 主场位置   ← Pro 板才能用
 * ───────────────────────────────────────────────
 * ── 运动能力 / 转向 / 高速 / 自动停止灵敏度 ──
 * ───────────────── (分割) ─────────────────
 * ── 风扇转速 / 耗材永远在线 ──
 * ───────────────────────────────────────────
 * ── 参考时间(stepper) ──
 *
 * BLE 字段:SPD / LED / WGT / USE_FIL / JIN / POW / DIR / HS / TRQ / FAN_SPD / FAN_ALW / DUR
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val appVm: AppViewModel = viewModel()
    val ctx = LocalContext.current
    fun applyLang(idx: Int) {
        val tag = LocaleHelper.supportedTags()[idx]
        appVm.setLanguage(tag)
        // recreate Activity 使所有 stringResource 刷新
        (ctx as? android.app.Activity)?.recreate()
    }
    // langIdx 初始 = 当前 locale
    var langIdx by rememberSaveable { mutableIntStateOf(
        LocaleHelper.supportedTags().indexOf(appVm.langTag.value).coerceAtLeast(0)
    ) }
    // 一级:速度 / LED / 转账金额 / 耗材 / 音效
    var speed by remember { mutableIntStateOf(85) }              // 50~100
    var led by remember { mutableIntStateOf(50) }                // 0~100,step 10
    var transferAmount by remember { mutableIntStateOf(0) }       // 0=整卷,1=1kg,2=0.5kg,3=0.25kg
    var useFilament by remember { mutableStateOf(true) }
    var jingle by remember { mutableIntStateOf(1) }               // 0=离开,1=简单,2=滑音,3=星战

    // 二级:伺服 pro)
    var servoStepMmStr by remember { mutableStateOf("1.75") }     // mm
    var servoHome by remember { mutableStateOf("R") }             // R|L

    // 三级:电机 / 转向 / 高速 / 自动停止
    var motorPower by remember { mutableIntStateOf(100) }          // 80~120
    var direction by remember { mutableStateOf(false) }
    var highSpeed by remember { mutableStateOf(false) }
    var autoStop by remember { mutableIntStateOf(0) }             // 0=离开,1=低,2=中,3=高

    // 四级:风扇
    var fanSpeed by remember { mutableIntStateOf(60) }             // 10~100,step 10
    var fanAlwaysOn by remember { mutableStateOf(false) }

    // 五级:参考时长(分 + 秒)
    var durMinutes by remember { mutableIntStateOf(14) }
    var durSeconds by remember { mutableIntStateOf(55) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // ── 语言切换(顶部,优先可见) ──
        StepperRow(
            label = stringResource(R.string.settings_language),
            valueText = langLabel(ctx, langIdx),
            onMinus = {
                val next = (langIdx - 1).coerceAtLeast(0)
                if (next != langIdx) { langIdx = next; applyLang(next) }
            },
            onPlus = {
                val next = (langIdx + 1).coerceAtMost(LocaleHelper.supportedTags().size - 1)
                if (next != langIdx) { langIdx = next; applyLang(next) }
            },
        )

        // ── 一级:5 个控件(速度+LED 转账金额 耗材传感器 完成音效) ──
        SliderRow(
            label = stringResource(R.string.settings_speed),
            value = speed,
            onChange = { speed = it },
            valueRange = 50..100,
            valueText = stringResource(R.string.settings_default_speed, speed),
        )

        SliderRow(
            label = stringResource(R.string.settings_led),
            value = led,
            onChange = { led = it },
            valueRange = 0..100,
            step = 10,
            valueText = led.toString(),
        )

        IntSelectRow(
            label = stringResource(R.string.settings_transfer_amount),
            value = transferAmount,
            values = listOf(0, 1, 2, 3),
            labels = listOf(
                stringResource(R.string.transfer_entire_spool),
                stringResource(R.string.transfer_1kg),
                stringResource(R.string.transfer_500g),
                stringResource(R.string.transfer_250g),
            ),
            onChange = { transferAmount = it },
        )

        SwitchRow(
            label = stringResource(R.string.settings_filament_sensor),
            checked = useFilament,
            onCheckedChange = { useFilament = it },
        )

        IntSelectRow(
            label = stringResource(R.string.settings_completion_sound),
            value = jingle,
            values = listOf(0, 1, 2, 3),
            labels = listOf(
                stringResource(R.string.sound_off),
                stringResource(R.string.sound_simple),
                stringResource(R.string.sound_gliss),
                stringResource(R.string.sound_starwars),
            ),
            onChange = { jingle = it },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── 二级:伺服(pro) ──
        // LTS HTML 显示但禁用(is-disabled),要给 Pro 板开可交互。我先以一个按钮 + 提示走一遍
        ActionRow(
            label = stringResource(R.string.settings_servo_calibration),
            suffix = stringResource(R.string.settings_pro_only),
            onClick = { /* TODO: 弹出 ServoCalibrationModal */ },
        )

        StepperRow(
            label = stringResource(R.string.settings_step_distance),
            valueText = stringResource(R.string.settings_default_step, servoStepMmStr),
            onMinus = { /* TODO */ },
            onPlus = { /* TODO */ },
        )

        IntSelectRow(
            label = stringResource(R.string.settings_home_position),
            value = if (servoHome == "R") 0 else 1,
            values = listOf(0, 1),
            labels = listOf(stringResource(R.string.home_right), stringResource(R.string.home_left)),
            onChange = { servoHome = if (it == 0) "R" else "L" },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── 三级:电机 / 转向 / 高速 / 自动停止灵敏度 ──
        StepperRow(
            label = stringResource(R.string.settings_motor_strength),
            valueText = stringResource(R.string.settings_default_motor, motorPower),
            onMinus = { if (motorPower > 80) motorPower -= 5 },
            onPlus = { if (motorPower < 120) motorPower += 5 },
        )

        SwitchRow(
            label = stringResource(R.string.settings_direction),
            checked = direction,
            onCheckedChange = { direction = it },
        )

        SwitchRow(
            label = stringResource(R.string.settings_high_speed),
            checked = highSpeed,
            onCheckedChange = { highSpeed = it },
        )

        IntSelectRow(
            label = stringResource(R.string.settings_autostop),
            value = autoStop,
            values = listOf(0, 1, 2, 3),
            labels = listOf(
                stringResource(R.string.autostop_off),
                stringResource(R.string.autostop_low),
                stringResource(R.string.autostop_medium),
                stringResource(R.string.autostop_high),
            ),
            onChange = { autoStop = it },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── 四级:风扇 ──
        SliderRow(
            label = stringResource(R.string.settings_fan_speed),
            value = fanSpeed,
            onChange = { fanSpeed = it },
            valueRange = 10..100,
            step = 10,
            valueText = stringResource(R.string.settings_default_fan, fanSpeed),
        )

        SwitchRow(
            label = stringResource(R.string.settings_fan_always_on),
            checked = fanAlwaysOn,
            onCheckedChange = { fanAlwaysOn = it },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── 五级:参考时间 ──
        StepperRow(
            label = stringResource(R.string.settings_reference_time),
            valueText = stringResource(R.string.settings_default_duration, durMinutes, durSeconds),
            onMinus = {
                if (durSeconds >= 5) durSeconds -= 5 else if (durMinutes > 0) { durMinutes--; durSeconds = 55 }
            },
            onPlus = {
                if (durSeconds < 55) durSeconds += 5 else if (durMinutes < 60) { durMinutes++; durSeconds = 0 }
            },
        )
    }
}

// ────── 通用控件 ──────

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int = 1,
    valueText: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(((it.toInt() / step) * step).coerceIn(valueRange)) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = ((valueRange.last - valueRange.first) / step) - 1,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(label: String, suffix: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("…")
        }
    }
}

@Composable
private fun StepperRow(label: String, valueText: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onMinus,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        OutlinedButton(
            onClick = onPlus,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun langLabel(ctx: android.content.Context, idx: Int): String {
    return when (idx) {
        0 -> ctx.getString(R.string.lang_system)
        1 -> ctx.getString(R.string.lang_en)
        2 -> ctx.getString(R.string.lang_zh)
        else -> ""
    }
}

@Composable
private fun IntSelectRow(
    label: String,
    value: Int,
    values: List<Int>,
    labels: List<String>,
    onChange: (Int) -> Unit,
) {
    val idx = values.indexOf(value).let { if (it < 0) 0 else it }
    val onMinus = { onChange(values[(idx - 1).coerceAtLeast(0)]) }
    val onPlus = { onChange(values[(idx + 1).coerceAtMost(values.size - 1)]) }
    StepperRow(
        label = label,
        valueText = labels.getOrNull(idx) ?: "",
        onMinus = onMinus,
        onPlus = onPlus,
    )
}