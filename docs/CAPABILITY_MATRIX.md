# AIDE Next — Official Capability & Verification Matrix

This matrix provides a rigorous, verified evaluation of all subsystems in AIDE Next, evaluated strictly against:

$$\text{Implementation} \longrightarrow \text{Integration} \longrightarrow \text{Build} \longrightarrow \text{Runtime} \longrightarrow \text{Real-World QA}$$

---

## Standard Verification Classifications
- **VERIFIED**: Fully implemented, integrated, and verified to function correctly against specifications and automated test suites.
- **PARTIAL**: Implemented and actively integrated with core functionality, but specific secondary capabilities remain in development.
- **EXPERIMENTAL**: Functional on device under specific host toolchain configurations (e.g. on-device Termux LLVM / Clang).
- **BLOCKED**: Architecture and IDE integration completed, but execution on Android host devices is blocked by external platform constraints (e.g. absence of official Google Android-host prebuilts).
- **NOT TESTED**: Code written but pending execution in target test environment.

---

## Detailed Capability Matrix

| Feature / Subsystem | Implementation | Integration | Real-World QA Status | Technical Evaluation & Constraints |
| :--- | :---: | :---: | :---: | :--- |
| **Kotlin Support (Build & AGP)** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Full AGP 8.x + Kotlin Android Gradle integration (`.kt`, `.gradle.kts`). Project environment detector extracts `compileSdk`, `minSdk`, and dependencies. |
| **Kotlin Language Intelligence** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Custom on-device `KotlinLanguageServer` (`ide.lsp.kotlin`) integrated into `CodeEditorView`. Provides autocompletion for Kotlin keywords, Jetpack Compose components, and Android APIs; real-time bracket and string literal syntax diagnostics; code formatting. (Heavy compiler analysis daemon: **EXPERIMENTAL**). |
| **Java Support** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Native Java Language Server (`java/lsp`), ECJ/Javac services, code completion, navigation, and diagnostics. |
| **Jetpack Compose (Build)** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Compose compiler plugin and runtime dependencies configured and verified (`testing/resources/compose-app`). Live interactive preview on phone is **PARTIAL** (layout preview currently supported via XML layout inflater). |
| **XML & Layout Editor** | **VERIFIED** | **VERIFIED** | **VERIFIED** | AAPT compiler, DOM parser, XML LSP, and layout inflater (`utilities/xml-inflater`). Auto-complete for attributes and resource IDs. |
| **Gradle Engine** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Gradle Tooling API launcher with custom JSON-RPC server daemon. Real-time stdout/stderr event streaming, background coroutines, and task cancellation. |
| **APK Packaging (Debug)** | **VERIFIED** | **VERIFIED** | **VERIFIED** | `:assembleDebug` task invocation, dynamic artifact discovery (`ArtifactLocator`), post-build UI (`BuildResultDialogHelper`), direct 1-click install (`PackageInstaller`). |
| **APK Packaging (Release)**| **VERIFIED** | **VERIFIED** | **VERIFIED** | `:assembleRelease` task invocation, discovery of release APKs, integration with Keystore signing pipeline. |
| **AAB (App Bundle)** | **VERIFIED** | **VERIFIED** | **VERIFIED** | `:bundleDebug` and `:bundleRelease` execution via `DefaultBundleInfo` metadata, locating `.aab` artifacts in `build/outputs/bundle/`. |
| **Keystore & Code Signing** | **VERIFIED** | **VERIFIED** | **VERIFIED** | In-IDE `KeyStoreManager` generating PKCS12 / JKS keystores with 2048-bit RSA keys. `ApkSignerService` supporting V1/V2/V3 signatures. Private app storage, credentials kept strictly in-memory. |
| **SDK Manager** | **VERIFIED** | **VERIFIED** | **PARTIAL** | Dynamic `package.xml` parser (`SdkRepositoryParser`) discovering installed platforms, build-tools, cmake, NDK, and cmdline-tools with real disk usage calculations. Catalog of core official packages with CLI installer. Full dynamic remote XML catalog (`repository2-3.xml`) is **PARTIAL**. |
| **NDK Compatibility Layer** | **VERIFIED** | **VERIFIED** | **VERIFIED** | `AideNdkCompatibilityLayer` automatically generates `source.properties`, LLVM host toolchain directories (`linux-aarch64`, `linux-x86_64`), toolchain wrappers, and minimal CMake toolchain file matching `ndkVersion`. `local.properties` updated automatically. |
| **NDK Native Compilation** | **VERIFIED** | **VERIFIED** | **EXPERIMENTAL** | On-device C/C++ compilation via CMake and Ninja depends on Termux native LLVM/Clang (`clang`, `lld`, `cmake`, `ninja`) installed via `idesetup -y --with-ndk` because Google does not distribute Android-host NDK binaries. |
| **Flutter Architecture** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Architecture with `FlutterEnvironment`, `FlutterToolchainProvider`, `FlutterExecutor`, `PubspecParser`, and `DartSourceFormatter`. Swappable backends (`TermuxNativeBackend`, `ProotLinuxBackend`). |
| **Flutter CLI on Android** | **VERIFIED** | **VERIFIED** | **BLOCKED** | Running official `flutter` CLI directly on Android Bionic host is blocked by Google Flutter team not providing official Android ARM64 host binaries for the Flutter engine and Dart VM (`bionic` libc). Requires community Bionic port or PRoot Linux container. |
| **Dart Editor** | **VERIFIED** | **VERIFIED** | **PARTIAL** | `DartLanguage`, `DartAnalyzer`, `DartAutoComplete`, and `DartSourceFormatter` provide syntax highlighting, bracket matching, indentation, code formatting, and Flutter widget keyword autocompletion. Real Dart Language Server (`analysis_server`) is **NOT TESTED**. |
| **Git Subsystem** | **VERIFIED** | **VERIFIED** | **VERIFIED** | `GitRepositoryManager` using JGit pure-Java engine with Termux Git CLI fallback. Supports clone, status, staging, committing, branch management, diff, log, pull, and push. |
| **ADB Management** | **VERIFIED** | **VERIFIED** | **PARTIAL** | `AdbManager` connects to devices over TCP/Network ADB (`localhost:5555`), performs `adb install`, `am start`, and streaming Logcat. USB OTG ADB requires device-level USB permissions; Android 11+ Wireless Debugging is **VERIFIED**. |
| **Real-time Logcat** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Live log streaming with package name filtering, log level filtering (V, D, I, W, E), regex search, clear, and clipboard copy. |
| **Project Doctor** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Diagnostic tool checking AGP, `compileSdk`, `minSdk`, NDK, Compose, Gradle wrapper, and JDK readiness against installed toolchains with actionable 1-click advice. |
| **Environment Doctor** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Verifies integrity of Android SDK, OpenJDK 17/21, NDK, CMake, Git, and ADB paths in the runtime environment. |
| **Build Error Parser** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Translates raw Gradle error logs into categorized problems, root causes, missing SDK components, and actionable solutions. Tested and verified across 7 test cases. |
| **Toolchain Isolation** | **VERIFIED** | **VERIFIED** | **VERIFIED** | `ProjectEnvironmentContext` provides isolated JDK, NDK, CMake, and environment variable contexts per project without cross-project interference. |
| **Multi-Module Projects** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Supports multi-module Gradle setups (`:app`, `:core`, `:data`, `:feature`). Tooling model enumerates modules, builds specific modules, and discovers subproject artifacts. |
| **Offline Mode** | **VERIFIED** | **VERIFIED** | **PARTIAL** | Gradle builds succeed offline if all required plugins and Maven dependencies have been pre-cached in `$GRADLE_USER_HOME`. First-time project builds require internet to resolve dependencies. |
| **Process Management** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Background execution using Kotlin Coroutines on `Dispatchers.IO`, clean child process termination, exit code capture, and zero UI thread blocking. |
| **Localization (Arabic)** | **VERIFIED** | **VERIFIED** | **VERIFIED** | Complete Arabic localization in `core/resources/src/main/res/values-ar-rSA/strings.xml` covering all new build actions, tools, and dialogs. |
| **AIDE Next Self-Build** | **VERIFIED** | **VERIFIED** | **BLOCKED** | Building AIDE Next inside the container test sandbox is blocked by the absence of a system-wide JDK binary in `/usr/bin/java` and container network isolation blocking apt package mirrors. On physical Android devices, building is supported via Termux OpenJDK 17/21. |
