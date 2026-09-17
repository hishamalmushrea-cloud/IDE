/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.actions

import android.content.Context
import android.graphics.drawable.Drawable
import com.aidenext.activities.editor.EditorHandlerActivity
import com.aidenext.projects.GradleProject
import com.aidenext.projects.IProjectManager
import com.aidenext.projects.android.AndroidModule
import com.aidenext.tasks.cancelIfActive
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import java.io.File

/** @author Akash Yadav */
abstract class EditorActivityAction : ActionItem {

  override var enabled: Boolean = true
  override var visible: Boolean = true
  override var icon: Drawable? = null
  override var label: String = ""
  override var location: ActionItem.Location = ActionItem.Location.EDITOR_TOOLBAR

  override var requiresUIThread: Boolean = false

  protected val actionScope = CoroutineScope(Dispatchers.Default) +
      CoroutineName("${javaClass.simpleName}Scope")

  override fun prepare(data: ActionData) {
    super.prepare(data)
    if (!data.hasRequiredData(Context::class.java)) {
      markInvisible()
    }
  }

  fun ActionData.getActivity(): EditorHandlerActivity? {
    return this[Context::class.java] as? EditorHandlerActivity
  }

  fun ActionData.requireActivity(): EditorHandlerActivity {
    return getActivity()!!
  }

  /**
   * Returns the project which is currently open in the IDE, or `null` if no project is open (or
   * the project is not configured yet).
   */
  fun ActionData.getProject(): GradleProject? {
    val manager = runCatching { IProjectManager.getInstance() }.getOrNull() ?: return null

    val workspace = runCatching { manager.getWorkspace() }.getOrNull()
    if (workspace != null) {
      return runCatching { workspace.getRootProject() }.getOrNull()
    }

    // The workspace is not configured yet. Fall back to the project directory of the manager.
    val dir = runCatching { manager.projectDir }.getOrNull() ?: return null
    if (!dir.exists()) {
      return null
    }

    return GradleProject(
      name = dir.name,
      description = dir.name,
      path = ":",
      projectDir = dir,
      buildDir = File(dir, "build"),
      buildScript = File(dir, "build.gradle"),
      tasks = emptyList()
    )
  }

  /**
   * Same as [getProject], but throws an [IllegalStateException] if no project is open.
   */
  fun ActionData.requireProject(): GradleProject {
    return requireNotNull(getProject()) { "No project is open in the IDE." }
  }

  /**
   * Returns all the Android application modules of the currently open project. If [project] does
   * not belong to the currently open workspace, then an empty list is returned.
   */
  protected fun getApplicationModules(
    @Suppress("UNUSED_PARAMETER") project: GradleProject? = null
  ): List<AndroidModule> {
    val workspace = runCatching { IProjectManager.getInstance().getWorkspace() }.getOrNull()
      ?: return emptyList()

    return runCatching {
      workspace.androidProjects().filter { it.isApplication }.toList()
    }.getOrDefault(emptyList())
  }

  override fun destroy() {
    super.destroy()
    actionScope.cancelIfActive("Action is being destroyed")
  }
}
