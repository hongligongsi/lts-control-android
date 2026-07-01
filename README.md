# LTS Control for Android — 安卓客户端

> **非官方第三方 Android 客户端,适用于 LTS Respooler Pro**
> 与 LTS Design **无任何关联**,仅供个人非商业使用。

为 LTS Respooler Pro(固件 ≥ 1.2.1)写的原生 Android 控制 App,补足 LTS Design 只发 iOS 客户端(`LTS Control 1.7.1`)的空白。

| 规格 | 值 |
|---|---|
| 包名 | `de.ltsdesign.android.control` |
| 版本 | 1.7.1-android(对齐 iOS LTS Control 1.7.1) |
| 最低 SDK | 26(Android 8.0)|
| 目标 SDK | 35(Android 15)|
| 编程语言 | Kotlin 2.0.20 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + StateFlow + Repository(transport 无关)|
| BLE 传输 | Mock(开箱即用)+ Real(占位 stub,见下面)|

## 已实现

这个 MVP 包含完整 UI 与 mock 数据,不用硬件也能跑起来。

| 页面 | 状态 | 说明 |
|---|---|---|
| 控制 | ✅ 已完成 | 4 张状态卡(连接 / 温度 / 耗材 / 散热风扇)、动画设备示意图、开始 / 暂停 / 恢复 / 停止、进度条 |
| 连接 | 🚧 占位 | 配对 / 解绑设备 UI(真 BLE 扫描待做) |
| 设置 | 🚧 占位 | 语言、传送速度、风扇等(已与 LTS Web 控制 HTML + BLE 协议对齐) |
| 更多 | ✅ 已完成 | 关于、版本、第三方声明、构建信息 |

启动屏带动画 logo,白天 / 夜间主题跟随系统。

## 设计目标

App 复刻 LTS Respooler 控制面板布局:

- **Logo**:黑色中心圆盘 + 两条 180 度对称的蓝色弧(俯视散热风扇/加热风扇)
- **4 张状态卡** 排成 2×2
- **大圆形设备示意图** 居中(canvas 绘制,运行时旋转)
- **开始 / 停止 / 暂停 / 恢复** 按钮带进度条
- **底部 4 个 Tab**:控制 · 连接 · 设置 · 更多

## 快速开始

### 前置准备

- Android Studio Koala(2024.1.1+)或更新版
- JDK 17
- Android SDK 35(编译用), 26+ 真机

### 打开并运行

1. Android Studio → 文件 → 打开 → 选择 `LTSControl-Android/`
2. 等待 Gradle sync(会下载 AGP 8.5.2、Compose BOM 2024.08 等)
3. 选 `app` 运行配置 → 选真机或模拟器(API 26+)
4. 点 运行 ▶️

第一次启动会看到启动屏 → 控制 Tab 显示**未连接**状态。
点 **扫描设备** 按钮(或 设置 → 连接) — mock 传输会在 800ms 模拟连接后跑起来。

### 构建 APK

```bash
cd LTSControl-Android
./gradlew assembleDebug       # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # 签名 release APK(需要 keystore)
```

### 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 接真 BLE

App 默认用 `MockTransport`,模拟 LTS Respooler Pro。
要切到真硬件,改 `ControlViewModel.kt` 里这行:

```kotlin
private val transport: BleTransport = MockTransport()
```

改为:

```kotlin
private val transport: BleTransport = LtsBleTransport(application)
```

### 你需要 BLE 协议

LTS Design 没有公开 Respooler Pro 的 GATT profile,所以你得自己:

1. **抓 iOS 应用流量**:用 PacketLogger 或 nRF Connect,连真设备后看
2. **识别 Service UUID + Characteristic UUID**(很可能是 128 位私有 UUID)
3. **逆向协议**:
   - 哪个 characteristic 用来 `write` 控制命令(开始/停止/暂停/恢复/设温度/设风扇)?
   - 哪个 characteristic `notify` 推状态帧(芯片温度 / 仓温 / 进度)?
4. 填 `LtsBleTransport.kt` 里的 TODO 注释:
   - `onServicesDiscovered()` — 找到 service & characteristic UUID
   - `onCharacteristicChanged()` — 解析 `parseStatusFrame(data: ByteArray)`
   - `start/stop/pause/resume/setTargetTemp/setFanOn` — 写 `buildCommandFrame(opcode, payload)`

所有方法已有 TODO 标记,协议知道就能 plug-and-play。

### 抓 iOS App 的 BLE

如果你能搞到 Mac + iPhone:

```bash
# macOS Hardware IO Tools → Bluetooth Explorer(老的)
# 或者 iOS 上装 nRF Connect,配对后看 characteristic 读写
```

