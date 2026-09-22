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

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.Properties

enum class DetectedProjectType(val displayName: String) {
  ANDROID_GRADLE("Android Gradle Project"),
  ANDROID_COMPOSE("Android Jetpack Compose Project"),
  ANDROID_NDK("Android Native NDK/C++ Project"),
  FLUTTER("Flutter Mobile Project"),
  JAVA_KOTLIN("Java / Kotlin Project"),
  UNKNOWN("Generic Project")
}

data class ProjectRequirements(
  val projectDir: File,
  val projectType: DetectedProjectType,
  val gradleVersion: String?,
  val agpVersion: String?,
  val compileSdk: Int?,
  val minSdk: Int?,
  val targetSdk: Int?,
  val kotlinVersion: String?,
  val requiredJdkVersion: String, // "17" or "21"
  val ndkVersion: String?,
  val cmakeVersion: String?,
  val flutterVersion: String?,
  val dartVersion: String?,
  val hasCompose: Boolean,
  val hasCpp: Boolean,
  val hasFlutter: Boolean,
  val modules: List<String>
)

object ProjectEnvironmentDetector {

  private val log = LoggerFactory.getLogger(ProjectEnvironmentDetector::class.java)

  fun detect(projectDir: File): ProjectRequirements {
    val isFlutter = File(projectDir, "pubspec.yaml").exists()
    if (isFlutter) {
      return detectFlutterProject(projectDir)
    }

    return detectGradleProject(projectDir)
  }

