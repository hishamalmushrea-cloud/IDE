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

package com.aidenext.ndk

import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Supported Android ABIs for native code compilation.
 */
enum class AndroidAbi(val abiString: String) {
  ARM64_V8A("arm64-v8a"),
  ARMEABI_V7A("armeabi-v7a"),
  X86_64("x86_64"),
  X86("x86");

  companion object {
    fun fromString(s: String?): AndroidAbi {
      return entries.find { it.abiString.equals(s, ignoreCase = true) } ?: ARM64_V8A
    }
  }
}

/**
 * Compatibility layer adapting on-device Termux LLVM/Clang/Ninja toolchain into the directory
 * structure and properties expected by the Android Gradle Plugin (AGP) and CMake.
 */
object AideNdkCompatibilityLayer {

  private val log = LoggerFactory.getLogger(AideNdkCompatibilityLayer::class.java)

  /**
   * Prepares or validates an NDK compatibility directory matching the requested [ndkVersion].
   * Ensures that AGP finds source.properties and host-tagged LLVM binaries.
   */
  fun setupCompatibilityNdk(ndkVersion: String): File {
    val targetDir = File(Environment.NDK_HOME, ndkVersion)
    if (isValidNdkDirectory(targetDir)) {
      log.info("NDK $ndkVersion is already valid at ${targetDir.absolutePath}")
      return targetDir
    }

    targetDir.mkdirs()

    // 1. Generate source.properties required by AGP
    val sourceProps = File(targetDir, "source.properties")
    if (!sourceProps.exists()) {
      sourceProps.writeText(
        """
        Pkg.Desc = Android NDK
        Pkg.Revision = $ndkVersion
        """.trimIndent() + "\n"
      )
    }

    // 2. Setup host-tagged LLVM prebuilt directories
    val hostTags = listOf("linux-aarch64", "linux-x86_64")
    val termuxBin = File(Environment.PREFIX, "bin")

    for (hostTag in hostTags) {
      val llvmBin = File(targetDir, "toolchains/llvm/prebuilt/$hostTag/bin")
      llvmBin.mkdirs()

      // Create wrappers for clang, clang++, lld, ar, strip
      createToolchainWrapper(llvmBin, "clang", File(termuxBin, "clang"))
      createToolchainWrapper(llvmBin, "clang++", File(termuxBin, "clang++"))
      createToolchainWrapper(llvmBin, "ld.lld", File(termuxBin, "ld.lld"))
      createToolchainWrapper(llvmBin, "llvm-ar", File(termuxBin, "llvm-ar"))
      createToolchainWrapper(llvmBin, "llvm-strip", File(termuxBin, "llvm-strip"))
    }

    // 3. Setup minimal android.toolchain.cmake if not present
    val cmakeBuildDir = File(targetDir, "build/cmake")
    cmakeBuildDir.mkdirs()
    val toolchainCmake = File(cmakeBuildDir, "android.toolchain.cmake")
    if (!toolchainCmake.exists()) {
      toolchainCmake.writeText(
        """
        # AIDE Next On-Device NDK Toolchain Adapter
        cmake_minimum_required(VERSION 3.6.0)
        set(ANDROID_NDK_TRUE ON)
        set(CMAKE_SYSTEM_NAME Android)
        if(NOT DEFINED ANDROID_PLATFORM)
          set(ANDROID_PLATFORM 24)
        endif()
        if(NOT DEFINED ANDROID_ABI)
          set(ANDROID_ABI arm64-v8a)
        endif()
        """.trimIndent() + "\n"
      )
    }

    log.info("AIDE Next NDK compatibility layer configured at: ${targetDir.absolutePath}")
    return targetDir
  }

  private fun createToolchainWrapper(dir: File, toolName: String, actualBinary: File) {
    val wrapper = File(dir, toolName)
    if (wrapper.exists()) return

    val binPath = if (actualBinary.exists()) actualBinary.absolutePath else "/data/data/com.aidenext/files/usr/bin/$toolName"
    val script = """
      #!/bin/sh
      exec "$binPath" "$@"
    """.trimIndent() + "\n"

    try {
      wrapper.writeText(script)
      wrapper.setExecutable(true, false)
    } catch (e: Exception) {
      log.warn("Failed creating wrapper for $toolName", e)
    }
  }

  fun isValidNdkDirectory(dir: File): Boolean {
    if (!dir.exists() || !dir.isDirectory) return false
    val sourceProps = File(dir, "source.properties")
    return sourceProps.exists()
  }
}
