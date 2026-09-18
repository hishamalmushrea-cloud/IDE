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

import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
import com.aidenext.actions.ActionData
import com.aidenext.actions.EditorActivityAction
import com.aidenext.resources.R
import com.aidenext.sdk.SdkManagerService
import com.aidenext.utils.DialogUtils

class SdkManagerAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "SDK Manager"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_android)
  }

  override val id: String = "ide.editor.tools.sdkManager"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()

    val options = arrayOf(
      "View Installed Packages & Disk Usage",
      "Catalog of Available Packages",
      "Scan Project Required Packages"
    )

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Android SDK Manager")
      .setItems(options) { _, which ->
        when (which) {
          0 -> showInstalled(activity)
          1 -> showAvailable(activity)
          2 -> showProjectRequired(activity, data)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()

    return true
  }

  private fun showInstalled(activity: Activity) {
    val installed = SdkManagerService.getInstalledComponents()
    val msg = buildString {
      appendLine("Installed Android SDK Components:")
      appendLine("=================================")
      if (installed.isEmpty()) {
        appendLine("No SDK components found in ANDROID_HOME.")
      } else {
        var totalBytes = 0L
        for (item in installed) {
          val size = SdkManagerService.formatSize(item.sizeBytes)
          appendLine("• ${item.name} (${item.type.name}): $size")
          totalBytes += item.sizeBytes
        }
        appendLine("\nTotal Disk Usage: ${SdkManagerService.formatSize(totalBytes)}")
      }
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Installed SDK Components")
      .setMessage(msg)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun showAvailable(activity: Activity) {
    val available = SdkManagerService.getAvailableComponents()
    val items = available.map { "${it.name} [${if (it.isInstalled) "INSTALLED" else "AVAILABLE"}]" }.toTypedArray()

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Available SDK Catalog")
      .setItems(items, null)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun showProjectRequired(activity: Activity, data: ActionData) {
    val project = data.requireProject()
    val missing = SdkManagerService.getMissingPackagesForProject(project.rootDirectory)

    val msg = buildString {
      if (missing.isEmpty()) {
        appendLine("All SDK components required by this project are installed.")
      } else {
        appendLine("Missing packages detected:")
        missing.forEach { appendLine("• ${it.name} (${it.sdkManagerPath})") }
        appendLine("\nRun idesetup or install them via terminal sdkmanager.")
      }
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Required SDK Packages")
      .setMessage(msg)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }
}
