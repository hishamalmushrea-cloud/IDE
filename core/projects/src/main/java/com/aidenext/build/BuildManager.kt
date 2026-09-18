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
 *  along with AIDE Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.build

import com.aidenext.build.errors.BuildErrorParser
import com.aidenext.lookup.Lookup
import com.aidenext.projects.GradleProject
import com.aidenext.projects.android.AndroidModule
import com.aidenext.projects.builder.BuildService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Executes build tasks for a project and collects the output artifacts and errors.
 *
 * This is a thin wrapper around the [BuildService] which is provided by the tooling API. It
 * provides a simplified, callback based API for the IDE's build actions.
 *
 * @author AIDE Next
 */
object BuildManager {

  private val log = LoggerFactory.getLogger(BuildManager::class.java)

  private val activeLogs = StringBuilder()

  /**
   * Appends the given [line] to the log of the build which is currently being executed.
   */
  @JvmStatic
  fun onBuildOutput(line: String) {
    synchronized(activeLogs) {
      activeLogs.appendLine(line)
    }
  }

  /**
   * Returns the log of the build which is currently being executed (or the last executed build).
   */
  @JvmStatic
  fun currentBuildLog(): String = synchronized(activeLogs) { activeLogs.toString() }

  /**
   * Executes the tasks of the given [buildType] and invokes [onFinished] with the result of the
   * build. The callback is always invoked on the main thread.
   *
   * @param project The project for which the tasks must be executed.
   * @param targetModule The module on which the tasks must be executed. If `null`, then the tasks
   *   are executed in the root project.
   * @param buildType The type of the build. This determines the tasks which are executed.
   * @param onFinished Invoked when the build finishes.
   */
  @JvmStatic
  fun executeBuild(
    project: GradleProject,
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
      var isSuccess = false
      var failure: String? = null

      if (buildService == null) {
        failure = "Build service is not available. Is a project open?"
        log.error(failure)
      } else if (tasks.isEmpty()) {
        failure = "No tasks are configured for the build type '${buildType.displayName}'."
        log.error(failure)
      } else {
        try {
          val result = buildService.executeTasks(*tasks.toTypedArray()).get()
          isSuccess = result.isSuccessful
          failure = result.failure?.name
        } catch (e: Exception) {
          failure = e.message ?: e.javaClass.simpleName
          log.error("Failed to execute build tasks: {}", tasks, e)
        }
      }

      val duration = System.currentTimeMillis() - startTime
      val logText = synchronized(activeLogs) { activeLogs.toString() }
      val artifacts = if (isSuccess) {
        runCatching { ArtifactLocator.locateArtifacts(project, targetModule, buildType) }
          .onFailure { log.error("Failed to locate build artifacts", it) }
          .getOrDefault(emptyList())
      } else {
        emptyList()
      }

      val errors = if (!isSuccess) {
        runCatching { BuildErrorParser.parse(logText + "\n" + (failure ?: "")) }
          .getOrDefault(emptyList())
      } else {
        emptyList()
      }

      val result = BuildExecutionResult(
        buildType = buildType,
        isSuccessful = isSuccess,
        exitCode = if (isSuccess) 0 else 1,
        durationMillis = duration,
        tasksExecuted = tasks,
        artifacts = artifacts,
        errors = errors,
        stdout = logText,
        stderr = if (!isSuccess) (failure ?: logText) else ""
      )

      withContext(Dispatchers.Main) {
        runCatching { onFinished(result) }
          .onFailure { log.error("Build result callback threw an exception", it) }
      }
    }
  }
}
