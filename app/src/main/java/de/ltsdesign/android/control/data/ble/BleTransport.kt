package de.ltsdesign.android.control.data.ble

import de.ltsdesign.android.control.data.model.RespoolerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 设备传输层抽象(UI 与具体协议解耦)
 *
 * - [MockTransport]: 假数据,UI Demo 用
 * - [LtsBleTransport]: 真 BLE(占位,未来实现)
 *
 * 设计: 任何 transport 都对外暴露同样的 StateFlow<RespoolerState> + 控制命令 suspend fn
 */
interface BleTransport {
    val state: StateFlow<RespoolerState>
    val events: Flow<TransportEvent>

    suspend fun connect(deviceAddress: String)
    suspend fun disconnect()
    suspend fun start()
    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    suspend fun setTargetTemp(celsius: Float)
    suspend fun setFanOn(on: Boolean)
}

sealed class TransportEvent {
    data class Connected(val deviceName: String, val firmware: String) : TransportEvent()
    data object Disconnected : TransportEvent()
    data class Error(val message: String) : TransportEvent()
    data object Started : TransportEvent()
    data object Stopped : TransportEvent()
    data object Paused : TransportEvent()
    data object Resumed : TransportEvent()
    data class Progress(val fraction: Float) : TransportEvent()
}
