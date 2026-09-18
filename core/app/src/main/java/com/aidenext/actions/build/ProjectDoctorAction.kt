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

package com.aidenext.actions.build

import android.content.Context
import androidx.core.content.ContextCompat
import com.aidenext.actions.ActionData
import com.aidenext.actions.EditorActivityAction
import com.aidenext.resources.R
import com.aidenext.toolchain.ProjectEnvironmentDetector
import com.aidenext.toolchain.ProjectToolchainResolver
import com.aidenext.utils.DialogUtils

class ProjectDoctorAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "Project Doctor"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_run)
  }

  override val id: String = "ide.editor.tools.projectDoctor"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val project = data.requireProject()

    val req = ProjectEnvironmentDetector.detect(project.rootDirectory)
    val checkResult = ProjectToolchainResolver.evaluateProject(project.rootDirectory)

    val message = buildString {
      appendLine("Project Type: ${req.projectType}")
      appendLine("AGP Version: ${req.agpVersion ?: "N/A"}")
      appendLine("compileSdk: ${req.compileSdk ?: "N/A"} | minSdk: ${req.minSdk ?: "N/A"}")
      appendLine("Compose Enabled: ${if (req.hasCompose) "Yes" else "No"}")
      appendLine("C++/NDK: ${if (req.hasCpp) "Yes (${req.ndkVersion ?: "default"})" else "No"}")
      appendLine("Required JDK: ${req.requiredJdkVersion}")
      appendLine("--------------------------------")
      appendLine("Readiness Status: ${checkResult.overallStatus}")
      appendLine()
      for (item in checkResult.reports) {
        val mark = if (item.isSatisfied) "[✓]" else "[✗]"
        appendLine("$mark ${item.title}: ${item.currentValue}")
        if (item.actionableHint != null) {
          appendLine("    → ${item.actionableHint}")
        }
      }
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Project Doctor Report")
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()

    return true
  }
}
