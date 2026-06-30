package de.ltsdesign.android.control.data.model

/**
 * 设备连接状态
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * 设备运行状态
 */
enum class DeviceRunState {
    IDLE,        // 空闲
    RUNNING,     // 运行中
    PAUSED,      // 暂停
    ERROR,       // 错误
}

/**
 * 设备完整状态(主页 4 状态卡 + 设备图的数据源)
 */
data class RespoolerState(
    val connection: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val runState: DeviceRunState = DeviceRunState.IDLE,
    val chipTempC: Float? = null,           // 主控芯片温度,°C
    val chamberTempC: Float? = null,        // 仓内温度,°C
    val targetTempC: Float = 60f,            // 目标干燥温度,°C
    val filamentLoaded: Boolean = false,    // 耗材是否已装载
    val filamentType: String? = null,        // 耗材类型
    val coolingFanOn: Boolean = false,      // 冷却风扇
    val progress: Float = 0f,               // 0..1
    val firmwareVersion: String? = null,     // 设备固件
    val deviceName: String? = null,         // 蓝牙名
    val errorMessage: String? = null,
)
