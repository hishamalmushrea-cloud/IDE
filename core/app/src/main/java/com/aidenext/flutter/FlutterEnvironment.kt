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
import java.io.File

/**
 * Libc runtime execution environment for Flutter on Android host.
 */
enum class FlutterLibcType {
  /** Native Android Bionic C library (requires bionic-compiled Flutter engine / Termux port). */
  BIONIC,
  /** Standard Linux Glibc runtime (executes inside PRoot/chroot Linux container on Android). */
  GLIBC,
  /** Unknown or unverified runtime. */
  UNKNOWN
}

/**
 * Encapsulates environment variables, paths, and runtime configuration for Flutter.
 */
data class FlutterEnvironment(
  val flutterHome: File,
  val dartHome: File? = null,
  val libcType: FlutterLibcType = FlutterLibcType.BIONIC,
  val flutterExecutable: File = File(flutterHome, "bin/flutter"),
  val dartExecutable: File? = dartHome?.let { File(it, "bin/dart") } ?: File(flutterHome, "bin/cache/dart-sdk/bin/dart"),
  val customStorageBaseUrl: String? = null,
  val customPubHostedUrl: String? = null
) {

  val isValid: Boolean
    get() = flutterHome.exists() && flutterHome.isDirectory

  fun createEnvironmentMap(): MutableMap<String, String> {
    val env = mutableMapOf<String, String>()
    env["FLUTTER_HOME"] = flutterHome.absolutePath
    env["FLUTTER_ROOT"] = flutterHome.absolutePath
    dartHome?.let {
      env["DART_HOME"] = it.absolutePath
      env["DART_ROOT"] = it.absolutePath
    }

    // Set custom storage mirrors if configured (useful in firewalled regions)
    customStorageBaseUrl?.let { env["FLUTTER_STORAGE_BASE_URL"] = it }
    customPubHostedUrl?.let { env["PUB_HOSTED_URL"] = it }

    // Prepend flutter and dart to PATH
    val binDir = File(flutterHome, "bin").absolutePath
    val dartBinDir = dartExecutable?.parentFile?.absolutePath ?: File(flutterHome, "bin/cache/dart-sdk/bin").absolutePath
    val currentPath = System.getenv("PATH") ?: ""
    env["PATH"] = "$binDir:$dartBinDir:$currentPath"

    return env
  }

  companion object {
    /**
     * Resolves the default [FlutterEnvironment] from system variables or standard paths.
     */
    fun defaultEnvironment(): FlutterEnvironment {
      val home = Environment.resolveFlutterHome()
      val dartHome = Environment.resolveDartHome()
      return FlutterEnvironment(
        flutterHome = home,
        dartHome = if (dartHome.exists()) dartHome else null,
        libcType = if (File("/system/bin/linker64").exists()) FlutterLibcType.BIONIC else FlutterLibcType.GLIBC
      )
    }
  }
}
