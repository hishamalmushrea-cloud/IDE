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
import com.aidenext.utils.JdkDistribution
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Isolated toolchain context for an individual project.
 * Ensures that Project A (e.g., JDK 17, NDK 27, CMake 3.22) and Project B (e.g., JDK 21, NDK 28, CMake 3.26)
 * maintain strictly isolated build and runtime environments without cross-project interference.
 */
data class ProjectEnvironmentContext(
  val projectDir: File,
  val jdkDistribution: JdkDistribution = JdkDistribution.JDK_17,
  val ndkVersion: String? = null,
  val cmakeVersion: String? = null,
  val flutterHome: File? = null,
  val dartHome: File? = null
) {

  fun toEnvironmentMap(): MutableMap<String, String> {
    val env = mutableMapOf<String, String>()

    // Project-specific JDK
    val jdkHome = File(Environment.PREFIX, "opt/openjdk-${jdkDistribution.version}")
    if (jdkHome.exists()) {
      env["JAVA_HOME"] = jdkHome.absolutePath
    } else {
      env["JAVA_HOME"] = Environment.JAVA_HOME.absolutePath
    }

    // Android SDK
    env["ANDROID_HOME"] = Environment.ANDROID_HOME.absolutePath
    env["ANDROID_SDK_ROOT"] = Environment.ANDROID_HOME.absolutePath

    // Project-specific NDK
    if (!ndkVersion.isNullOrBlank()) {
      val specificNdk = File(Environment.NDK_HOME, ndkVersion)
      if (specificNdk.exists()) {
        env["NDK_HOME"] = specificNdk.absolutePath
        env["ANDROID_NDK_HOME"] = specificNdk.absolutePath
        env["ANDROID_NDK_ROOT"] = specificNdk.absolutePath
      }
    }

    // Project-specific CMake
    if (!cmakeVersion.isNullOrBlank()) {
      val specificCmake = File(Environment.CMAKE_HOME, cmakeVersion)
      if (specificCmake.exists()) {
        env["CMAKE_HOME"] = specificCmake.absolutePath
      }
    }

    // Project-specific Flutter / Dart
    flutterHome?.let {
      env["FLUTTER_HOME"] = it.absolutePath
      env["FLUTTER_ROOT"] = it.absolutePath
    }
    dartHome?.let {
      env["DART_HOME"] = it.absolutePath
      env["DART_ROOT"] = it.absolutePath
    }

    // Isolated Gradle user home per project or shared cache
    env["GRADLE_USER_HOME"] = Environment.GRADLE_USER_HOME.absolutePath
    env["PROJECT_ROOT"] = projectDir.absolutePath

    return env
  }

  companion object {
    private val log = LoggerFactory.getLogger(ProjectEnvironmentContext::class.java)
    private val contexts = ConcurrentHashMap<String, ProjectEnvironmentContext>()

    /**
     * Resolves or loads the isolated [ProjectEnvironmentContext] for a project directory.
     */
    fun forProject(projectDir: File): ProjectEnvironmentContext {
      val key = projectDir.canonicalPath
      return contexts.computeIfAbsent(key) {
        detectAndBuild(projectDir)
      }
    }

    /**
     * Updates or sets an explicit context for a project.
     */
    fun setForProject(projectDir: File, context: ProjectEnvironmentContext) {
      contexts[projectDir.canonicalPath] = context
    }

    /**
     * Cleans up stored context when a project is closed.
     */
    fun removeProject(projectDir: File) {
      contexts.remove(projectDir.canonicalPath)
    }

    private fun detectAndBuild(projectDir: File): ProjectEnvironmentContext {
      val req = ProjectEnvironmentDetector.detect(projectDir)

      // Read local.properties if present
      val localProps = File(projectDir, "local.properties")
      var ndkFromProps: String? = null
      var cmakeFromProps: String? = null
      if (localProps.exists()) {
        try {
          val props = Properties()
          FileInputStream(localProps).use { props.load(it) }
          ndkFromProps = props.getProperty("ndk.dir")?.let { File(it).name }
          cmakeFromProps = props.getProperty("cmake.dir")?.let { File(it).name }
        } catch (_: Exception) {}
      }

      val jdk = when (req.requiredJdkVersion) {
        "21" -> JdkDistribution.JDK_21
        else -> JdkDistribution.JDK_17
      }

      return ProjectEnvironmentContext(
        projectDir = projectDir,
        jdkDistribution = jdk,
        ndkVersion = req.ndkVersion ?: ndkFromProps,
        cmakeVersion = req.cmakeVersion ?: cmakeFromProps,
        flutterHome = if (req.hasFlutter) Environment.resolveFlutterHome() else null,
        dartHome = if (req.hasFlutter) Environment.resolveDartHome() else null
      )
    }
  }
}
