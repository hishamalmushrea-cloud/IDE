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

import com.aidenext.build.ArtifactKind
import com.aidenext.build.BuildArtifact
import com.aidenext.build.BuildExecutionResult
import com.aidenext.build.BuildType
import com.aidenext.utils.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

object FlutterManager {

  private val log = LoggerFactory.getLogger(FlutterManager::class.java)

  fun getFlutterExecutable(): File? {
    val status = FlutterToolchainProvider.inspectToolchain()
    if (status.isInstalled && status.flutterHome != null) {
      val bin = File(status.flutterHome, "bin/flutter")
      if (bin.exists()) return bin
    }
    val defaultBin = File(Environment.FLUTTER_HOME, "bin/flutter")
    if (defaultBin.exists()) return defaultBin
    val sysBin = File(Environment.BIN_DIR, "flutter")
    if (sysBin.exists()) return sysBin
    return null
  }

  fun getDartExecutable(): File? {
    val status = FlutterToolchainProvider.inspectToolchain()
    if (status.isInstalled && status.dartHome != null) {
      val bin = File(status.dartHome, "bin/dart")
      if (bin.exists()) return bin
    }
    val defaultDart = File(Environment.FLUTTER_HOME, "bin/cache/dart-sdk/bin/dart")
    if (defaultDart.exists()) return defaultDart
    val sysDart = File(Environment.BIN_DIR, "dart")
    if (sysDart.exists()) return sysDart
    return null
  }

  fun isFlutterProject(dir: File): Boolean {
    return File(dir, "pubspec.yaml").exists()
  }

  fun parsePubspec(projectDir: File): PubspecModel? {
    return PubspecParser.parse(File(projectDir, "pubspec.yaml"))
  }

  fun formatDartSource(source: String): String {
    return DartSourceFormatter.format(source)
  }

  suspend fun runFlutterCommand(
    projectDir: File,
    commandArgs: List<String>,
    onOutputLine: (String) -> Unit
  ): Pair<Int, String> = withContext(Dispatchers.IO) {
    val status = FlutterToolchainProvider.inspectToolchain()
    if (!status.isInstalled) {
      onOutputLine("Error: Flutter SDK not found. Install Flutter or set up Linux container.")
      return@withContext -1 to "Flutter SDK not found"
    }

    val result = FlutterExecutor.activeBackend.executeCommand(
      projectDir = projectDir,
      args = commandArgs,
      env = FlutterEnvironment.defaultEnvironment(),
      onOutputLine = onOutputLine
    )

    result.exitCode to (result.stdout.ifBlank { result.stderr })
  }

  suspend fun buildApk(
    projectDir: File,
    mode: String = "debug", // debug, release, profile
    onOutputLine: (String) -> Unit
  ): BuildExecutionResult {
    val start = System.currentTimeMillis()
    val result = FlutterExecutor.buildApk(projectDir, mode = mode, onOutputLine = onOutputLine)
    val duration = System.currentTimeMillis() - start
    val isSuccess = result.isSuccess

    val artifacts = mutableListOf<BuildArtifact>()
    if (isSuccess) {
      val apkDir = File(projectDir, "build/app/outputs/flutter-apk")
      if (apkDir.exists()) {
        apkDir.listFiles()?.filter { it.extension == "apk" }?.forEach { f ->
          artifacts.add(
            BuildArtifact(
              kind = ArtifactKind.APK,
              file = f,
              variant = mode,
              isSigned = mode == "debug" || !f.name.contains("unsigned")
            )
          )
        }
      }
    }

    val out = result.stdout + "\n" + result.stderr

    return BuildExecutionResult(
      buildType = BuildType.FLUTTER_BUILD_APK,
      isSuccessful = isSuccess,
      exitCode = result.exitCode,
      durationMillis = duration,
      tasksExecuted = listOf(result.command),
      artifacts = artifacts,
      errors = if (!isSuccess) com.aidenext.build.errors.BuildErrorParser.parse(out) else emptyList(),
      warnings = emptyList(),
      stdout = result.stdout,
      stderr = result.stderr
    )
  }

  suspend fun buildAppBundle(
    projectDir: File,
    onOutputLine: (String) -> Unit
  ): BuildExecutionResult {
    val start = System.currentTimeMillis()
    val result = FlutterExecutor.buildAppBundle(projectDir, onOutputLine = onOutputLine)
    val duration = System.currentTimeMillis() - start
    val isSuccess = result.isSuccess

    val artifacts = mutableListOf<BuildArtifact>()
    if (isSuccess) {
      val bundleDir = File(projectDir, "build/app/outputs/bundle/release")
      if (bundleDir.exists()) {
        bundleDir.listFiles()?.filter { it.extension == "aab" }?.forEach { f ->
          artifacts.add(
            BuildArtifact(
              kind = ArtifactKind.AAB,
              file = f,
              variant = "release",
              isSigned = !f.name.contains("unsigned")
            )
          )
        }
      }
    }

    val out = result.stdout + "\n" + result.stderr

    return BuildExecutionResult(
      buildType = BuildType.FLUTTER_BUILD_AAB,
      isSuccessful = isSuccess,
      exitCode = result.exitCode,
      durationMillis = duration,
      tasksExecuted = listOf(result.command),
      artifacts = artifacts,
      errors = if (!isSuccess) com.aidenext.build.errors.BuildErrorParser.parse(out) else emptyList(),
      warnings = emptyList(),
      stdout = result.stdout,
      stderr = result.stderr
    )
  }
}
