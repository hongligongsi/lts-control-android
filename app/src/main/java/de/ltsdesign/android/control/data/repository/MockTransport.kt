package de.ltsdesign.android.control.data.repository

import de.ltsdesign.android.control.data.ble.BleTransport
import de.ltsdesign.android.control.data.ble.TransportEvent
import de.ltsdesign.android.control.data.model.ConnectionStatus
import de.ltsdesign.android.control.data.model.DeviceRunState
import de.ltsdesign.android.control.data.model.RespoolerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

/**
 * Mock transport - 假数据,UI Demo 用
 *
 * 行为:
 * - 启动时 disconnected
 * - 用户按 "Connect" 模拟 1.5s 后变 connected,带温度/风扇读数
 * - 用户按 Start, 模拟加热曲线,每 200ms 升温一档,2 分钟到目标温度
 * - 用户按 Stop/ Pause 立即停下
 *
 * 真机版只需替换为 [LtsBleTransport]
 */
class MockTransport : BleTransport {

    private val _state = MutableStateFlow(RespoolerState())
    override val state: StateFlow<RespoolerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: kotlinx.coroutines.Job? = null

    override suspend fun connect(deviceAddress: String) {
        // 模拟配对 + 协商
        _state.update { it.copy(connection = ConnectionStatus.CONNECTING) }
        delay(800)
        _state.update {
            it.copy(
                connection = ConnectionStatus.CONNECTED,
                deviceName = "LTS-Respooler-Pro-$deviceAddress",
                firmwareVersion = "1.2.1",
                chipTempC = 32.4f,
                chamberTempC = 24.8f,
                filamentLoaded = true,
                filamentType = "PLA",
                coolingFanOn = false,
                runState = DeviceRunState.IDLE,
                progress = 0f,
            )
        }
        _events.emit(TransportEvent.Connected("LTS-Respooler-Pro", "1.2.1"))
    }

    override suspend fun disconnect() {
        simulationJob?.cancel()
        _state.update {
            RespoolerState(
                connection = ConnectionStatus.DISCONNECTED,
                targetTempC = it.targetTempC,
            )
        }
        _events.emit(TransportEvent.Disconnected)
    }

    override suspend fun start() {
        if (_state.value.connection != ConnectionStatus.CONNECTED) return
        _state.update { it.copy(runState = DeviceRunState.RUNNING, progress = 0f) }
        _events.emit(TransportEvent.Started)
        simulateRun()
    }

    override suspend fun stop() {
        simulationJob?.cancel()
        _state.update {
            it.copy(
                runState = DeviceRunState.IDLE,
                progress = 0f,
                coolingFanOn = false,
            )
        }
        _events.emit(TransportEvent.Stopped)
    }

    override suspend fun pause() {
        if (_state.value.runState != DeviceRunState.RUNNING) return
        simulationJob?.cancel()
        _state.update { it.copy(runState = DeviceRunState.PAUSED) }
        _events.emit(TransportEvent.Paused)
    }

    override suspend fun resume() {
        if (_state.value.runState != DeviceRunState.PAUSED) return
        _state.update { it.copy(runState = DeviceRunState.RUNNING) }
        _events.emit(TransportEvent.Resumed)
        simulateRun()
    }

    override suspend fun setTargetTemp(celsius: Float) {
        _state.update { it.copy(targetTempC = celsius) }
    }

    override suspend fun setFanOn(on: Boolean) {
        _state.update { it.copy(coolingFanOn = on) }
    }

    private fun simulateRun() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val target = _state.value.targetTempC
            val startTemp = _state.value.chamberTempC ?: 24f
            val totalMillis = 90_000L  // 90s 模拟完整周期
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = min(1f, elapsed.toFloat() / totalMillis)
                val chamber = startTemp + (target - startTemp) * easeInOut(fraction)
                val chip = chamber + 8f + Random.nextFloat() * 2f
                _state.update {
                    it.copy(
                        chamberTempC = chamber,
                        chipTempC = chip,
                        progress = fraction,
                        coolingFanOn = chamber > target - 2f,
                    )
                }
                _events.emit(TransportEvent.Progress(fraction))
                if (fraction >= 1f) break
                delay(200L)
            }
        }
    }

    private fun easeInOut(t: Float): Float =
        if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
}
