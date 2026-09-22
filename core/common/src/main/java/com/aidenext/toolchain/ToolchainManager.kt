/*
 *  This file is part of AIDE Next.
 *
 *  AIDE Next is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AIDE Next is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AIDE Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.toolchain

import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import java.io.File

object ToolchainManager {

  private val log = LoggerFactory.getLogger(ToolchainManager::class.java)

  /**
   * Scans and returns all known toolchain components along with their installation status.
   */
  @JvmStatic
  fun getInstalledToolchains(): List<ToolchainComponent> {
    val components = mutableListOf<ToolchainComponent>()

    // 1. JDKs
    components.addAll(scanJdks())

    // 2. Android SDK
    components.addAll(scanAndroidSdk())

    // 3. Android NDK
    components.addAll(scanNdks())

    // 4. CMake & Ninja
    components.addAll(scanCMake())

    // 5. Flutter & Dart
    components.addAll(scanFlutterAndDart())

    // 6. Git
    components.addAll(scanGit())

    // 7. ADB
    components.addAll(scanAdb())

    return components
  }

  fun scanJdks(): List<ToolchainComponent> {
    val jdks = mutableListOf<ToolchainComponent>()
    val optDir = File(Environment.PREFIX, "opt")
    if (optDir.exists() && optDir.isDirectory) {
      optDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("openjdk") }?.forEach { jdkDir ->
        val javaBin = File(jdkDir, "bin/java")
        val ver = jdkDir.name.removePrefix("openjdk-").ifEmpty { "17" }
        jdks.add(
          ToolchainComponent(
            id = "jdk-$ver",
            name = "OpenJDK $ver",
            type = ToolchainType.JDK,
            version = ver,
            path = jdkDir,
            isInstalled = javaBin.exists() && javaBin.canExecute(),
            statusDescription = if (javaBin.exists()) "Installed at ${jdkDir.absolutePath}" else "Missing or not executable"
          )
        )
      }
    }
    // Check default JAVA_HOME if not already added
    if (jdks.isEmpty() && Environment.JAVA_HOME != null && Environment.JAVA_HOME.exists()) {
      val javaBin = File(Environment.JAVA_HOME, "bin/java")
      jdks.add(
        ToolchainComponent(
          id = "jdk-default",
          name = "Default OpenJDK",
          type = ToolchainType.JDK,
          version = "17",
          path = Environment.JAVA_HOME,
          isInstalled = javaBin.exists() && javaBin.canExecute(),
          statusDescription = "Installed at ${Environment.JAVA_HOME.absolutePath}"
        )
      )
    }
    return jdks
  }

  fun scanAndroidSdk(): List<ToolchainComponent> {
    val list = mutableListOf<ToolchainComponent>()
    val sdkHome = Environment.ANDROID_HOME

    // Platforms
    val platformsDir = File(sdkHome, "platforms")
    if (platformsDir.exists() && platformsDir.isDirectory) {
      platformsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("android-") }?.forEach { dir ->
        val api = dir.name.removePrefix("android-")
        val androidJar = File(dir, "android.jar")
        list.add(
          ToolchainComponent(
            id = "platform-$api",
            name = "Android SDK Platform $api",
            type = ToolchainType.ANDROID_SDK,
            version = api,
            path = dir,
            isInstalled = androidJar.exists(),
            statusDescription = if (androidJar.exists()) "API $api installed" else "Incomplete (android.jar missing)"
          )
        )
      }
    }

    // Build Tools
    val buildToolsDir = File(sdkHome, "build-tools")
    if (buildToolsDir.exists() && buildToolsDir.isDirectory) {
      buildToolsDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        val ver = dir.name
        val aapt2 = File(dir, "aapt2")
        list.add(
          ToolchainComponent(
            id = "build-tools-$ver",
            name = "Build-Tools $ver",
            type = ToolchainType.ANDROID_SDK,
            version = ver,
            path = dir,
            isInstalled = dir.exists(),
            statusDescription = "Build-Tools $ver installed"
          )
        )
      }
    }

    // Platform Tools
    val platformTools = File(sdkHome, "platform-tools")
    val adb = File(platformTools, "adb")
    list.add(
      ToolchainComponent(
        id = "platform-tools",
        name = "Android Platform Tools",
        type = ToolchainType.ANDROID_SDK,
        version = if (platformTools.exists()) "Installed" else "Missing",
        path = platformTools,
        isInstalled = adb.exists() || platformTools.exists(),
        statusDescription = if (adb.exists()) "Installed (adb present)" else "Missing or incomplete"
      )
    )

    // Command-line Tools
    val cmdlineTools = File(sdkHome, "cmdline-tools")
    val sdkmanager = File(cmdlineTools, "latest/bin/sdkmanager")
    list.add(
      ToolchainComponent(
        id = "cmdline-tools",
        name = "Android Command-line Tools",
        type = ToolchainType.ANDROID_SDK,
        version = if (sdkmanager.exists()) "latest" else "Missing",
        path = cmdlineTools,
        isInstalled = sdkmanager.exists() || File(cmdlineTools, "bin/sdkmanager").exists(),
        statusDescription = if (sdkmanager.exists()) "sdkmanager available" else "Missing sdkmanager"
      )
    )

    return list
  }

  fun scanNdks(): List<ToolchainComponent> {
    val list = mutableListOf<ToolchainComponent>()
    val ndkHome = Environment.NDK_HOME
    if (ndkHome.exists() && ndkHome.isDirectory) {
      ndkHome.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        val ver = dir.name
        val ndkBuild = File(dir, "ndk-build")
        val sourceProp = File(dir, "source.properties")
        list.add(
          ToolchainComponent(
            id = "ndk-$ver",
            name = "Android NDK $ver",
            type = ToolchainType.ANDROID_NDK,
            version = ver,
            path = dir,
            isInstalled = sourceProp.exists() || ndkBuild.exists() || dir.exists(),
            statusDescription = "NDK $ver installed at ${dir.absolutePath}"
          )
        )
      }
    }
    // Also check opt/android-ndk
    val optNdk = File(Environment.PREFIX, "opt/android-ndk")
    if (optNdk.exists() && optNdk.isDirectory) {
      list.add(
        ToolchainComponent(
          id = "ndk-opt",
          name = "Android NDK (opt)",
          type = ToolchainType.ANDROID_NDK,
          version = "custom",
          path = optNdk,
          isInstalled = true,
          statusDescription = "Installed at ${optNdk.absolutePath}"
        )
      )
    }
    return list
  }

  fun scanCMake(): List<ToolchainComponent> {
    val list = mutableListOf<ToolchainComponent>()
    val cmakeHome = Environment.CMAKE_HOME
    if (cmakeHome.exists() && cmakeHome.isDirectory) {
      cmakeHome.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        val ver = dir.name
        val cmakeBin = File(dir, "bin/cmake")
        list.add(
          ToolchainComponent(
            id = "cmake-$ver",
            name = "CMake $ver",
            type = ToolchainType.CMAKE,
            version = ver,
            path = dir,
            isInstalled = cmakeBin.exists() || dir.exists(),
            statusDescription = "CMake $ver installed at ${dir.absolutePath}"
          )
        )
      }
    }
    // Check system cmake in bin
    val sysCmake = File(Environment.BIN_DIR, "cmake")
    if (sysCmake.exists()) {
      list.add(
        ToolchainComponent(
          id = "cmake-system",
          name = "CMake (System)",
          type = ToolchainType.CMAKE,
          version = "system",
          path = Environment.BIN_DIR,
          isInstalled = true,
          statusDescription = "Installed at ${sysCmake.absolutePath}"
        )
      )
    }
    // Check ninja
    val ninjaBin = File(Environment.BIN_DIR, "ninja")
    list.add(
      ToolchainComponent(
        id = "ninja",
        name = "Ninja Build Tool",
        type = ToolchainType.CMAKE,
        version = if (ninjaBin.exists()) "Available" else "Not installed",
        path = ninjaBin,
        isInstalled = ninjaBin.exists() && ninjaBin.canExecute(),
        statusDescription = if (ninjaBin.exists()) "Ninja executable ready" else "Install ninja via package manager"
      )
    )
    return list
  }

  fun scanFlutterAndDart(): List<ToolchainComponent> {
    val list = mutableListOf<ToolchainComponent>()
    val flutterHome = Environment.FLUTTER_HOME
    val flutterBin = File(flutterHome, "bin/flutter")
    val dartBin = File(flutterHome, "bin/cache/dart-sdk/bin/dart")
    val isFlutterInstalled = flutterBin.exists() && flutterBin.canExecute()

    list.add(
      ToolchainComponent(
        id = "flutter-sdk",
        name = "Flutter SDK",
        type = ToolchainType.FLUTTER,
        version = if (isFlutterInstalled) "Ready" else "Missing",
        path = flutterHome,
        isInstalled = isFlutterInstalled,
        statusDescription = if (isFlutterInstalled) "Flutter CLI ready at ${flutterBin.absolutePath}" else "Flutter not installed in $flutterHome"
      )
    )

    val isDartInstalled = dartBin.exists() || (File(Environment.BIN_DIR, "dart").exists())
    list.add(
      ToolchainComponent(
        id = "dart-sdk",
        name = "Dart SDK",
        type = ToolchainType.DART,
        version = if (isDartInstalled) "Ready" else "Missing",
        path = if (dartBin.exists()) dartBin.parentFile?.parentFile else Environment.DART_HOME,
        isInstalled = isDartInstalled,
        statusDescription = if (isDartInstalled) "Dart CLI ready" else "Dart SDK not installed"
      )
    )
    return list
  }

  fun scanGit(): List<ToolchainComponent> {
    val gitBin = File(Environment.BIN_DIR, "git")
    val hasGitBin = gitBin.exists() && gitBin.canExecute()
    return listOf(
      ToolchainComponent(
        id = "git-cli",
        name = "Git CLI",
        type = ToolchainType.GIT,
        version = if (hasGitBin) "Available" else "Missing",
        path = gitBin,
        isInstalled = hasGitBin,
        statusDescription = if (hasGitBin) "Git CLI executable at ${gitBin.absolutePath}" else "git binary not found in bin"
      ),
      ToolchainComponent(
        id = "git-jgit",
        name = "JGit Engine",
        type = ToolchainType.GIT,
        version = "Embedded",
        path = null,
        isInstalled = true,
        statusDescription = "Pure Java Git engine active"
      )
    )
  }

  fun scanAdb(): List<ToolchainComponent> {
    val adbFile = File(Environment.ANDROID_HOME, "platform-tools/adb")
    val sysAdb = File(Environment.BIN_DIR, "adb")
    val isInstalled = (adbFile.exists() && adbFile.canExecute()) || (sysAdb.exists() && sysAdb.canExecute())
    return listOf(
      ToolchainComponent(
        id = "adb",
        name = "ADB (Android Debug Bridge)",
        type = ToolchainType.ADB,
        version = if (isInstalled) "Available" else "Missing",
        path = if (adbFile.exists()) adbFile else sysAdb,
        isInstalled = isInstalled,
        statusDescription = if (isInstalled) "ADB binary available" else "ADB not found"
      )
    )
  }
}
