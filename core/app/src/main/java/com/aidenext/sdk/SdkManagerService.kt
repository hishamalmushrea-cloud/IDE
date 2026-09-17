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

package com.aidenext.sdk

import com.aidenext.toolchain.ProjectRequirements
import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import com.aidenext.toolchain.ProjectEnvironmentDetector
import java.io.File

object SdkManagerService {

  private val log = LoggerFactory.getLogger(SdkManagerService::class.java)

  /** Standard SDK catalog of available components */
  val AVAILABLE_PACKAGES: List<SdkPackage> = listOf(
    // Platforms
    SdkPackage("platforms;android-35", "Android SDK Platform 35", SdkCategory.PLATFORMS, "35", false, description = "Android 15 Platform"),
    SdkPackage("platforms;android-34", "Android SDK Platform 34", SdkCategory.PLATFORMS, "34", false, description = "Android 14 Platform"),
    SdkPackage("platforms;android-33", "Android SDK Platform 33", SdkCategory.PLATFORMS, "33", false, description = "Android 13 Platform"),
    SdkPackage("platforms;android-32", "Android SDK Platform 32", SdkCategory.PLATFORMS, "32", false, description = "Android 12L Platform"),
    SdkPackage("platforms;android-31", "Android SDK Platform 31", SdkCategory.PLATFORMS, "31", false, description = "Android 12 Platform"),

    // Build Tools
    SdkPackage("build-tools;35.0.0", "Build-Tools 35.0.0", SdkCategory.BUILD_TOOLS, "35.0.0", false, description = "Android SDK Build-Tools 35.0.0"),
    SdkPackage("build-tools;34.0.0", "Build-Tools 34.0.0", SdkCategory.BUILD_TOOLS, "34.0.0", false, description = "Android SDK Build-Tools 34.0.0"),
    SdkPackage("build-tools;34.0.4", "Build-Tools 34.0.4", SdkCategory.BUILD_TOOLS, "34.0.4", false, description = "AndroidIDE Enhanced Build-Tools 34.0.4"),
    SdkPackage("build-tools;33.0.2", "Build-Tools 33.0.2", SdkCategory.BUILD_TOOLS, "33.0.2", false, description = "Android SDK Build-Tools 33.0.2"),
    SdkPackage("build-tools;30.0.3", "Build-Tools 30.0.3", SdkCategory.BUILD_TOOLS, "30.0.3", false, description = "Legacy Build-Tools 30.0.3"),

    // Platform Tools
    SdkPackage("platform-tools", "Android SDK Platform-Tools", SdkCategory.PLATFORM_TOOLS, "35.0.1", false, description = "adb, etc."),

    // Command-line Tools
    SdkPackage("cmdline-tools;latest", "Android SDK Command-line Tools (latest)", SdkCategory.CMDLINE_TOOLS, "latest", false, description = "sdkmanager, avdmanager"),

    // NDK
    SdkPackage("ndk;28.0.12433566", "Android NDK r28", SdkCategory.NDK, "28.0.12433566", false, description = "Android NDK (Side by side) 28"),
    SdkPackage("ndk;27.2.12479018", "Android NDK r27b", SdkCategory.NDK, "27.2.12479018", false, description = "Android NDK (Side by side) 27.2"),
    SdkPackage("ndk;26.3.11579264", "Android NDK r26d", SdkCategory.NDK, "26.3.11579264", false, description = "Android NDK (Side by side) 26.3"),
    SdkPackage("ndk;25.2.9519653", "Android NDK r25c", SdkCategory.NDK, "25.2.9519653", false, description = "Android NDK (Side by side) 25.2"),

    // CMake
    SdkPackage("cmake;3.28.1", "CMake 3.28.1", SdkCategory.CMAKE, "3.28.1", false, description = "CMake build tool for C/C++"),
    SdkPackage("cmake;3.22.1", "CMake 3.22.1", SdkCategory.CMAKE, "3.22.1", false, description = "Default AGP CMake version")
  )

  /**
   * Alias for [getInstalledPackages]. Returns the SDK packages which are installed on this device.
   */
  fun getInstalledComponents(): List<SdkPackage> = getInstalledPackages()

  /**
   * Alias for [getAllPackages]. Returns the catalog of all the known SDK packages along with
   * their installation status.
   */
  fun getAvailableComponents(): List<SdkPackage> = getAllPackages()

  /**
   * Returns the SDK packages which are required by the project in the given [projectDir] and are
   * not installed yet.
   */
  fun getMissingPackagesForProject(projectDir: File): List<SdkPackage> {
    return try {
      val requirements = ProjectEnvironmentDetector.detect(projectDir)
      findMissingRequirements(requirements)
    } catch (e: Exception) {
      log.warn("Failed to detect required SDK packages for {}", projectDir, e)
      emptyList()
    }
  }

  /**
   * Formats the given [bytes] count into a human readable string, for example `1.2 MB`.
   */
  fun formatSize(bytes: Long): String {
    if (bytes <= 0L) {
      return "0 B"
    }

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
      value /= 1024.0
      unit++
    }

