# AIDE Next — Real Device QA & Testing Guide

This guide describes the complete, end-to-end verification and testing workflow on physical Android devices (ARM64 / aarch64, Android 10+).

```
[Install APK] ──► [Onboarding SDK/JDK] ──► [Install NDK / Flutter] ──► [Open Project] ──► [Build APK] ──► [Sign & Install] ──► [Logcat QA]
```

---

## 1. Prerequisites & Installation

### Device Requirements
- **OS**: Android 10 (API 29) or higher (Android 11+ recommended for Wireless Debugging).
- **Architecture**: `arm64-v8a` (ARM64 / aarch64).
- **RAM**: Minimum 4GB (6GB–8GB recommended for concurrent Gradle builds).
- **Storage**: Minimum 8GB free internal storage.

### Installing AIDE Next
1. Download the latest debug/release APK from the GitHub Releases or compile via:
   ```bash
   ./gradlew :core:app:assembleDebug
   ```
2. Sideload and install the APK on your Android device.
3. Launch AIDE Next and grant:
   - **All Files Access** (Storage permission)
   - **Notifications** (for build status notifications)

---

## 2. Onboarding & Toolchain Setup

### Step A: Android SDK & OpenJDK Configuration
1. Upon first launch, the Onboarding wizard opens automatically.
2. Select your desired Android SDK platform (e.g. `platforms;android-35` or `android-34`).
3. Select your JDK version:
   - **OpenJDK 17**: Default LTS for AGP 8.0 through 8.6.
   - **OpenJDK 21**: Modern LTS for AGP 8.7+ and modern Kotlin libraries.
4. Tap **Download & Setup**. The setup executes `idesetup.sh` inside the internal Termux environment.

### Step B: Native NDK & C/C++ Setup
To compile C/C++ native code via CMake on Android:
1. Open the internal Terminal inside AIDE Next (bottom panel or drawer).
2. Run the onboarding setup with NDK flag:
   ```bash
   idesetup -y --with-ndk
   ```
   Or install the Termux native compiler packages directly:
   ```bash
   pkg install -y clang lld cmake ninja make
   ```
3. AIDE Next's `AideNdkCompatibilityLayer` will automatically wire `local.properties` with:
   ```properties
   ndk.dir=/data/data/com.aidenext/files/home/android-sdk/ndk/27.2.12479018
   cmake.dir=/data/data/com.aidenext/files/home/android-sdk/cmake/3.22.1
   ```

### Step C: Flutter & Dart Setup (Experimental / Container)
Because Google does not publish official precompiled ARM64 Android-host binaries:
- **Option 1 (Termux Native Bionic Port)**:
  Install the community ARM64 Bionic package:
  ```bash
  idesetup -y --with-flutter
  ```
- **Option 2 (PRoot-Distro Linux Container)**:
  ```bash
  pkg install -y proot-distro
  proot-distro install debian
  proot-distro login debian -- apt update && apt install -y curl git openjdk-17-jdk
  ```

---

## 3. Project Creation & Verification Workflows

### Workflow 1: Kotlin Android Application
1. Tap **+ / Open Project** and navigate to your Kotlin project or clone from Git:
   ```
   https://github.com/example/kotlin-app.git
   ```
2. **Project Doctor:**
   - Tap **Tools ➔ Project Doctor**.
   - Verify all checks display `✓ READY` (`compileSdk: 35`, `AGP: 8.7`, `JDK: 17/21`).
3. **Build Debug APK:**
   - Tap **Build ➔ Build Debug APK** (or `:assembleDebug`).
   - Observe live build logs in the bottom build output panel.
4. **Inspect & Install:**
   - Upon completion, the **Build Results Dashboard** appears displaying:
     - Output file: `app-debug.apk`
     - File size: ~4.2 MB
     - Status: `Signed (Debug Keystore)`
   - Tap **[Install APK]** to trigger Android's `PackageInstaller`.
   - Tap **[Share APK]** to test sharing via Android Share Sheet.

---

### Workflow 2: Jetpack Compose Application
1. Open a Jetpack Compose project (e.g. `testing/resources/compose-app`).
2. Verify in `build.gradle.kts`:
   ```kotlin
   buildFeatures { compose = true }
   ```
3. Open `MainActivity.kt` in the editor:
   - Verify Kotlin syntax highlighting.
   - Type `@Com` and observe autocomplete proposing `@Composable`.
   - Type `Col` and observe autocomplete proposing `Column`.
4. Run **Build ➔ Build Debug APK**.
5. Install and launch the Compose app on the device.

---

### Workflow 3: C/C++ Native NDK Project
1. Open a native C++ project (e.g. `testing/resources/native-ndk-app`).
2. Verify `app/src/main/cpp/CMakeLists.txt` and `native-lib.cpp`:
   ```cpp
   #include <jni.h>
   extern "C" JNIEXPORT jstring JNICALL
   Java_com_example_nativeapp_MainActivity_stringFromJNI(JNIEnv* env, jobject thiz) {
       return env->NewStringUTF("Hello from C++ with NDK in AIDE Next!");
   }
   ```
3. Tap **Tools ➔ Project Doctor** to ensure `ndkVersion` and CMake are recognized.
4. Run **Build ➔ Build Debug APK**.
5. Verify that Gradle executes `externalNativeBuildDebug` and compiles `libnativeapp.so`.
6. Install and open the app: verify the native string renders on screen.

---

### Workflow 4: Release APK & Keystore Code Signing
1. Tap **Tools ➔ Keystore & Signing**.
2. Tap **Create New Keystore**:
   - Keystore Name: `release-key.jks`
   - Alias: `release-alias`
   - Validity: 25 years
3. Tap **Build ➔ Build Release APK**.
4. In the Build Results Dialog, tap **[Sign with Keystore]**.
5. Select the created keystore and confirm signing.
6. Verify output artifact is signed and ready for installation.

---

### Workflow 5: Git & GitHub Workflow
1. In the editor toolbar, tap **Tools ➔ Git / VCS**.
2. Tap **Git Status**: View modified, untracked, and staged files.
3. Select a modified file, tap **View Diff**: Verify unified diff output.
4. Tap **Stage All**, enter commit message: `feat: test commit on mobile`.
5. Tap **Commit**.
6. Tap **Git Pull** and **Git Push** to sync with remote GitHub repository.

---

### Workflow 6: ADB Device & Logcat Testing
1. Enable **Developer Options** and **Wireless Debugging** in Android Settings.
2. Note the Wi-Fi pairing port and IP.
3. In AIDE Next, tap **Tools ➔ ADB & Devices**.
4. Connect to localhost:
   ```bash
   adb connect localhost:5555
   ```
5. Tap **View Logcat**:
   - Filter by your app's package name (e.g. `com.example.kotlinapp`).
   - Filter by level `Debug` or `Error`.
   - Verify log entries stream in real-time as your app executes.
