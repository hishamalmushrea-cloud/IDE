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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Result of a Flutter command execution.
 */
data class FlutterExecutionResult(
  val exitCode: Int,
  val isSuccess: Boolean,
  val stdout: String,
  val stderr: String,
  val command: String
)

/**
 * Interface for interchangeable Flutter execution backends on Android.
 */
interface FlutterBuildBackend {

  val backendName: String

  /**
   * Executes a generic Flutter command in the given project directory.
   */
  suspend fun executeCommand(
    projectDir: File,
    args: List<String>,
    env: FlutterEnvironment,
    onOutputLine: ((String) -> Unit)? = null
  ): FlutterExecutionResult
}

/**
 * Native execution backend running directly against the Termux / Android Bionic environment.
 */
class TermuxNativeBackend : FlutterBuildBackend {

  override val backendName: String = "Termux Native (Bionic)"

  companion object {
    private val log = LoggerFactory.getLogger(TermuxNativeBackend::class.java)
  }

  override suspend fun executeCommand(
    projectDir: File,
    args: List<String>,
    env: FlutterEnvironment,
    onOutputLine: ((String) -> Unit)?
  ): FlutterExecutionResult = withContext(Dispatchers.IO) {
    val cmd = mutableListOf<String>()
    cmd.add(env.flutterExecutable.absolutePath)
    cmd.addAll(args)

    val pb = ProcessBuilder(cmd)
    pb.directory(projectDir)
    pb.environment().putAll(env.createEnvironmentMap())

    val fullOutput = StringBuilder()
    val fullError = StringBuilder()

    try {
      val process = pb.start()
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        val l = line!!
        fullOutput.appendLine(l)
        onOutputLine?.invoke(l)
      }

      val errReader = BufferedReader(InputStreamReader(process.errorStream))
      while (errReader.readLine().also { line = it } != null) {
        val l = line!!
        fullError.appendLine(l)
        onOutputLine?.invoke("[ERR] $l")
      }

      val exitCode = process.waitFor()
      FlutterExecutionResult(
        exitCode = exitCode,
        isSuccess = exitCode == 0,
        stdout = fullOutput.toString(),
        stderr = fullError.toString(),
        command = cmd.joinToString(" ")
      )
    } catch (e: Exception) {
      log.error("Failed executing flutter command on Termux backend: ${cmd.joinToString(" ")}", e)
      FlutterExecutionResult(
        exitCode = -1,
        isSuccess = false,
        stdout = fullOutput.toString(),
        stderr = "${e.message}\n$fullError",
        command = cmd.joinToString(" ")
      )
    }
  }
}

/**
 * PRoot-distro Linux container execution backend allowing standard glibc Linux ARM64 Flutter SDK.
 */
class ProotLinuxBackend : FlutterBuildBackend {

  override val backendName: String = "PRoot Linux Container (Glibc)"

  companion object {
    private val log = LoggerFactory.getLogger(ProotLinuxBackend::class.java)
  }

  override suspend fun executeCommand(
    projectDir: File,
    args: List<String>,
    env: FlutterEnvironment,
    onOutputLine: ((String) -> Unit)?
  ): FlutterExecutionResult = withContext(Dispatchers.IO) {
    // proot-distro login debian -- bash -c "cd /project && flutter ..."
    val cmd = mutableListOf(
      "proot-distro",
      "login",
      "debian",
      "--",
      "bash",
      "-c",
      "cd '${projectDir.absolutePath}' && flutter ${args.joinToString(" ")}"
    )

    val pb = ProcessBuilder(cmd)
    pb.directory(projectDir)

    val fullOutput = StringBuilder()
    val fullError = StringBuilder()

    try {
      val process = pb.start()
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        val l = line!!
        fullOutput.appendLine(l)
        onOutputLine?.invoke(l)
      }

      val errReader = BufferedReader(InputStreamReader(process.errorStream))
      while (errReader.readLine().also { line = it } != null) {
        val l = line!!
        fullError.appendLine(l)
        onOutputLine?.invoke("[ERR] $l")
      }

      val exitCode = process.waitFor()
      FlutterExecutionResult(
        exitCode = exitCode,
        isSuccess = exitCode == 0,
        stdout = fullOutput.toString(),
        stderr = fullError.toString(),
        command = cmd.joinToString(" ")
      )
    } catch (e: Exception) {
      log.error("Failed executing proot flutter command: ${cmd.joinToString(" ")}", e)
      FlutterExecutionResult(
        exitCode = -1,
        isSuccess = false,
        stdout = fullOutput.toString(),
        stderr = "${e.message}\n$fullError",
        command = cmd.joinToString(" ")
      )
    }
  }
}
