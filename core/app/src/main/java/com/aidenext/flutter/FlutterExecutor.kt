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

import org.slf4j.LoggerFactory
import java.io.File

/**
 * High-level executor orchestrating Flutter operations across active backends.
 */
object FlutterExecutor {

  private val log = LoggerFactory.getLogger(FlutterExecutor::class.java)

  var activeBackend: FlutterBuildBackend = TermuxNativeBackend()
    private set

  fun selectBackend(backend: FlutterBuildBackend) {
    activeBackend = backend
    log.info("Active Flutter build backend switched to: ${backend.backendName}")
  }

  suspend fun runDoctor(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    return activeBackend.executeCommand(projectDir, listOf("doctor", "-v"), env, onOutputLine)
  }

  suspend fun runPubGet(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    return activeBackend.executeCommand(projectDir, listOf("pub", "get"), env, onOutputLine)
  }

  suspend fun runClean(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    return activeBackend.executeCommand(projectDir, listOf("clean"), env, onOutputLine)
  }

  suspend fun runAnalyze(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    return activeBackend.executeCommand(projectDir, listOf("analyze"), env, onOutputLine)
  }

  suspend fun runTest(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    return activeBackend.executeCommand(projectDir, listOf("test"), env, onOutputLine)
  }

  suspend fun buildApk(
    projectDir: File,
    mode: String = "debug", // debug, release, profile
    targetPlatform: String = "android-arm64",
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    val args = mutableListOf("build", "apk", "--$mode")
    if (targetPlatform.isNotBlank()) {
      args.add("--target-platform")
      args.add(targetPlatform)
    }
    // Bypass tree shaking on ARM64 JIT if release
    if (mode == "release") {
      args.add("--no-tree-shake-icons")
    }
    return activeBackend.executeCommand(projectDir, args, env, onOutputLine)
  }

  suspend fun buildAppBundle(
    projectDir: File,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult {
    val env = resolveEnvironment()
    val args = listOf("build", "appbundle", "--no-tree-shake-icons")
    return activeBackend.executeCommand(projectDir, args, env, onOutputLine)
  }

  private fun resolveEnvironment(): FlutterEnvironment {
    val status = FlutterToolchainProvider.inspectToolchain()
    if (status.isInstalled && status.flutterHome != null) {
      if (status.libcType == FlutterLibcType.GLIBC && activeBackend !is ProotLinuxBackend) {
        selectBackend(ProotLinuxBackend())
      }
      return FlutterEnvironment(
        flutterHome = status.flutterHome,
        dartHome = status.dartHome,
        libcType = status.libcType
      )
    }
    return FlutterEnvironment.defaultEnvironment()
  }
}
