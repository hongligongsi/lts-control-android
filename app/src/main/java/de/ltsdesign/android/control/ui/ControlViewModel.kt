package de.ltsdesign.android.control.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.ltsdesign.android.control.data.ble.BleTransport
import de.ltsdesign.android.control.data.ble.TransportEvent
import de.ltsdesign.android.control.data.model.RespoolerState
import de.ltsdesign.android.control.data.repository.MockTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 全局 ViewModel:跨 Tab 共享设备状态
 *
 * - 持有 [transport](默认 Mock,可在 [LtsBleTransport] 之间切换)
 * - 暴露 [state] 状态流
 * - 暴露控制方法(connect/start/stop/...)
 */
class ControlViewModel(app: Application) : AndroidViewModel(app) {

    // 切换这里 = Mock vs 真机
    private val transport: BleTransport = MockTransport()

    val state: StateFlow<RespoolerState> = transport.state

    private val _events = MutableStateFlow<List<TransportEvent>>(emptyList())
    val events: StateFlow<List<TransportEvent>> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            transport.events.collect { ev ->
                _events.value = (_events.value + ev).takeLast(50)
            }
        }
    }

    // -------- 控制命令 --------
    fun connect(address: String = "AA:BB:CC:11:22:33") = viewModelScope.launch {
        transport.connect(address)
    }

    fun disconnect() = viewModelScope.launch { transport.disconnect() }
    fun start() = viewModelScope.launch { transport.start() }
    fun stop() = viewModelScope.launch { transport.stop() }
    fun pause() = viewModelScope.launch { transport.pause() }
    fun resume() = viewModelScope.launch { transport.resume() }
    fun setTargetTemp(c: Float) = viewModelScope.launch { transport.setTargetTemp(c) }
    fun setFanOn(on: Boolean) = viewModelScope.launch { transport.setFanOn(on) }
}
