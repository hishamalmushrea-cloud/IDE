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
import com.aidenext.actions.openApplicationModuleChooser
import com.aidenext.build.BuildManager
import com.aidenext.build.BuildType
import com.aidenext.build.ui.BuildResultDialogHelper
import com.aidenext.resources.R
import com.aidenext.utils.flashError
import org.slf4j.LoggerFactory

class BuildDebugApkAction(context: Context, override val order: Int) : BaseBuildAction() {

  companion object {
    private val log = LoggerFactory.getLogger(BuildDebugApkAction::class.java)
  }

  init {
    label = "Build Debug APK"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_android)
  }

  override val id: String = "ide.editor.build.debugApk"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val project = data.requireProject()

    val modules = getApplicationModules(project)
    if (modules.isEmpty()) {
      activity.flashError("No runnable Android application module found in project.")
      return false
    }

    openApplicationModuleChooser(data) { selectedModule ->
      BuildManager.executeBuild(
        project = project,
        targetModule = selectedModule,
        buildType = BuildType.DEBUG_APK
      ) { result ->
        BuildResultDialogHelper.showBuildResultDialog(activity, result)
      }
    }

    return true
  }
}
