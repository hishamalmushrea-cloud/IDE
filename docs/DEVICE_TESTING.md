# AIDE Next — Real-Device Verification & Production Testing Manual

This document provides the official, evidence-based testing specification for validating **AIDE Next** directly on physical Android devices.

---

## 1. Hardware & Runtime Test Environments

### Device Hardware Profiles
- **Profile A (4 GB RAM / Low-Tier)**: Requires `org.gradle.jvmargs=-Xmx1536m`, single-worker Gradle daemon (`--max-workers=2`), and build cache enabled.
- **Profile B (6 GB–8 GB RAM / Mid-Tier / Standard)**: Default configuration (`-Xmx2048m`, `--max-workers=4`).
- **Profile C (12 GB+ RAM / Flagship / Tablet)**: High performance (`-Xmx4096m`, parallel builds, instant syntax analysis).

### Android OS & Permissions
- Android 10 (API 29) through Android 15 (API 35).
- Storage Access: `MANAGE_EXTERNAL_STORAGE` (All Files Access) granted to AIDE Next.
- Notification permission granted for background build status.
- Termux / Private Sandbox: `/data/data/com.aidenext/files/home`.

---

## 2. On-Device Setup Verification Workflow

### Step 1: Bootstrap Toolchains
Launch AIDE Next, open the integrated Terminal drawer, and execute:
```bash
# 1. Update Termux environment
pkg update -y

# 2. Setup Android SDK (Platform 34/35 & Build-Tools) and OpenJDK 17/21
idesetup -y

# 3. Setup Native C/C++ Toolchain (Clang, LLD, CMake, Ninja)
idesetup -y --with-ndk
```

Verify the environment output:
```bash
echo $JAVA_HOME       # Expected: /data/data/com.aidenext/files/usr/opt/openjdk-17
echo $ANDROID_HOME    # Expected: /data/data/com.aidenext/files/home/android-sdk
which clang ninja cmake adb git # Expected: all found in $PREFIX/bin
```

---

## 3. Evidence-Based Real-Device Test Suite

### TEST-ANDROID-KOTLIN-001: Kotlin Android App Build & Run
- **Test ID**: `TEST-ANDROID-KOTLIN-001`
- **Target Project**: `testing/resources/kotlin-app`
- **Specification**: Kotlin 2.0.21, AGP 8.7.0, compileSdk 35, minSdk 24.
- **Workflow**:
  1. Open project in AIDE Next.
  2. Open **Tools ➔ Project Doctor**: Verify `[✓] compileSdk: 35`, `[✓] AGP: 8.7.0`, `[✓] JDK: 17`.
  3. Tap **Build ➔ Build Debug APK**.
  4. Command Executed: `:assembleDebug`
  5. Expected Output:
     - Output Path: `app/build/outputs/apk/debug/app-debug.apk`
     - Duration: ~18–35s (first run), ~4–8s (incremental)
     - Status: `SUCCESSFUL`
  6. In Build Result Dialog, tap **[Install APK]**: Android `PackageInstaller` prompts installation.
  7. Launch app: Verifies Activity renders without crashing.
- **Result**: `VERIFIED`

---

### TEST-ANDROID-COMPOSE-001: Jetpack Compose App Build & Edit
- **Test ID**: `TEST-ANDROID-COMPOSE-001`
- **Target Project**: `testing/resources/compose-app`
- **Specification**: Jetpack Compose BOM 2024.09.00, Material 3, Compose Compiler Plugin.
- **Workflow**:
  1. Open `compose-app/src/main/kotlin/com/example/composeapp/MainActivity.kt`.
  2. Verify syntactic completion: Type `@Com` ➔ Editor suggests `@Composable`.
  3. Edit text: Change `Greeting("AIDE Next Compose")` to `Greeting("Hello from Physical Android Device")`.
  4. Tap **Save** then **Build ➔ Build Debug APK**.
  5. Install and launch APK.
  6. Verify updated greeting string renders on screen.
- **Compose Preview Status**: `PARTIAL` (Layout preview supported via XML layout inflater; interactive Compose preview on device requires host rendering surface).
- **Build & Packaging Result**: `VERIFIED`

---

### TEST-ANDROID-JAVA-001: Java Android App Build
- **Test ID**: `TEST-ANDROID-JAVA-001`
- **Target Project**: `testing/resources/java-app`
- **Specification**: Java 17, AGP 8.x, compileSdk 34.
- **Workflow**:
  1. Open project and verify Java LSP initialization (`JavaLanguageServer`).
  2. Test code completion in `MainActivity.java`: Type `set` ➔ suggests `setContentView(...)`.
  3. Execute **Build ➔ Build Debug APK**.
  4. Verify APK generation in `build/outputs/apk/debug/`.
- **Result**: `VERIFIED`

---

### TEST-ANDROID-MULTI-001: Multi-Module Gradle App
- **Test ID**: `TEST-ANDROID-MULTI-001`
- **Target Project**: `testing/resources/multi-module-app`
- **Specification**: 3 modules: `:app` depends on `:feature` and `:core`; `:feature` depends on `:core`.
- **Workflow**:
  1. Open `multi-module-app`.
  2. Verify Tooling API imports all 3 modules in Project Explorer.
  3. Execute `:app:assembleDebug`.
  4. Verify Gradle builds `:core:compileDebugKotlin`, `:feature:compileDebugKotlin`, and packages `:app:assembleDebug`.
  5. Install and launch APK: Verifies string `"Hello from Core module, AIDE Next User! [Processed by Feature module]"` displays on screen.
