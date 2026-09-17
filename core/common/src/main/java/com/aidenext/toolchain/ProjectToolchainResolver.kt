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

enum class ResolutionStatus {
  READY,
  MISSING_REQUIRED,
  WARNING_VERSION_MISMATCH
}

data class ToolchainRequirementCheck(
  val name: String,
  val requiredVersion: String,
  val isSatisfied: Boolean,
  val statusIcon: String, // "✓", "✗", "⚠"
  val detail: String,
  val actionHint: String? = null
)

data class ProjectDoctorReport(
  val projectType: DetectedProjectType,
  val overallStatus: ResolutionStatus,
  val checks: List<ToolchainRequirementCheck>,
  val missingComponents: List<String>,
  val warningComponents: List<String>
)

object ProjectToolchainResolver {

  fun resolve(requirements: ProjectRequirements): ProjectDoctorReport {
    val checks = mutableListOf<ToolchainRequirementCheck>()
    val missing = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    val allTools = ToolchainManager.getInstalledToolchains()

    // 1. Check JDK
    val installedJdks = allTools.filter { it.type == ToolchainType.JDK && it.isInstalled }
    val matchingJdk = installedJdks.find { it.version.startsWith(requirements.requiredJdkVersion) }
    if (matchingJdk != null) {
      checks.add(
        ToolchainRequirementCheck(
          name = "JDK Requirement",
          requiredVersion = "JDK ${requirements.requiredJdkVersion}",
          isSatisfied = true,
          statusIcon = "✓",
          detail = "Found ${matchingJdk.name} at ${matchingJdk.path?.absolutePath}"
        )
      )
    } else if (installedJdks.isNotEmpty()) {
      checks.add(
        ToolchainRequirementCheck(
          name = "JDK Requirement",
          requiredVersion = "JDK ${requirements.requiredJdkVersion}",
          isSatisfied = false,
          statusIcon = "⚠",
          detail = "Project prefers JDK ${requirements.requiredJdkVersion}, but found ${installedJdks[0].name}",
          actionHint = "Install OpenJDK ${requirements.requiredJdkVersion} via Tools Manager"
        )
      )
      warnings.add("JDK ${requirements.requiredJdkVersion} mismatch")
    } else {
      checks.add(
        ToolchainRequirementCheck(
          name = "JDK Requirement",
          requiredVersion = "JDK ${requirements.requiredJdkVersion}",
          isSatisfied = false,
          statusIcon = "✗",
          detail = "No OpenJDK installed",
          actionHint = "Install OpenJDK ${requirements.requiredJdkVersion}"
        )
      )
      missing.add("JDK ${requirements.requiredJdkVersion}")
    }

    if (requirements.hasFlutter) {
      // 2. Flutter SDK check
      val flutterTool = allTools.find { it.type == ToolchainType.FLUTTER }
      if (flutterTool?.isInstalled == true) {
        checks.add(
          ToolchainRequirementCheck(
            name = "Flutter SDK",
            requiredVersion = requirements.flutterVersion ?: ">=3.0.0",
            isSatisfied = true,
            statusIcon = "✓",
            detail = "Flutter SDK ready at ${flutterTool.path?.absolutePath}"
          )
        )
      } else {
        checks.add(
          ToolchainRequirementCheck(
            name = "Flutter SDK",
            requiredVersion = requirements.flutterVersion ?: ">=3.0.0",
            isSatisfied = false,
            statusIcon = "✗",
            detail = "Flutter SDK is not installed",
            actionHint = "Install Flutter SDK via Toolchain Manager"
          )
        )
        missing.add("Flutter SDK")
      }
    } else {
      // Android SDK checks
      val compileSdkStr = requirements.compileSdk?.toString() ?: "34"
      val platform = allTools.find { it.type == ToolchainType.ANDROID_SDK && it.id == "platform-$compileSdkStr" }
      if (platform?.isInstalled == true) {
        checks.add(
          ToolchainRequirementCheck(
            name = "Android SDK Platform",
            requiredVersion = "API $compileSdkStr",
            isSatisfied = true,
            statusIcon = "✓",
            detail = "Platform android-$compileSdkStr is installed"
          )
        )
      } else {
        checks.add(
          ToolchainRequirementCheck(
            name = "Android SDK Platform",
            requiredVersion = "API $compileSdkStr",
            isSatisfied = false,
            statusIcon = "✗",
            detail = "Missing Android SDK Platform $compileSdkStr",
            actionHint = "Install Android SDK Platform $compileSdkStr in SDK Manager"
          )
        )
        missing.add("Platform android-$compileSdkStr")
      }

      // Check NDK if native
      if (requirements.hasCpp) {
        val reqNdk = requirements.ndkVersion ?: "Default NDK"
        val ndkInstalled = allTools.find { it.type == ToolchainType.ANDROID_NDK && it.isInstalled }
        if (ndkInstalled != null) {
          checks.add(
            ToolchainRequirementCheck(
              name = "Android NDK",
              requiredVersion = reqNdk,
              isSatisfied = true,
              statusIcon = "✓",
              detail = "Found ${ndkInstalled.name}"
            )
          )
        } else {
          checks.add(
            ToolchainRequirementCheck(
              name = "Android NDK",
              requiredVersion = reqNdk,
              isSatisfied = false,
              statusIcon = "✗",
              detail = "Native project requires NDK $reqNdk",
              actionHint = "Install Android NDK via SDK Manager"
            )
          )
          missing.add("Android NDK ($reqNdk)")
        }

        // Check CMake
        val reqCmake = requirements.cmakeVersion ?: "3.22+"
        val cmakeInstalled = allTools.find { it.type == ToolchainType.CMAKE && it.isInstalled }
        if (cmakeInstalled != null) {
          checks.add(
            ToolchainRequirementCheck(
              name = "CMake",
              requiredVersion = reqCmake,
              isSatisfied = true,
              statusIcon = "✓",
              detail = "Found ${cmakeInstalled.name}"
            )
          )
        } else {
          checks.add(
            ToolchainRequirementCheck(
              name = "CMake",
              requiredVersion = reqCmake,
              isSatisfied = false,
              statusIcon = "✗",
              detail = "Native project requires CMake",
              actionHint = "Install CMake in SDK Manager"
            )
          )
          missing.add("CMake")
        }
      }
    }

    val overallStatus = when {
      missing.isNotEmpty() -> ResolutionStatus.MISSING_REQUIRED
      warnings.isNotEmpty() -> ResolutionStatus.WARNING_VERSION_MISMATCH
      else -> ResolutionStatus.READY
    }

    return ProjectDoctorReport(
      projectType = requirements.projectType,
      overallStatus = overallStatus,
      checks = checks,
      missingComponents = missing,
      warningComponents = warnings
    )
  }
}
