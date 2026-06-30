package de.ltsdesign.android.control.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import de.ltsdesign.android.control.R
import de.ltsdesign.android.control.data.model.ConnectionStatus
import de.ltsdesign.android.control.data.model.DeviceRunState
import de.ltsdesign.android.control.data.model.RespoolerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LTS Respooler Pro 真机 BLE Transport(占位)
 *
 * ⚠️ 这是一个**框架/占位实现**——LTS Design 没有公开 BLE GATT 协议
 *
 * 真实集成步骤:
 * 1. 抓 iOS LTS Control 1.7.1 的蓝牙流量(用 PacketLogger / nRF Connect)
 * 2. 找出 Service UUID + Characteristic UUID
 * 3. 解析出"读状态 / 写命令"的协议格式
 * 4. 填到下面 [parseStatusFrame] / [buildCommandFrame] 里
 *
 * 当前状态: 接口已通,所有方法都是 no-op + state 推到 ERROR
 */
@SuppressLint("MissingPermission")
class LtsBleTransport(
    private val context: Context,
) : BleTransport {

    private val _state = MutableStateFlow(RespoolerState())
    override val state: StateFlow<RespoolerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var gatt: BluetoothGatt? = null
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // TODO: 解析 LTS 设备广播包(可能按 name 前缀 "LTS-" 过滤)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    scope.launch { _events.emit(TransportEvent.Disconnected) }
                    _state.update { it.copy(connection = ConnectionStatus.DISCONNECTED) }
                    g.close()
                    gatt = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            // TODO: 找出 LTS 的 service/characteristic UUID
            // 例: val service = g.getService(LTS_SERVICE_UUID)
            //     val txChar = service.getCharacteristic(LTS_TX_CHAR_UUID)
            //     val rxChar = service.getCharacteristic(LTS_RX_CHAR_UUID)
            //     g.setCharacteristicNotification(rxChar, true)
            scope.launch {
                _events.emit(TransportEvent.Connected("LTS-Respooler-Pro", "1.2.1"))
            }
            _state.update {
                it.copy(
                    connection = ConnectionStatus.CONNECTED,
                    deviceName = "LTS-Respooler-Pro",
                    firmwareVersion = "1.2.1",
                )
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            // TODO: 解析设备主动推送的状态帧
            val data = characteristic.value
            // parseStatusFrame(data) -> 更新 _state
        }
    }

    override suspend fun connect(deviceAddress: String) {
        if (!hasBlePermission()) {
            val msg = context.getString(R.string.error_no_bluetooth_permission)
            _state.update { it.copy(connection = ConnectionStatus.ERROR, errorMessage = msg) }
            _events.emit(TransportEvent.Error(msg))
            return
        }
        val adapter = bluetoothAdapter() ?: run {
            val msg = context.getString(R.string.error_no_bluetooth)
            _state.update { it.copy(connection = ConnectionStatus.ERROR, errorMessage = msg) }
            return
        }
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            val msg = context.getString(R.string.error_invalid_address)
            _state.update { it.copy(connection = ConnectionStatus.ERROR, errorMessage = msg) }
            return
        }
        _state.update { it.copy(connection = ConnectionStatus.CONNECTING) }
        gatt = device.connectGatt(context, false, gattCallback)
    }

    override suspend fun disconnect() {
        gatt?.disconnect()
    }

    override suspend fun start() {
        // TODO: 写入 start 命令帧
    }

    override suspend fun stop() {
        // TODO: 写入 stop 命令帧
    }

    override suspend fun pause() {
        // TODO: 写入 pause 命令帧
    }

    override suspend fun resume() {
        // TODO: 写入 resume 命令帧
    }

    override suspend fun setTargetTemp(celsius: Float) {
        _state.update { it.copy(targetTempC = celsius) }
        // TODO: 写入 set_temp 命令
    }

    override suspend fun setFanOn(on: Boolean) {
        _state.update { it.copy(coolingFanOn = on) }
        // TODO: 写入 set_fan 命令
    }

    // -------- helpers --------

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return mgr?.adapter
    }

    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    @Suppress("unused")
    private fun parseStatusFrame(data: ByteArray) {
        // TODO: 替换为真实协议解析
        // LTS 协议未公开,先放占位
        _state.update {
            it.copy(
                runState = DeviceRunState.IDLE,
            )
        }
    }

    @Suppress("unused")
    private fun buildCommandFrame(opcode: Byte, payload: ByteArray = byteArrayOf()): ByteArray =
        byteArrayOf(opcode) + payload
}
