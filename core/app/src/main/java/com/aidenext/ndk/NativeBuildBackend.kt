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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class NativeBuildResult(
  val isSuccess: Boolean,
  val exitCode: Int,
  val output: String,
  val generatedLibraries: List<File>
)

/**
 * Backend providing direct CMake and Ninja configuration and compilation for native C/C++ targets.
 */
object NativeBuildBackend {

  private val log = LoggerFactory.getLogger(NativeBuildBackend::class.java)

  /**
   * Configures CMake for the native source directory.
   */
  suspend fun configureCMake(
    sourceDir: File,
    buildDir: File,
    abi: AndroidAbi = AndroidAbi.ARM64_V8A,
    onOutputLine: ((String) -> Unit)? = null
  ): NativeBuildResult = withContext(Dispatchers.IO) {
    buildDir.mkdirs()

    val cmakeBin = File(Environment.PREFIX, "bin/cmake")
    val ninjaBin = File(Environment.PREFIX, "bin/ninja")

    val cmd = mutableListOf(
      cmakeBin.absolutePath,
      "-GNinja",
      "-B", buildDir.absolutePath,
      "-S", sourceDir.absolutePath,
      "-DCMAKE_MAKE_PROGRAM=${ninjaBin.absolutePath}",
      "-DANDROID_ABI=${abi.abiString}"
    )

    executeProcess(cmd, buildDir, onOutputLine)
  }

  /**
   * Compiles native C/C++ targets using Ninja.
   */
  suspend fun buildWithNinja(
    buildDir: File,
    targetName: String? = null,
    onOutputLine: ((String) -> Unit)? = null
  ): NativeBuildResult = withContext(Dispatchers.IO) {
    val ninjaBin = File(Environment.PREFIX, "bin/ninja")
    val cmd = mutableListOf(ninjaBin.absolutePath, "-C", buildDir.absolutePath)
    if (!targetName.isNullOrBlank()) {
      cmd.add(targetName)
    }

    val res = executeProcess(cmd, buildDir, onOutputLine)
    val libraries = mutableListOf<File>()
    if (res.isSuccess) {
      buildDir.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach {
        libraries.add(it)
      }
    }

    NativeBuildResult(
      isSuccess = res.isSuccess,
      exitCode = res.exitCode,
      output = res.output,
      generatedLibraries = libraries
    )
  }

  private fun executeProcess(
    command: List<String>,
    workingDir: File,
    onOutputLine: ((String) -> Unit)?
  ): NativeBuildResult {
    val pb = ProcessBuilder(command)
    pb.directory(workingDir)
    val env = HashMap<String, String>()
    Environment.putEnvironment(env, false)
    pb.environment().putAll(env)

    val out = StringBuilder()
    return try {
      val process = pb.start()
      val reader = BufferedReader(InputStreamReader(process.inputStream))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        out.appendLine(line)
        onOutputLine?.invoke(line!!)
      }

      val errReader = BufferedReader(InputStreamReader(process.errorStream))
      while (errReader.readLine().also { line = it } != null) {
        out.appendLine(line)
        onOutputLine?.invoke(line!!)
      }

      val code = process.waitFor()
      NativeBuildResult(code == 0, code, out.toString(), emptyList())
    } catch (e: Exception) {
      log.error("Failed executing native command: {}", command, e)
      NativeBuildResult(false, -1, "${e.message}\n$out", emptyList())
    }
  }
}