- **Result**: `VERIFIED`

---

### TEST-ANDROID-NDK-001: Native C++ / CMake / JNI Build
- **Test ID**: `TEST-ANDROID-NDK-001`
- **Target Project**: `testing/resources/native-ndk-app`
- **Specification**: `CMakeLists.txt`, `native-lib.cpp`, `ndkVersion = "27.2.12479018"`, ABI `arm64-v8a`.
- **Workflow**:
  1. Open project in AIDE Next.
  2. `AideNdkCompatibilityLayer` configures `local.properties` with `ndk.dir` and `cmake.dir`.
  3. Execute `:app:assembleDebug`.
  4. CMake generates Ninja build rules using Termux Clang.
  5. Ninja compiles `native-lib.cpp` into `libnativeapp.so`.
  6. APK packages `lib/arm64-v8a/libnativeapp.so`.
  7. Install and launch APK: Screen displays native JNI string `"Hello from C++ with NDK & CMake in AIDE Next!"`.
- **ABI Constraints**:
  - `arm64-v8a`: `VERIFIED` (via Termux native clang/lld).
  - `armeabi-v7a`, `x86_64`: `EXPERIMENTAL` (requires Termux-NDK cross-sysroot).
- **Result**: `VERIFIED (arm64-v8a) / EXPERIMENTAL (cross-ABIs)`

---

### TEST-ANDROID-SIGNING-001: Keystore Generation & Release Signing
- **Test ID**: `TEST-ANDROID-SIGNING-001`
- **Workflow**:
  1. Open **Tools ➔ Keystore & Signing**.
  2. Generate new Keystore: `release.jks`, alias `myreleasekey`, password `SecurePassword123`.
  3. Verify file saved to app-private storage: `$HOME/keystores/release.jks`.
  4. Verify password is never printed in logs or terminal history.
  5. Execute **Build ➔ Build Release APK**.
  6. Tap **[Sign & Save]**: Generates `app-release-signed.apk`.
  7. Run verification:
     ```bash
     apksigner verify --verbose app-release-signed.apk
     ```
  8. Output verifies: `Verified using v1 scheme (JAR signing): true`, `Verified using v2 scheme (APK Signature Scheme v2): true`.
- **Result**: `VERIFIED`

---

### TEST-ANDROID-GIT-001: Dual-Engine Git Subsystem
- **Test ID**: `TEST-ANDROID-GIT-001`
- **Workflow**:
  1. Open a Git project.
  2. Edit a file in editor.
  3. Open **Tools ➔ Git / VCS ➔ Git Status**: Modified file is displayed.
  4. Tap **View Diff**: Unified diff shows added/removed lines.
  5. Tap **Stage All & Commit**: Enter commit message.
  6. Tap **Commit History (Log)**: Shows commit hash, author, and timestamp.
  7. Tap **Branch Manager**: Switch between branches or create a new branch.
- **Result**: `VERIFIED`

---

### TEST-ANDROID-ADB-001: Wireless ADB & Real-Time Logcat
- **Test ID**: `TEST-ANDROID-ADB-001`
- **Workflow**:
  1. Enable Wireless Debugging in Android Developer Options.
  2. Open **Tools ➔ ADB & Devices ➔ Connect via Wireless ADB**: Connect to `localhost:5555`.
  3. Tap **List Connected Devices**: Verifies device is in `device` state.
  4. Tap **View Real-Time Logcat**:
     - Filter by app package: `com.example.kotlinapp`.
     - Filter by priority: `Warn` and `Error`.
     - Introduce deliberate crash (`throw RuntimeException("Test Crash")`).
     - Verify full stack trace is captured in Logcat viewer.
- **USB OTG Status**: `PARTIAL` (Restricted without root / requires Android USB Host permissions).
- **Wireless ADB Status**: `VERIFIED`

---

### TEST-ANDROID-FLUTTER-001: Flutter & Dart Toolchain Investigation
- **Test ID**: `TEST-ANDROID-FLUTTER-001`
- **Target Project**: `testing/resources/flutter-app`
- **Specification**: `pubspec.yaml`, `lib/main.dart`.
- **Status Breakdown**:
  - **Project Detection & Pubspec Parser**: `VERIFIED` (Extracts name, sdk constraints, dependencies).
  - **Dart Editor & Formatter**: `VERIFIED` (Syntax highlighting, bracket indentation via `DartSourceFormatter`).
  - **Toolchain Abstraction**: `VERIFIED` (`FlutterToolchainProvider`, `FlutterExecutor`, `FlutterBuildBackend`).
  - **Flutter CLI on Native Bionic Android Host**: `BLOCKED` (Google official Flutter SDK binaries are glibc-only; requires community Bionic port or PRoot Linux container).
  - **Flutter via PRoot Linux Backend**: `EXPERIMENTAL` (Runs Linux ARM64 Flutter inside Debian PRoot).

---

## 4. Security & Secret Protection Audits

1. **Keystore Passwords**: Keystore and key passwords are typed into masked `EditText` (`TYPE_TEXT_VARIATION_PASSWORD`), maintained in-memory as `char[]`, and overwritten with zeros immediately after cryptographic operations.
2. **Logs & Diagnostics**: Passwords and tokens are strictly excluded from `BuildManager` log outputs, Logcat reader, and diagnostic reports.
3. **Storage Hygiene**: `$HOME/keystores/` is situated in the app's internal sandbox and excluded from Git `.gitignore`.
