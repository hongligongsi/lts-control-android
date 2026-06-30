# 编译打包指南

## 0. 先验证文件树完整

```bash
cd /root/projects/LTSControl-Android
find . -type f -not -path '*/\.*' | sort
```

应看到 40+ 个文件,包括:
- 根: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`
- `app/`: `build.gradle.kts`, `proguard-rules.pro`, `src/main/AndroidManifest.xml`
- 12 个 .kt 文件 + 14 个 .xml 资源

## 1. 首次同步(很慢,下载 ~600MB)

⚠️ 容器里 gradle 跑不动(没 JRE,也没 SDK),**这一步需要你自己在 Android Studio 里做**,或宿主机装了 SDK。

### 选项 A:Android Studio(推荐)
1. Android Studio → `File → Open`
2. 选 `/root/projects/LTSControl-Android/`
3. 等 Gradle Sync 完成(10~30 分钟,看网络)
4. 看到 `BUILD SUCCESSFUL` 后,**Run ▶️** 装到模拟器或真机

### 选项 B:命令行(必须有 JDK 17 + Android SDK 35)

```bash
cd /root/projects/LTSControl-Android
export ANDROID_HOME=$HOME/Android/Sdk             # 改成你的路径
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk     # 你的 JDK 17 路径

./gradlew --version          # 验证 gradle wrapper 工作
./gradlew tasks              # 列出可用任务

./gradlew assembleDebug      # 编译 debug APK
```

输出:`app/build/outputs/apk/debug/app-debug.apk`

## 2. 安装到真机 / 模拟器

```bash
# 通过 ADB
adb devices                                   # 确认设备在线
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb shell am start -n de.ltsdesign.android.control.debug/de.ltsdesign.android.control.MainActivity
```

包名注意:
- Debug: `de.ltsdesign.android.control.debug`(自动加 `.debug` 后缀)
- Release: `de.ltsdesign.android.control`

## 3. Release 签名(发布需要)

### 3.1 生成 keystore(只一次)

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias ltscontrol \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

(密码自己设,记好)

### 3.2 配置签名

编辑 `app/build.gradle.kts`,加 `signingConfigs`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = "你的密码"
            keyAlias = "ltscontrol"
            keyPassword = "你的密码"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### 3.3 编译 release

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

⚠️ **不要** 把 `release.keystore` 提交到 git。`/root/projects/LTSControl-Android/.gitignore` 已加 `*.keystore` 排除。

## 4. 验证 APK 安装后的状态

第一次启动应该看到:

1. **Splash**:蓝色渐变背景 + 大 logo + "LTS Control" + 副标
2. **Control Tab**:
   - 4 状态卡显示全部 "Disconnected" / "—"
   - 中间设备图(无旋转)
   - "Scan for Devices" 按钮在底部 / 设备图下方
3. 点 **Scan**:800ms 后变成:
   - Connection: ✅ Connected (绿)
   - Temperature: 24.8°C / Chip 32.4°C
   - Filament: PLA
   - Cooling Fan: Off
   - 设备名:LTS-Respooler-Pro,FW 1.2.1
4. 点 **Start**:进度条缓慢推进,温度从 24.8 升到 60°C,风扇在温度接近目标时打开,设备图开始旋转
5. 点 **Pause**:暂停,设备图停下
6. 点 **Resume / Stop**:恢复正常 / 完全停止

## 5. 调试常见问题

### Gradle Sync 失败 / 依赖下载超时

Gradle wrapper 默认从 `services.gradle.org` 下载,国内可能慢。改 mirror:

`app/build.gradle.kts` 顶部加:
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}
```

或者用全局 init 脚本 `~/.gradle/init.gradle.kts`:
```kotlin
allprojects {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
```

### Compose 编译器报错

`kotlin.version` vs `compose.compiler` 版本不匹配。Compose 1.7+ 用 Kotlin Compose Compiler Plugin (Kotlin 2.0+),工程用的是 `org.jetbrains.kotlin.plugin.compose` 自动匹配 Kotlin 2.0.20,正常不会出问题。

### `MaterialTheme.colorScheme.onSurfaceVariant` 在老 API 缺失

`minSdk = 26`,Material3 在 26+ 全支持,不会出问题。

### 容器里跑 build 报错"Unable to find Java"

容器没装 JDK。**这一步必须在宿主机/AS 里跑**,容器只能写代码。

## 6. 发布到 F-Droid(可选)

1. 把工程推到 GitHub
2. 加 `LICENSE`(Apache-2.0 推荐)
3. 加 `fastlane/metadata/android/en-US/short.txt` 等
4. 提 PR 到 https://github.com/f-droid/fdroiddata

⚠️ App icon 里如果有"商标"问题,F-Droid 会要求提供授权证明——这种情况下需要把图标改成不挂品牌的版本。
