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

package com.aidenext.flutter

import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Inspection status for Flutter toolchain.
 */
data class FlutterToolchainStatus(
  val isInstalled: Boolean,
  val flutterVersion: String?,
  val dartVersion: String?,
  val flutterHome: File?,
  val dartHome: File?,
  val libcType: FlutterLibcType,
  val hasGlibcProot: Boolean,
  val statusMessage: String
)

/**
 * Provider responsible for discovering, inspecting, and validating Flutter and Dart SDKs on device.
 */
object FlutterToolchainProvider {

  private val log = LoggerFactory.getLogger(FlutterToolchainProvider::class.java)

  private val CANDIDATE_PATHS = listOf(
    File(Environment.HOME, "flutter"),
    File(Environment.PREFIX, "opt/flutter"),
    File(Environment.PREFIX, "share/flutter"),
    File(Environment.PREFIX, "var/lib/proot-distro/installed-rootfs/debian/usr/local/flutter"),
    File(Environment.PREFIX, "var/lib/proot-distro/installed-rootfs/ubuntu/usr/local/flutter"),
    File("/data/local/tmp/flutter")
  )

  /**
   * Scans available paths and environment variables to detect Flutter toolchain status.
   */
  fun inspectToolchain(): FlutterToolchainStatus {
    val prootDebian = File(Environment.PREFIX, "var/lib/proot-distro/installed-rootfs/debian")
    val hasGlibcProot = prootDebian.exists() && prootDebian.isDirectory

    val resolvedHome = resolveInstalledFlutterHome()
    if (resolvedHome == null) {
      return FlutterToolchainStatus(
        isInstalled = false,
        flutterVersion = null,
        dartVersion = null,
        flutterHome = null,
        dartHome = null,
        libcType = FlutterLibcType.UNKNOWN,
        hasGlibcProot = hasGlibcProot,
        statusMessage = "Flutter SDK not found in standard paths or FLUTTER_HOME."
      )
    }

    val flutterVersion = readVersionFile(File(resolvedHome, "version"))
    val dartVersion = readVersionFile(File(resolvedHome, "bin/cache/dart-sdk/version"))

    val isProotPath = resolvedHome.absolutePath.contains("proot-distro")
    val libc = if (isProotPath) FlutterLibcType.GLIBC else FlutterLibcType.BIONIC

    val dartHome = File(resolvedHome, "bin/cache/dart-sdk")

    return FlutterToolchainStatus(
      isInstalled = true,
      flutterVersion = flutterVersion,
      dartVersion = dartVersion,
      flutterHome = resolvedHome,
      dartHome = if (dartHome.exists()) dartHome else null,
      libcType = libc,
      hasGlibcProot = hasGlibcProot,
      statusMessage = "Flutter $flutterVersion (Dart $dartVersion) detected at ${resolvedHome.absolutePath} [${libc.name}]"
    )
  }

  fun resolveInstalledFlutterHome(): File? {
    // Check environment variable first
    val envFlutter = System.getenv("FLUTTER_HOME") ?: System.getenv("FLUTTER_ROOT")
    if (!envFlutter.isNullOrBlank()) {
      val f = File(envFlutter)
      if (isFlutterSdkDirectory(f)) return f
    }

    // Check candidate paths
    for (candidate in CANDIDATE_PATHS) {
      if (isFlutterSdkDirectory(candidate)) {
        return candidate
      }
    }

    // Check default Environment
    val defaultHome = Environment.resolveFlutterHome()
    if (isFlutterSdkDirectory(defaultHome)) {
      return defaultHome
    }

    return null
  }

  private fun isFlutterSdkDirectory(dir: File): Boolean {
    if (!dir.exists() || !dir.isDirectory) return false
    val flutterBin = File(dir, "bin/flutter")
    val versionFile = File(dir, "version")
    return flutterBin.exists() || versionFile.exists()
  }

  private fun readVersionFile(file: File): String? {
    return try {
      if (file.exists() && file.isFile) {
        file.readText().trim()
      } else null
    } catch (e: Exception) {
      log.debug("Failed reading version file: ${file.absolutePath}", e)
      null
    }
  }
}
