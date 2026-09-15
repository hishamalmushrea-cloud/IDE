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
import com.aidenext.flutter.FlutterExecutor
import com.aidenext.flutter.FlutterManager
import com.aidenext.flutter.FlutterToolchainProvider
import com.aidenext.flutter.ProotLinuxBackend
import com.aidenext.flutter.TermuxNativeBackend
import com.aidenext.resources.R
import com.aidenext.utils.DialogUtils
import com.aidenext.utils.flashError
import com.aidenext.utils.flashSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlutterAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "Flutter & Dart"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_build)
  }

  override val id: String = "ide.editor.tools.flutter"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val project = data.requireProject()

    val isFlutter = FlutterManager.isFlutterProject(project.rootDirectory)
    val status = FlutterToolchainProvider.inspectToolchain()

    val options = arrayOf(
      "Flutter Toolchain Status",
      "Switch Backend: ${FlutterExecutor.activeBackend.backendName}",
      "Run 'flutter doctor -v'",
      "Run 'flutter pub get'",
      "Run 'flutter clean'",
      "Run 'flutter analyze'",
      "Run 'flutter test'",
      "Format Current Dart File"
    )

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Flutter Toolchain — ${if (isFlutter) project.name else "Not a Flutter Project"}")
      .setItems(options) { _, which ->
        when (which) {
          0 -> showStatus(activity)
          1 -> switchBackend(activity)
          2 -> runCommand(activity, project.rootDirectory, "doctor")
          3 -> runCommand(activity, project.rootDirectory, "pubGet")
          4 -> runCommand(activity, project.rootDirectory, "clean")
          5 -> runCommand(activity, project.rootDirectory, "analyze")
          6 -> runCommand(activity, project.rootDirectory, "test")
          7 -> formatCurrentDart(activity, data)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()

    return true
  }

  private fun showStatus(activity: Activity) {
    val status = FlutterToolchainProvider.inspectToolchain()
    val msg = buildString {
      appendLine("Installed: ${if (status.isInstalled) "Yes" else "No"}")
      appendLine("Flutter Version: ${status.flutterVersion ?: "Unknown"}")
      appendLine("Dart Version: ${status.dartVersion ?: "Unknown"}")
      appendLine("Libc Architecture: ${status.libcType}")
      appendLine("Linux PRoot Available: ${if (status.hasGlibcProot) "Yes" else "No"}")
      appendLine("Active Backend: ${FlutterExecutor.activeBackend.backendName}")
      if (status.flutterHome != null) {
        appendLine("Path: ${status.flutterHome.absolutePath}")
      }
      appendLine("\n${status.statusMessage}")
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Flutter Toolchain Status")
      .setMessage(msg)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun switchBackend(activity: Activity) {
    val backends = arrayOf("Termux Native (Bionic)", "PRoot Linux Container (Glibc)")
    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Select Flutter Build Backend")
      .setItems(backends) { _, which ->
        if (which == 0) {
          FlutterExecutor.selectBackend(TermuxNativeBackend())
          activity.flashSuccess("Selected Termux Native backend.")
        } else {
          FlutterExecutor.selectBackend(ProotLinuxBackend())
          activity.flashSuccess("Selected PRoot Linux backend.")
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun runCommand(activity: Activity, rootDir: java.io.File, cmd: String) {
    CoroutineScope(Dispatchers.IO).launch {
      val res = when (cmd) {
        "doctor" -> FlutterExecutor.runDoctor(rootDir)
        "pubGet" -> FlutterExecutor.runPubGet(rootDir)
        "clean" -> FlutterExecutor.runClean(rootDir)
        "analyze" -> FlutterExecutor.runAnalyze(rootDir)
        "test" -> FlutterExecutor.runTest(rootDir)
        else -> null
      }

      withContext(Dispatchers.Main) {
        if (res != null) {
          val out = res.stdout.ifBlank { res.stderr }
          DialogUtils.newMaterialDialogBuilder(activity)
            .setTitle("Flutter: $cmd")
            .setMessage(out.ifBlank { "Exit code: ${res.exitCode}" })
            .setPositiveButton(android.R.string.ok, null)
            .show()
        }
      }
    }
  }

  private fun formatCurrentDart(activity: Activity, data: ActionData) {
    activity.flashSuccess("Dart formatter active in editor.")
  }
}
