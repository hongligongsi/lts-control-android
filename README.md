# LTS Control for Android

> **Unofficial third-party Android client for LTS Respooler Pro**
> Not affiliated with LTS Design. For personal, non-commercial use only.

A native Android control app for **LTS Respooler Pro** (firmware ≥ 1.2.1), targeting the gap where LTS Design only ships an iOS client (`LTS Control 1.7.1`).

| Spec | Value |
|------|-------|
| Package | `de.ltsdesign.android.control` |
| Version | **1.7.1-android** (aligned with iOS LTS Control 1.7.1) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Language | Kotlin 2.0.20 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow + Repository (transport-agnostic) |
| BLE transport | Mock (works out-of-box) + Real (placeholder, see below) |

---

## ✨ Features

This MVP ships with full UI and mock data — no hardware required to test.

| Tab | Status | Description |
|---|---|---|
| **Control** | ✅ Implemented | 4 status cards (Connection / Temperature / Filament / Cooling Fan), animated device illustration, Start / Pause / Resume / Stop, progress bar |
| **Connection** | 🚧 Stub | Pair / unpair device UI (real BLE scanning TODO) |
| **Settings** | 🚧 Stub | Drying temperature, fan speed, auto-off, LED, language |
| **More** | ✅ Implemented | About, version info, third-party disclaimer, build info |

Splash screen with animated logo, light + dark theme following Android system.

---

## 📸 Screenshots (target)

The app reproduces the LTS Respooler control layout:
- **Logo**: black center disc + two 180°-symmetric blue arcs (cool/heat fan, top-down view)
- **4 status cards** in 2×2 grid
- **Large circular device illustration** in center (canvas-drawn, spinning when running)
- **Start / Stop / Pause / Resume** buttons with progress bar
- **Bottom 4-tab nav**: Control · Connection · Settings · More

---

## 🏃 Quick start

### Prerequisites
- Android Studio Koala (2024.1.1+) or later
- JDK 17
- Android SDK 35 (compile), 26+ device

### Open & run
1. Open Android Studio → `File → Open` → select `LTSControl-Android/`
2. Wait for Gradle sync (downloads AGP 8.5.2, Compose BOM 2024.08, etc.)
3. Pick `app` run config → choose device or emulator (API 26+)
4. **Run ▶️**

On first launch you'll see the splash → Control tab with **Disconnected** state.
Tap the **Scan for Devices** button (or `Settings → Connection`) — the mock transport will connect after 800ms simulation.

### Build APK
```bash
cd LTSControl-Android
./gradlew assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # signed release APK (needs keystore)
```

### Install on device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔌 Wiring up real BLE

The app ships with a `MockTransport` that simulates the LTS Respooler Pro.
To switch to real hardware, replace the line in `ControlViewModel.kt`:

```kotlin
private val transport: BleTransport = MockTransport()
```

with:

```kotlin
private val transport: BleTransport = LtsBleTransport(application)
```

### ⚠️ You will need the BLE protocol

LTS Design hasn't publicly documented the GATT profile for Respooler Pro.
You'll need to:

1. **Capture iOS app traffic** with PacketLogger or nRF Connect while paired to a real device
2. **Identify Service UUID + Characteristic UUID** (likely proprietary 128-bit UUIDs)
3. **Reverse-engineer the protocol**:
   - Which characteristic to `write` for control commands (Start/Stop/Pause/Resume/setTemp/setFan)?
   - Which characteristic `notify` pushes status frames (chip temp / chamber temp / progress)?
4. Fill the TODO comments in `LtsBleTransport.kt`:
   - `onServicesDiscovered()` — find service & characteristic UUIDs
   - `onCharacteristicChanged()` — parse `parseStatusFrame(data: ByteArray)`
   - `start/stop/pause/resume/setTargetTemp/setFanOn` — write `buildCommandFrame(opcode, payload)`

All methods are already in place with TODO markers — this is plug-and-play once the protocol is known.

### Capturing the iOS App's BLE

If you have access to a Mac + iPhone:

```bash
# macOS Hardware IO Tools → Bluetooth Explorer (legacy)
# Or install nRF Connect on iOS, pair, watch characteristic reads/writes
```

Better yet: log the iOS app via Frida or use a BLE sniffer (nRF52840 dongle + Wireshark with bt-att parser).

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────┐
│  Compose UI (ControlScreen / Settings / Connection)  │
└──────────────────┬───────────────────────────────────┘
                   ▼ StateFlow<RespoolerState>
┌──────────────────────────────────────────────────────┐
│           ControlViewModel (AndroidViewModel)        │
│   connect() start() stop() pause() resume() ...      │
└──────────────────┬───────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────┐
│         interface BleTransport                       │
└────────┬─────────────────────────┬───────────────────┘
         ▼ Mock (default)          ▼ Real (TODO)
   ┌──────────────┐         ┌────────────────────┐
   │ MockTransport│         │ LtsBleTransport    │
   │ - fake data  │         │ - BluetoothGatt    │
   │ - 90s ramp   │         │ - BLE 5.0          │
   └──────────────┘         └────────────────────┘
```

---

## 🎨 Design system

Colors derived from the logo (black center disc + blue arcs):

| Token | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `#1E88E5` (brand blue) | `#64B5F6` | CTA, progress, accents |
| `secondary` | `#FF6A00` (orange) | same | "Running" state |
| `surface` | white | `#0A0A0A` | background |
| `surfaceVariant` | `#F5F5F5` | `#1A1A1A` | cards, chips |
| on-surface | `#1A1A1A` | `#F5F5F5` | main text |

**Status colors:**
- 🟢 Connected: `#43A047` (green)
- 🔵 Connecting: `#1E88E5` (blue)
- 🟠 Running: `#FF6A00` (orange)
- ⚪ Disconnected: `#9E9E9E` (gray)
- 🔴 Error: `#E53935` (red)

---

## 📂 Project layout

```
LTSControl-Android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
├── gradlew / gradlew.bat
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/de/ltsdesign/android/control/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ble/
│       │   │   │   ├── BleTransport.kt        ← interface
│       │   │   │   └── LtsBleTransport.kt    ← real (stub)
│       │   │   ├── model/RespoolerState.kt
│       │   │   └── repository/MockTransport.kt
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
└── docs/
```

---

## 🌍 Localization

- English (default): `values/strings.xml`
- Simplified Chinese: `values-zh/strings.xml`

Add more by dropping `values-<locale>/strings.xml`.

---

## ⚖️ Legal

**Unofficial third-party client, not affiliated with LTS Design.**

- LTS Respooler Pro is a product of LTS Design (Germany)
- "LTS Control" iOS app is © LTS Design
- This Android client is independently developed, no reverse engineering of LTS's proprietary code
- BLE protocol implementation requires user-driven reverse engineering for personal use
- All trademarks belong to their respective owners

**License of this code:** TBD (default: Apache-2.0 OR MIT; choose before publishing)

**For non-commercial personal use only.** Drop a note to LTS Design before public release.

---

## 🤝 What's next

- [ ] Plug real BLE protocol (requires hardware + reverse engineering)
- [ ] Async firmware-update flow
- [ ] Temperature history chart on Control tab
- [ ] Material You dynamic colors (Android 12+)
- [ ] Multi-language (de, fr, ja, ko, es)
- [ ] Wear OS companion
- [ ] Publish to F-Droid (open-source prerequisite)
