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
import com.aidenext.toolchain.ToolchainManager
import com.aidenext.utils.DialogUtils

class EnvironmentDoctorAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "Environment Doctor"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_terminal)
  }

  override val id: String = "ide.editor.tools.environmentDoctor"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val components = ToolchainManager.scanAllComponents()

    val message = buildString {
      appendLine("AIDE Next Host Environment Diagnostics")
      appendLine("======================================")
      for (comp in components) {
        val mark = if (comp.isInstalled) "[✓]" else "[ ]"
        appendLine("$mark ${comp.name} (${comp.type}): ${if (comp.isInstalled) comp.version ?: "Found" else "Not installed"}")
        val home = comp.homePath
        if (comp.isInstalled && home != null) {
          appendLine("    Path: ${home.absolutePath}")
        }
      }
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Environment Doctor")
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()

    return true
  }
}