更好:用 Frida 拦 iOS App 的 BLE 调用,或者用 BLE sniffer(nRF52840 加密狗 + Wireshark + bt-att 解析器)。

## 项目结构

```
┌──────────────────────────────────────────────────────┐
│  Compose UI(控制 / 设置 / 连接 / 更多)              │
└──────────────────┬───────────────────────────────────┘
                   ▼ StateFlow<RespoolerState>
┌──────────────────────────────────────────────────────┐
│           ControlViewModel(AndroidViewModel)         │
│   connect() start() stop() pause() resume() ...      │
└──────────────────┬───────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────┐
│         interface BleTransport                       │
└────────┬─────────────────────────┬───────────────────┘
         ▼ Mock(默认)             ▼ Real(TODO)
   ┌──────────────┐         ┌────────────────────┐
   │ MockTransport│         │ LtsBleTransport    │
   │ - 假数据     │         │ - BluetoothGatt    │
   │ - 90 秒爬升 │         │ - BLE 5.0          │
   └──────────────┘         └────────────────────┘
```

## 设计系统

颜色从 logo 衍生(黑中心 + 蓝弧):

| Token | 白天 | 夜晚 | 用途 |
|---|---|---|---|
| 主色 | `#1E88E5`(品牌蓝)| `#64B5F6` | CTA、进度条、强调 |
| 次色 | `#FF6A00`(橘)| 同上 | "运行中"状态 |
| 表面 | 白 | `#0A0A0A` | 背景 |
| 表面变体 | `#F5F5F5` | `#1A1A1A` | 卡片、chip |
| 表面字 | `#1A1A1A` | `#F5F5F5` | 主文字 |

**状态色:**

- 🟢 已连接:`#43A047`(绿)
- 🔵 连接中:`#1E88E5`(蓝)
- 🟠 运行中:`#FF6A00`(橘)
- ⚪ 未连接:`#9E9E9E`(灰)
- 🔴 错误:`#E53935`(红)

## 文件组织

```
LTSControl-Android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
├── gradlew / gradlew.bat
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/de/ltsdesign/android/control/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ble/
│       │   │   │   ├── BleTransport.kt        ← 接口
│       │   │   │   └── LtsBleTransport.kt    ← 真(stub)
│       │   │   ├── model/RespoolerState.kt
│       │   │   └── repository/MockTransport.kt
│       │   ├── i18n/
│       │   │   ├── LocaleHelper.kt           ← 语言切换
│       │   │   └── AppViewModel.kt
│       │   ├── navigation/
│       │   │   ├── AppNavigation.kt
│       │   │   └── BottomBar.kt
│       │   └── ui/
│       │       ├── ControlViewModel.kt
│       │       ├── theme/
│       │       ├── components/
│       │       │   ├── StatusCard.kt
│       │       │   └── AnimatedDeviceIllustration.kt
│       │       └── screens/
│       │           ├── ControlScreen.kt
│       │           ├── SettingsScreen.kt
│       │           ├── ConnectionScreen.kt
│       │           └── MoreScreen.kt
│       └── res/
│           ├── drawable/ic_logo.xml
│           ├── mipmap-anydpi-v26/...
│           ├── values/colors.xml, strings.xml, themes.xml
│           ├── values-night/themes.xml
│           └── values-zh/strings.xml
```

## 国际化

- **英文**(默认):`values/strings.xml`
- **简体中文**:`values-zh/strings.xml`

切换语言:设置 → 语言 → 跟随系统 / English / 中文。

加新语言就新建 `values-<locale>/strings.xml` 即可。

## 法律声明

**非官方第三方客户端,与 LTS Design 无任何关联。**

- LTS Respooler Pro 是 LTS Design(德国)的产品
- "LTS Control" iOS 应用 © LTS Design
- 这个 Android 客户端是我独立开发的,**没有反向工程 LTS 的私有代码**
- BLE 协议实现需要用户自行研究 + 仅供个人使用
- 所有商标归各自所有者

**本仓库许可:** 待选(默认 Apache-2.0 或 MIT,发布前选)

**仅供个人非商业使用。** 公开发布前请先通知 LTS Design。

## 后续计划

- [ ] 接真 BLE 协议(需要硬件 + 逆向)
- [ ] 异步固件升级流程
- [ ] 控制 Tab 加温度历史图
- [ ] Material You 动态取色(Android 12+)
- [ ] 多语言(德、法、日、韩、西)
- [ ] Wear OS 配套 App
- [ ] 上 F-Droid(开源前置)
