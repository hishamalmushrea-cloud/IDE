# AIDE Next — Full Architectural Audit & Technical Roadmap

## 1. System Architecture Analysis

AIDE Next (forked from AndroidIDE) is designed as an Android application that hosts a local software development environment directly on Android devices (ARM64, ARM, x86_64).

### Key Subsystems:
1. **Core Application & UI Layer (`core:app`, `core:actions`, `utilities:*`)**:
   - Built on AndroidX Jetpack, Material Design 3, Fragment navigation, and Kotlin coroutines.
   - Uses an extensible Action Registry (`ActionsRegistry`) to wire UI actions to toolbar, menus, file tree context actions, and sidebar drawers.
   - Service binding model: `GradleBuildService` runs as a foreground service with notification channel to prevent Android OS termination during long builds.

2. **Terminal & Process Execution (`termux:*`, `core:common`)**:
   - Embedded Termux terminal emulator and shell runner (`termux:emulator`, `termux:view`, `termux:application`, `termux:shared`).
   - Linux sysroot rooted at `/data/data/<pkg>/files/usr` (`PREFIX`) and home at `/data/data/<pkg>/files/home` (`HOME`).
   - Process execution via standard POSIX `fork`/`exec` via Android Bionic runtime, allowing native command execution (`bash`, `git`, `pkg`, `ninja`, `cmake`, `clang`).

3. **Tooling & Build System (`tooling:*`, `core:projects`)**:
   - Multi-process architecture: UI process connects to a separate JVM process running `tooling-api-all.jar` through standard streams using LSP4J JSON-RPC protocol.
   - Communicates with Gradle Daemon through the Gradle Tooling API.
   - Uses Android Gradle Plugin (AGP) builder models (`BasicAndroidProject`, `AndroidProject`, `AndroidDsl`, `VariantDependencies`) to inspect projects, modules, variants, source folders, and dependencies.
   - Custom Gradle init script (`androidide.init.gradle`) injects IDE hooks, custom Maven repositories, AAPT2 override, and optional LogSender plugin.

4. **Editor & Language Infrastructure (`editor:*`, `java:*`, `xml:*`)**:
   - Sora Editor (`io.github.Rosemoe.sora-editor`) provides code editing, syntax highlighting, text actions, and completion popups.
   - Tree-sitter integration for Java, Kotlin, XML, JSON, and log files.
   - ANTLR4-based incremental lexers for C/C++ (`CPP14Lexer`), Groovy, and Java.
   - Language servers: `nb-javac-android` for Java LSP, and custom AAPT2-based XML LSP.

---

## 2. Existing Features vs. Gap Analysis

| Component | Existing State | Required Future State | Gap Severity |
|---|---|---|---|
| **Android Build** | Quick run (`assemble<Variant>`) + install | Full variant builds (Debug APK, Release APK, All APKs, AAB), artifact metadata dashboard, duration/size stats | High |
| **AAB Support** | Model contains `BundleInfo`, but no UI or bundle action | Dedicated "Build AAB" action, bundle output parser, signing & export | High |
| **APK/AAB Signing** | None (only internal signing plugin for IDE APK) | Keystore Manager (create, pick, alias, passwords, secure credentials), V1/V2/V3 signing | Critical |
| **Toolchain Manager** | Rudimentary bash script (`idesetup.sh`) | Unified Toolchain Manager for Android SDK, NDK, CMake, JDK, Flutter, Dart, Git | Critical |
| **SDK Manager** | Terminal-only command `sdkmanager` | Full GUI SDK Manager (Platforms, Build Tools, Platform Tools, NDK, CMake, CLI) with download/install/update/remove | Critical |
| **Project Doctor / Detection** | Project opened directly; fails if SDK/JDK mismatch | Auto-detection of AGP, Gradle, compileSdk, minSdk, NDK, CMake, Flutter, JDK with health check & fix actions | Critical |
| **NDK & C/C++** | Disabled ("No official NDK support"), no NDK version picker | Full NDK & CMake integration: multi-version NDK management, CMake discovery, C++ code completion, JNI support | Critical |
| **Flutter & Dart** | Not supported | Full Flutter & Dart toolchain (SDK discovery, pubspec detection, flutter CLI commands, APK/AAB build, Dart editor) | Critical |
| **Git / GitHub** | Simple `cloneRepository` dialog on main screen | Complete Git UI: Clone (HTTPS/SSH), Commit, Push, Pull, Branch, Checkout, Merge, Fetch, Status, Diff, Log, Stash | High |
| **ADB & Devices** | None (direct on-device package installer only) | ADB device manager (local/network), `adb install`, `pm`, `am start`, multi-device targets | High |
| **Logcat** | Internal IDE PID logger only; custom app socket log | Real device Logcat reader with package filter, priority filter, search, clear, copy, export | High |
| **Error Diagnostics** | Raw console output | Intelligent build error parser (Problem, Cause, Required Component, 1-Click Fix action) | High |

---

## 3. Toolchain-based Architecture Plan

```
Toolchain Manager
├── Android Toolchain
│   ├── SDK Platforms (android-31 ... android-35)
│   ├── Build Tools (30.0.3 ... 35.0.0)
│   ├── Platform Tools (adb, fastboot)
│   ├── Command-line Tools (sdkmanager, avdmanager)
│   ├── CMake (3.22.1, 3.28.x)
│   └── NDK (r25, r26, r27, r28)
├── Java Toolchain
│   ├── JDK 17 (Default LTS)
│   └── JDK 21 (Modern LTS)
├── Kotlin Toolchain
│   └── Kotlin Compiler / Daemon / CLI
├── Flutter & Dart Toolchain
│   ├── Flutter SDK (Stable / Custom channels)
│   └── Dart SDK (Dart CLI, dart analysis server)
├── Git & VCS Toolchain
│   ├── JGit (In-process Java Git engine)
│   └── Git CLI (Termux native git binary)
└── ADB & Platform Tools
    ├── ADB Client / Server
    └── Device Bridge
```

---

## 4. Technical Roadmap & Implementation Phases

- **Phase 1: Comprehensive Audit & Technical Baseline (Done)**
- **Phase 2: Android Build Engine Enhancement (Debug, Release, Variants, Build Progress, Artifacts)**
- **Phase 3: Toolchain Manager & Multi-JDK Selection**
- **Phase 4: APK & AAB Packaging, Keystore & Signing Management**
- **Phase 5: ADB Device Bridge, Enhanced Logcat & Testing Subsystem**
- **Phase 6: Native NDK, CMake & C/C++ Development Subsystem**
- **Phase 7: Flutter & Dart Toolchain, Project Workflow & Editor Support**
- **Phase 8: Advanced Git / GitHub Integration**
- **Phase 9: Project Doctor, Environment Doctor & Error Diagnostics**
- **Phase 10: Performance Optimization, Documentation, Sample Test Projects & Final Validation**