  private fun detectFlutterProject(projectDir: File): ProjectRequirements {
    val pubspec = File(projectDir, "pubspec.yaml")
    var dartSdkConstraint: String? = null
    var flutterVersion: String? = null

    if (pubspec.exists()) {
      pubspec.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("sdk:") && !trimmed.contains("flutter")) {
          dartSdkConstraint = trimmed.substringAfter("sdk:").trim().trim('"', '\'')
        }
        if (trimmed.startsWith("flutter:")) {
          flutterVersion = "SDK"
        }
      }
    }

    val hasAndroidSubDir = File(projectDir, "android").exists()
    var compileSdk: Int? = null
    if (hasAndroidSubDir) {
      val appBuild = File(projectDir, "android/app/build.gradle")
      if (appBuild.exists()) {
        compileSdk = extractSdkInt(appBuild.readText(), "compileSdk")
      }
    }

    return ProjectRequirements(
      projectDir = projectDir,
      projectType = DetectedProjectType.FLUTTER,
      gradleVersion = null,
      agpVersion = null,
      compileSdk = compileSdk ?: 34,
      minSdk = 21,
      targetSdk = 34,
      kotlinVersion = null,
      requiredJdkVersion = "17",
      ndkVersion = null,
      cmakeVersion = null,
      flutterVersion = flutterVersion ?: ">=3.0.0",
      dartVersion = dartSdkConstraint ?: ">=3.0.0",
      hasCompose = false,
      hasCpp = false,
      hasFlutter = true,
      modules = listOf("root", if (hasAndroidSubDir) "android" else "")
    )
  }

  private fun detectGradleProject(projectDir: File): ProjectRequirements {
    var gradleVersion: String? = null
    val wrapperProps = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
    if (wrapperProps.exists()) {
      try {
        val props = Properties()
        FileInputStream(wrapperProps).use { props.load(it) }
        val distUrl = props.getProperty("distributionUrl", "")
        val match = Regex("gradle-([0-9.]+)-").find(distUrl)
        if (match != null) {
          gradleVersion = match.groupValues[1]
        }
      } catch (e: Exception) {
        log.warn("Failed to parse gradle-wrapper.properties", e)
      }
    }

    var agpVersion: String? = null
    var kotlinVersion: String? = null
    var compileSdk: Int? = null
    var minSdk: Int? = null
    var targetSdk: Int? = null
    var ndkVersion: String? = null
    var cmakeVersion: String? = null
    var hasCompose = false
    var hasCpp = false

    // Scan build scripts across modules
    val buildFiles = mutableListOf<File>()
    findBuildFiles(projectDir, buildFiles, 0)

    val modules = mutableListOf<String>()
    for (bf in buildFiles) {
      val content = try { bf.readText() } catch (e: Exception) { "" }
      val moduleName = bf.parentFile.name
      modules.add(moduleName)

      if (agpVersion == null) {
        agpVersion = extractRegex(content, "(?:com\\.android\\.tools\\.build:gradle:|id\\([\"']com\\.android\\.(?:application|library)[\"']\\)\\s*version\\s*[\"'])([0-9.]+)")
      }

      if (kotlinVersion == null) {
        kotlinVersion = extractRegex(content, "(?:org\\.jetbrains\\.kotlin:kotlin-gradle-plugin:|id\\([\"']org\\.jetbrains\\.kotlin\\.android[\"']\\)\\s*version\\s*[\"'])([0-9.]+)")
      }

      if (compileSdk == null) {
        compileSdk = extractSdkInt(content, "compileSdk") ?: extractSdkInt(content, "compileSdkVersion")
      }
      if (minSdk == null) {
        minSdk = extractSdkInt(content, "minSdk") ?: extractSdkInt(content, "minSdkVersion")
      }
      if (targetSdk == null) {
        targetSdk = extractSdkInt(content, "targetSdk") ?: extractSdkInt(content, "targetSdkVersion")
      }

      if (ndkVersion == null) {
        ndkVersion = extractRegex(content, "ndkVersion\\s*(?:=|)\\s*[\"']([0-9.a-zA-Z_-]+)[\"']")
      }

      if (cmakeVersion == null) {
        cmakeVersion = extractRegex(content, "version\\s*[\"']([0-9.]+)[\"']\\s*(?:#.*cmake|//.*cmake|)")
      }

      if (content.contains("compose = true") || content.contains("androidx.compose") || content.contains("compose.compiler")) {
        hasCompose = true
      }

      if (content.contains("externalNativeBuild") || content.contains("CMakeLists.txt") || content.contains("cmake {")) {
        hasCpp = true
      }
    }

    // Also check for C/C++ sources on filesystem
    if (!hasCpp) {
      hasCpp = File(projectDir, "app/src/main/cpp").exists() ||
          File(projectDir, "src/main/cpp").exists() ||
          File(projectDir, "CMakeLists.txt").exists() ||
          File(projectDir, "app/CMakeLists.txt").exists()
    }

    val reqJdk = if (gradleVersion != null && isGradle84OrHigher(gradleVersion)) {
      "21" // Gradle 8.4+ supports JDK 21
    } else {
      "17" // Default AGP 8 LTS JDK
    }

    val projectType = when {
      hasCompose -> DetectedProjectType.ANDROID_COMPOSE
      hasCpp -> DetectedProjectType.ANDROID_NDK
      agpVersion != null || compileSdk != null -> DetectedProjectType.ANDROID_GRADLE
      else -> DetectedProjectType.JAVA_KOTLIN
    }

    return ProjectRequirements(
      projectDir = projectDir,
      projectType = projectType,
      gradleVersion = gradleVersion,
      agpVersion = agpVersion,
      compileSdk = compileSdk ?: 34,
      minSdk = minSdk ?: 21,
      targetSdk = targetSdk ?: 34,
      kotlinVersion = kotlinVersion,
      requiredJdkVersion = reqJdk,
      ndkVersion = ndkVersion,
      cmakeVersion = cmakeVersion,
      flutterVersion = null,
      dartVersion = null,
      hasCompose = hasCompose,
      hasCpp = hasCpp,
      hasFlutter = false,
      modules = modules.distinct()
    )
  }

  private fun isGradle84OrHigher(version: String): Boolean {
    val parts = version.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.isEmpty()) return false
    if (parts[0] > 8) return true
    if (parts[0] == 8 && parts.size > 1 && parts[1] >= 4) return true
    return false
  }

  private fun extractSdkInt(content: String, key: String): Int? {
    val regex = Regex("$key\\s*(?:=|)\\s*(\\d+)")
    return regex.find(content)?.groupValues?.get(1)?.toIntOrNull()
  }

  private fun extractRegex(content: String, pattern: String): String? {
    val regex = Regex(pattern)
    return regex.find(content)?.groupValues?.get(1)
  }

  private fun findBuildFiles(dir: File, result: MutableList<File>, depth: Int) {
    if (depth > 4) return
    val files = dir.listFiles() ?: return
    for (f in files) {
      if (f.isDirectory && !f.name.startsWith(".") && f.name != "build") {
        findBuildFiles(f, result, depth + 1)
      } else if (f.isFile && (f.name == "build.gradle" || f.name == "build.gradle.kts")) {
        result.add(f)
      }
    }
  }
}
