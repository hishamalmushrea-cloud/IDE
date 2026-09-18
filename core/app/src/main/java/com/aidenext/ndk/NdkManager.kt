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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

data class NdkInstallation(
  val version: String,
  val path: File,
  val isDefault: Boolean = false
)

object NdkManager {

  private val log = LoggerFactory.getLogger(NdkManager::class.java)

  /**
   * Scans for all available NDK installations in $ANDROID_HOME/ndk and $SYSROOT/opt/android-ndk
   */
  fun getInstalledNdks(): List<NdkInstallation> {
    val list = mutableListOf<NdkInstallation>()
    val ndkHome = Environment.NDK_HOME

    if (ndkHome.exists() && ndkHome.isDirectory) {
      ndkHome.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }?.forEach { dir ->
        list.add(NdkInstallation(version = dir.name, path = dir))
      }
    }

    val optNdk = File(Environment.PREFIX, "opt/android-ndk")
    if (optNdk.exists() && optNdk.isDirectory) {
      list.add(NdkInstallation(version = "System-NDK", path = optNdk))
    }

    return list
  }

  /**
   * Returns the best matching NDK for a requested version string, or the latest available.
   * If not found, attempts to set up a compatibility NDK layer for that version.
   */
  fun resolveNdk(requestedVersion: String?): NdkInstallation? {
    val installed = getInstalledNdks()

    if (!requestedVersion.isNullOrBlank()) {
      installed.find { it.version.startsWith(requestedVersion) }?.let { return it }
      // If requested version is specified but not present, set up compatibility layer
      try {
        val compatDir = AideNdkCompatibilityLayer.setupCompatibilityNdk(requestedVersion)
        return NdkInstallation(version = requestedVersion, path = compatDir)
      } catch (e: Exception) {
        log.warn("Failed creating NDK compatibility adapter for $requestedVersion", e)
      }
    }

    return installed.firstOrNull()
  }

  /**
   * Writes ndk.dir and cmake.dir into local.properties of the project so Gradle uses them.
   */
  fun configureProjectNdk(projectDir: File, requestedNdkVersion: String?, requestedCmakeVersion: String?): Boolean {
    val localProps = File(projectDir, "local.properties")
    val props = Properties()

    if (localProps.exists()) {
      try {
        FileInputStream(localProps).use { props.load(it) }
      } catch (e: Exception) {
        log.warn("Failed to load local.properties", e)
      }
    }

    val ndk = resolveNdk(requestedNdkVersion)
    if (ndk != null) {
      props.setProperty("ndk.dir", ndk.path.absolutePath)
    }

    val cmakeDir = Environment.getCmakeDirectory(requestedCmakeVersion)
    if (cmakeDir.exists()) {
      props.setProperty("cmake.dir", cmakeDir.absolutePath)
    }

    // Always ensure sdk.dir is set
    props.setProperty("sdk.dir", Environment.ANDROID_HOME.absolutePath)

    return try {
      FileOutputStream(localProps).use { props.store(it, "Configured by AIDE Next") }
      log.info("Updated local.properties with ndk.dir and cmake.dir")
      true
    } catch (e: Exception) {
      log.error("Failed to update local.properties", e)
      false
    }
  }
}
