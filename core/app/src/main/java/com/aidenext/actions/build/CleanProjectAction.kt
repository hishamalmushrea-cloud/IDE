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
import com.aidenext.actions.BaseBuildAction
import com.aidenext.build.BuildManager
import com.aidenext.build.BuildType
import com.aidenext.resources.R
import com.aidenext.utils.flashSuccess

class CleanProjectAction(context: Context, override val order: Int) : BaseBuildAction() {

  init {
    label = "Clean Project"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_clean)
  }

  override val id: String = "ide.editor.build.clean"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val project = data.requireProject()

    BuildManager.executeBuild(
      project = project,
      targetModule = null,
      buildType = BuildType.CLEAN
    ) { result ->
      if (result.isSuccessful) {
        activity.flashSuccess("Project cleaned successfully.")
      }
    }

    return true
  }
}
