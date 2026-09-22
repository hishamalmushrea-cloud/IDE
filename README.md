# IDE

[![GitHub Repo stars](https://img.shields.io/github/stars/hishamalmushrea-cloud/IDE?style=social)](https://github.com/hishamalmushrea-cloud/IDE)
[![GitHub](https://img.shields.io/github/license/hishamalmushrea-cloud/IDE)](https://github.com/hishamalmushrea-cloud/IDE/blob/main/LICENSE)
[![Trendshift](https://trendshift.io/api/badge/repositories/4119)](https://trendshift.io/)
[![Docs Website](https://img.shields.io/badge/Docs-Website-blue?style=for-the-badge&logo=readthedocs)](https://github.com/hishamalmushrea-cloud/IDE)
[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/)
[![X (formerly Twitter) Follow](https://img.shields.io/twitter/follow/hishamalmushrea-cloud?style=social)](https://x.com/hishamalmushrea-cloud)

---

<p align="center">
  <img src="./images/icon.png" alt="AIDE Next" width="80" height="80"/>
</p>

<h2 align="center"><b>AIDE Next — Professional Mobile IDE</b></h2>
<p align="center">
  A professional, toolchain-based IDE to develop, build, sign, and test real Gradle-based Android, NDK/C++, and Flutter applications directly on Android devices.
<p><br>

<p align="center">
<img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License">
<img src="https://img.shields.io/badge/Platform-Android%20(ARM64%20|%20ARM32%20|%20x86__64)-green.svg" alt="Platform">
<img src="https://img.shields.io/badge/Architecture-Toolchain--Based-orange.svg" alt="Architecture">
</p>

---

## Overview

**AIDE Next** turns your Android phone or tablet into a full-fledged software engineering workstation, delivering a desktop-grade Android Studio experience natively on Android.

From GitHub clone to APK/AAB installation, everything runs self-contained on the device:
```
GitHub ──► Clone ──► Open ──► Configure ──► Build ──► APK/AAB ──► Sign ──► Install ──► Run ──► Logcat
```

> **Note on AI / Autonomous Systems:**
> AIDE Next is strictly a real, developer-controlled IDE and build environment. There are **no AI assistants, chatbots, LLMs, or code-generation bots** inside the application. The IDE focuses entirely on: **EDIT ➔ BUILD ➔ TEST ➔ RUN ➔ DEBUG ➔ PACKAGE**.

---

## Core Capabilities & Toolchains

### 1. Toolchain-Based Architecture
The IDE is powered by an independent, modular **Toolchain Manager**:
- **Android SDK:** Platforms (API 31 - 35), Build Tools (30.0.3 - 35.0.0), Platform Tools (`adb`), Command-line Tools (`sdkmanager`).
- **Java Toolchain:** Multi-JDK support with **OpenJDK 17** (LTS default) and **OpenJDK 21** (Modern LTS), automatically selected per project requirement.
- **Android NDK & C/C++:** Native C/C++ development with multi-version NDK support (r25c, r26d, r27b, r28), CMake, Ninja, Clang, LLD, and `CMakeLists.txt` integration.
- **Flutter & Dart:** Native Flutter SDK and Dart CLI integration with support for `flutter doctor`, `flutter pub get`, `flutter clean`, `flutter build apk`, and `flutter build appbundle`.
- **Git & VCS:** Dual-engine Git integration featuring pure-Java JGit plus Termux native Git CLI with branch, commit, diff, stash, pull, and push support.
- **Terminal System:** Embedded Linux sysroot with full POSIX shell environment (`bash`, `git`, `gradle`, `cmake`, `ninja`, `clang`, `flutter`, `dart`, `adb`).

---

### 2. Android Build System & Packaging
- **Supported Project Types:** Kotlin, Java, Jetpack Compose, XML, Gradle Kotlin DSL (`.gradle.kts`), Multi-module projects, and Android libraries (`.aar`).
- **Build Operations:**
  - `Build Debug APK`
  - `Build Release APK`
  - `Build All APKs`
  - `Build App Bundle (AAB)` (Debug & Release)
  - `Clean Project` & `Rebuild Project`
- **Real-Time Build Diagnostics:** Full stdout/stderr capture, duration timing, exit code tracking, warning and error counter.
- **Post-Build Dashboard:** Displays artifact file name, path, variant, size (KB/MB), architecture, and provides 1-click actions:
  - `[Install APK]`
  - `[Open APK Location]`
  - `[Share APK]`
  - `[Sign with Keystore]`
  - `[Build Again]`

---

### 3. Keystore & APK/AAB Signing
- In-IDE **Keystore Manager**:
  - Generate new PKCS12 / JKS keystores with 2048-bit RSA keys and self-signed X.509 certificates.
  - Load existing keystores, list aliases, and unlock keys with secure password input.
  - V1 (JAR signing) and V2/V3 signature scheme support.
  - Build ➔ Sign ➔ Install/Share workflow.

---

### 4. SDK Manager GUI
A dedicated visual interface replacing terminal-only setup:
- **Installed Components:** Browse installed SDK platforms, build-tools, NDKs, and CMake with disk usage calculations.
- **Available Components:** Catalog of official Android SDK packages with download and installation support.
- **Safe Component Removal:** Disk space reclamation with confirmation prompts.
- **Project Requirements Auto-Detection:** Automatically discovers required `compileSdk` and `ndkVersion` from the current project.

---

### 5. Native NDK & C/C++ Support
- Complete C and C++ support for Android apps (`app/src/main/cpp/`).
- External native builds via `CMakeLists.txt` and `Android.mk`.
- Support for `ndkVersion` and `cmake.version` configured in `build.gradle` or `build.gradle.kts`.
- Automatic configuration of `ndk.dir` and `cmake.dir` in `local.properties`.
- In-editor C/C++ syntax highlighting, block indenting, and autocompletion for C++ keywords, standard library headers, and JNI functions (`JNIEnv`, `JNIEXPORT`, `__android_log_print`).

---

### 6. Flutter & Dart Support
- Automatic Flutter project recognition from `pubspec.yaml`.
- Toolchain detection of Flutter SDK and Dart SDK.
- Flutter commands available directly in the IDE:
  - `flutter doctor`
  - `flutter pub get`
  - `flutter clean`
  - `flutter build apk` (Debug, Profile, Release)
  - `flutter build appbundle`
  - `flutter analyze`
  - `flutter test`
- Editor support for Dart files (`.dart`): syntax highlighting, bracket matching, indentation, and autocompletion for Dart keywords and Flutter widgets (`StatelessWidget`, `StatefulWidget`, `Scaffold`, `MaterialApp`, etc.).

---

### 7. Diagnostics: Project Doctor & Environment Doctor
- **Project Doctor:** Inspects the active project's `build.gradle`, wrapper, and manifests. Reports whether requirements are satisfied (`✓`), missing (`✗`), or warning on version mismatch (`⚠`), with suggested 1-click actions.
- **Environment Doctor:** Validates the health of the host environment: Java runtime, Android SDK, NDK, CMake, Git, Flutter, Dart, and ADB.
- **Intelligent Build Error Parser:** Translates cryptic Gradle and compiler errors into actionable solutions:
  - Missing Android SDK Platform ➔ Direct link to install platform.
  - Missing Build-Tools ➔ Direct link to install build-tools.
  - Java version incompatibility ➔ Suggests switching between JDK 17 and JDK 21.
  - Out of Memory ➔ Suggests adjusting `org.gradle.jvmargs`.
  - Unresolved dependencies ➔ Offers offline/online checks.

---

### 8. ADB & Logcat
- ADB device manager: lists connected devices (including current local host device and remote/USB ADB devices).
- Deploy and launch applications directly on target devices (`am start` / `pm install`).
- Real-time Logcat viewer with filtering by package name, log priority levels (Verbose, Debug, Info, Warn, Error), search, clear, and export.

---

## Installation & Setup

1. Download and install the AIDE Next APK.
2. Grant Storage and Notification permissions.
3. In the Onboarding screen, select your preferred Android SDK version (e.g. `34.0.4` or `35.0.0`) and JDK version (`17` or `21`).
4. To install native C/C++ or Flutter support, open **Toolchain Manager** or run:
   ```bash
   pkg install clang lld cmake ninja make
   ```
5. Clone any repository via HTTPS or SSH:
   ```
   https://github.com/username/repository.git
   ```

---

## License

AIDE Next is free software licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
Based on [AndroidIDE](https://github.com/AndroidIDEOfficial/AndroidIDE), licensed under GPL-3.0.
All original copyright notices and license terms are preserved.