    return String.format(java.util.Locale.US, "%.1f %s", value, units[unit])
  }

  fun getAllPackages(): List<SdkPackage> {
    val installed = getInstalledPackages()
    val installedMap = installed.associateBy { it.id }

    return AVAILABLE_PACKAGES.map { avail ->
      installedMap[avail.id] ?: avail
    }
  }

  fun getInstalledPackages(): List<SdkPackage> {
    val list = mutableListOf<SdkPackage>()
    val sdkHome = Environment.ANDROID_HOME ?: return list

    // Platforms
    val platformsDir = File(sdkHome, "platforms")
    if (platformsDir.exists()) {
      platformsDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("android-") }?.forEach { dir ->
        val api = dir.name.removePrefix("android-")
        list.add(
          SdkPackage(
            id = "platforms;android-$api",
            name = "Android SDK Platform $api",
            category = SdkCategory.PLATFORMS,
            version = api,
            isInstalled = true,
            installDir = dir,
            sizeBytes = calculateDirectorySize(dir),
            description = "Installed Android SDK Platform"
          )
        )
      }
    }

    // Build Tools
    val buildToolsDir = File(sdkHome, "build-tools")
    if (buildToolsDir.exists()) {
      buildToolsDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        list.add(
          SdkPackage(
            id = "build-tools;${dir.name}",
            name = "Build-Tools ${dir.name}",
            category = SdkCategory.BUILD_TOOLS,
            version = dir.name,
            isInstalled = true,
            installDir = dir,
            sizeBytes = calculateDirectorySize(dir),
            description = "Installed Build Tools"
          )
        )
      }
    }

    // Platform Tools
    val platformTools = File(sdkHome, "platform-tools")
    if (platformTools.exists()) {
      list.add(
        SdkPackage(
          id = "platform-tools",
          name = "Android SDK Platform-Tools",
          category = SdkCategory.PLATFORM_TOOLS,
          version = "Installed",
          isInstalled = true,
          installDir = platformTools,
          sizeBytes = calculateDirectorySize(platformTools),
          description = "Installed Platform-Tools"
        )
      )
    }

    // NDK
    val ndkDir = File(sdkHome, "ndk")
    if (ndkDir.exists()) {
      ndkDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        list.add(
          SdkPackage(
            id = "ndk;${dir.name}",
            name = "Android NDK ${dir.name}",
            category = SdkCategory.NDK,
            version = dir.name,
            isInstalled = true,
            installDir = dir,
            sizeBytes = calculateDirectorySize(dir),
            description = "Installed NDK toolchain"
          )
        )
      }
    }

    // CMake
    val cmakeDir = File(sdkHome, "cmake")
    if (cmakeDir.exists()) {
      cmakeDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        list.add(
          SdkPackage(
            id = "cmake;${dir.name}",
            name = "CMake ${dir.name}",
            category = SdkCategory.CMAKE,
            version = dir.name,
            isInstalled = true,
            installDir = dir,
            sizeBytes = calculateDirectorySize(dir),
            description = "Installed CMake"
          )
        )
      }
    }

    return list
  }

  fun uninstallPackage(pkg: SdkPackage): Boolean {
    val dir = pkg.installDir ?: return false
    return try {
      dir.deleteRecursively()
    } catch (e: Exception) {
      log.error("Failed to delete package: {}", pkg.id, e)
      false
    }
  }

  fun calculateTotalDiskUsage(): Long {
    val sdkHome = Environment.ANDROID_HOME ?: return 0L
    return calculateDirectorySize(sdkHome)
  }

  private fun calculateDirectorySize(dir: File): Long {
    var size = 0L
    try {
      dir.walkTopDown().filter { it.isFile }.forEach { size += it.length() }
    } catch (e: Exception) {
      // ignore
    }
    return size
  }

  fun findMissingRequirements(req: ProjectRequirements): List<SdkPackage> {
    val installed = getInstalledPackages().associateBy { it.id }
    val missing = mutableListOf<SdkPackage>()

    val reqPlatform = "platforms;android-${req.compileSdk ?: 34}"
    if (!installed.containsKey(reqPlatform)) {
      missing.add(AVAILABLE_PACKAGES.find { it.id == reqPlatform } ?: SdkPackage(
        id = reqPlatform,
        name = "Android SDK Platform ${req.compileSdk ?: 34}",
        category = SdkCategory.PLATFORMS,
        version = "${req.compileSdk ?: 34}",
        isInstalled = false
      ))
    }

    if (req.hasCpp && req.ndkVersion != null) {
      val reqNdk = "ndk;${req.ndkVersion}"
      if (!installed.containsKey(reqNdk)) {
        missing.add(AVAILABLE_PACKAGES.find { it.id == reqNdk } ?: SdkPackage(
          id = reqNdk,
          name = "Android NDK ${req.ndkVersion}",
          category = SdkCategory.NDK,
          version = req.ndkVersion,
          isInstalled = false
        ))
      }
    }

    return missing
  }
}
