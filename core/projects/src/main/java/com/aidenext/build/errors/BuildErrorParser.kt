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

package com.aidenext.build.errors

import java.util.regex.Pattern

data class ParsedBuildError(
  val problem: String,
  val cause: String? = null,
  val requiredComponent: String? = null,
  val suggestedAction: String? = null
)

object BuildErrorParser {

  private val PATTERN_MISSING_PLATFORM = Pattern.compile("Failed to find target with hash string 'android-(\\d+)'")
  private val PATTERN_MISSING_BUILD_TOOLS = Pattern.compile("Failed to find Build Tools revision ([0-9.]+)")
  private val PATTERN_JAVA_VERSION = Pattern.compile("Unsupported Java version: (\\d+)|Android Gradle plugin requires Java (\\d+)")
  private val PATTERN_CMAKE_MISSING = Pattern.compile("CMake '([0-9.]+)' was not found")
  private val PATTERN_NDK_MISSING = Pattern.compile("NDK not configured|No version of NDK was found")
  private val PATTERN_OUT_OF_MEMORY = Pattern.compile("OutOfMemoryError|Java heap space|Expiring Daemon because JVM heap space is exhausted")
  private val PATTERN_DEP_RESOLUTION = Pattern.compile("Could not resolve ([a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+)")

  fun parse(buildLog: String): List<ParsedBuildError> {
    val results = mutableListOf<ParsedBuildError>()

    // Missing platform
    val platformMatcher = PATTERN_MISSING_PLATFORM.matcher(buildLog)
    if (platformMatcher.find()) {
      val api = platformMatcher.group(1)
      results.add(
        ParsedBuildError(
          problem = "Missing Android SDK Platform $api",
          cause = "The project requires Android API level $api, which is not installed in your SDK.",
          requiredComponent = "platforms;android-$api",
          suggestedAction = "Open SDK Manager and install Platform $api, or run 'idesetup' in terminal."
        )
      )
    }

    // Missing build tools
    val btMatcher = PATTERN_MISSING_BUILD_TOOLS.matcher(buildLog)
    if (btMatcher.find()) {
      val version = btMatcher.group(1)
      results.add(
        ParsedBuildError(
          problem = "Missing Android SDK Build-Tools $version",
          cause = "The project requires Build-Tools revision $version.",
          requiredComponent = "build-tools;$version",
          suggestedAction = "Install Build-Tools $version in SDK Manager."
        )
      )
    }

    // Java incompatibility
    val javaMatcher = PATTERN_JAVA_VERSION.matcher(buildLog)
    if (javaMatcher.find()) {
      results.add(
        ParsedBuildError(
          problem = "Java Version Incompatibility",
          cause = "The current AGP version is incompatible with the active Java runtime.",
          suggestedAction = "Switch between OpenJDK 17 and OpenJDK 21 in Settings or Project Doctor."
        )
      )
    }

    // Missing CMake
    val cmakeMatcher = PATTERN_CMAKE_MISSING.matcher(buildLog)
    if (cmakeMatcher.find()) {
      val v = cmakeMatcher.group(1)
      results.add(
        ParsedBuildError(
          problem = "CMake Not Found",
          cause = "CMake $v was not found in PATH or cmake.dir.",
          requiredComponent = "cmake;$v",
          suggestedAction = "Install CMake using SDK Manager or 'pkg install cmake ninja'."
        )
      )
    }

    // Missing NDK
    val ndkMatcher = PATTERN_NDK_MISSING.matcher(buildLog)
    if (ndkMatcher.find()) {
      results.add(
        ParsedBuildError(
          problem = "Android NDK Not Configured",
          cause = "Native C++ build requested but NDK is missing.",
          suggestedAction = "Run 'idesetup -y --with-ndk' or configure ndk.dir in local.properties."
        )
      )
    }

    // Out of memory
    val oomMatcher = PATTERN_OUT_OF_MEMORY.matcher(buildLog)
    if (oomMatcher.find()) {
      results.add(
        ParsedBuildError(
          problem = "Build Daemon Out Of Memory",
          cause = "Gradle daemon exhausted its allocated JVM heap space.",
          suggestedAction = "Increase org.gradle.jvmargs in gradle.properties (e.g. -Xmx2048m)."
        )
      )
    }

    // Dependency resolution failure
    val depMatcher = PATTERN_DEP_RESOLUTION.matcher(buildLog)
    if (depMatcher.find()) {
      val dep = depMatcher.group(1)
      results.add(
        ParsedBuildError(
          problem = "Dependency Resolution Failure",
          cause = "Could not download or resolve $dep.",
          requiredComponent = dep,
          suggestedAction = "Check your internet connection and verify maven repositories in settings.gradle."
        )
      )
    }

    return results
  }
}
