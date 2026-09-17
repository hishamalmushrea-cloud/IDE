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

package com.aidenext.build

import com.aidenext.build.errors.BuildErrorParser
import com.aidenext.lookup.Lookup
import com.aidenext.projects.android.AndroidModule
import com.aidenext.projects.builder.BuildService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

object BuildManager {

  private val log = LoggerFactory.getLogger(BuildManager::class.java)

  private val activeLogs = StringBuilder()

  fun onBuildOutput(line: String) {
    synchronized(activeLogs) {
      activeLogs.appendLine(line)
    }
  }

  fun executeBuild(
    projectDir: File,
    targetModule: AndroidModule?,
    buildType: BuildType,
    onFinished: (BuildExecutionResult) -> Unit
  ) {
    synchronized(activeLogs) {
      activeLogs.clear()
    }

    val tasks = mutableListOf<String>()
    val prefix = if (targetModule != null) "${targetModule.path}:" else ""
    for (t in buildType.defaultTasks) {
      tasks.add(if (prefix.isNotEmpty() && !t.startsWith(":")) "$prefix$t" else t)
    }

    val startTime = System.currentTimeMillis()
    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)

    CoroutineScope(Dispatchers.IO).launch {
      val isSuccess = if (buildService != null) {
        val future = buildService.executeTasks(*tasks.toTypedArray())
        val result = try {
          future.get()
        } catch (e: Exception) {
          log.error("Build failed", e)
          null
        }
        result?.isSuccessful == true
      } else {
        false
      }

      val duration = System.currentTimeMillis() - startTime

      val logText = synchronized(activeLogs) { activeLogs.toString() }
      val artifacts = if (isSuccess) {
        ArtifactLocator.locateArtifacts(projectDir, targetModule, buildType)
      } else emptyList()

      val errors = if (!isSuccess) {
        BuildErrorParser.parse(logText)
      } else emptyList()

      val result = BuildExecutionResult(
        buildType = buildType,
        isSuccessful = isSuccess,
        exitCode = if (isSuccess) 0 else 1,
        durationMillis = duration,
        tasksExecuted = tasks,
        artifacts = artifacts,
        errors = errors,
        stdout = logText,
        stderr = if (!isSuccess) logText else ""
      )

      withContext(Dispatchers.Main) {
        onFinished(result)
      }
    }
  }
}
