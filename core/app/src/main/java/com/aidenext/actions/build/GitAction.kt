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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.aidenext.actions.ActionData
import com.aidenext.actions.EditorActivityAction
import com.aidenext.git.GitRepositoryManager
import com.aidenext.resources.R
import com.aidenext.utils.DialogUtils
import com.aidenext.utils.flashError
import com.aidenext.utils.flashSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "Git & Version Control"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_git)
  }

  override val id: String = "ide.editor.tools.git"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    val project = data.requireProject()

    if (!GitRepositoryManager.isGitRepository(project.rootDirectory)) {
      activity.flashError("Current project is not a Git repository.")
      return false
    }

    val options = arrayOf(
      "Git Status",
      "Stage All & Commit",
      "Pull from Remote",
      "Push to Remote",
      "Branch Manager",
      "Commit History (Log)",
      "View Diff"
    )

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Git VCS — ${project.name}")
      .setItems(options) { _, which ->
        when (which) {
          0 -> showGitStatus(activity, project.rootDirectory)
          1 -> showCommitDialog(activity, project.rootDirectory)
          2 -> executePull(activity, project.rootDirectory)
          3 -> executePush(activity, project.rootDirectory)
          4 -> showBranches(activity, project.rootDirectory)
          5 -> showLog(activity, project.rootDirectory)
          6 -> showDiff(activity, project.rootDirectory)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()

    return true
  }

  private fun showGitStatus(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val status = GitRepositoryManager.getStatus(rootDir)
      withContext(Dispatchers.Main) {
        val msg = buildString {
          appendLine("Current Branch: ${status.currentBranch}")
          appendLine("Clean Working Tree: ${status.isClean}")
          if (status.staged.isNotEmpty()) {
            appendLine("\nStaged:")
            status.staged.forEach { appendLine("  + $it") }
          }
          if (status.modified.isNotEmpty()) {
            appendLine("\nModified:")
            status.modified.forEach { appendLine("  * $it") }
          }
          if (status.untracked.isNotEmpty()) {
            appendLine("\nUntracked:")
            status.untracked.forEach { appendLine("  ? $it") }
          }
        }
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("Git Status")
          .setMessage(msg)
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }

  private fun showCommitDialog(activity: Activity, rootDir: java.io.File) {
    val input = EditText(activity).apply {
      hint = "Commit Message"
    }
    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Commit Changes")
      .setView(input)
      .setPositiveButton("Commit") { _, _ ->
        val msg = input.text.toString().trim()
        if (msg.isNotEmpty()) {
          CoroutineScope(Dispatchers.IO).launch {
            GitRepositoryManager.stageAll(rootDir)
            val res = GitRepositoryManager.commit(rootDir, msg)
            withContext(Dispatchers.Main) {
              if (res.isSuccess) activity.flashSuccess("Committed: $msg")
              else activity.flashError("Commit failed: ${res.message}")
            }
          }
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun executePull(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val res = GitRepositoryManager.pull(rootDir)
      withContext(Dispatchers.Main) {
        if (res.isSuccess) activity.flashSuccess("Pull succeeded.")
        else activity.flashError("Pull failed: ${res.message}")
      }
    }
  }

  private fun executePush(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val res = GitRepositoryManager.push(rootDir)
      withContext(Dispatchers.Main) {
        if (res.isSuccess) activity.flashSuccess("Push succeeded.")
        else activity.flashError("Push failed: ${res.message}")
      }
    }
  }

  private fun showBranches(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val branches = GitRepositoryManager.listBranches(rootDir)
      withContext(Dispatchers.Main) {
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("Git Branches")
          .setItems(branches.toTypedArray()) { _, which ->
            val branch = branches[which].removePrefix("* ").trim()
            CoroutineScope(Dispatchers.IO).launch {
              GitRepositoryManager.checkoutBranch(rootDir, branch)
              withContext(Dispatchers.Main) {
                activity.flashSuccess("Switched to branch $branch")
              }
            }
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      }
    }
  }

  private fun showLog(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val logs = GitRepositoryManager.getLog(rootDir, 20)
      withContext(Dispatchers.Main) {
        val items = logs.map { "${it.commitHash.take(7)} - ${it.shortMessage} (${it.authorName})" }.toTypedArray()
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("Commit History")
          .setItems(items, null)
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }

  private fun showDiff(activity: Activity, rootDir: java.io.File) {
    CoroutineScope(Dispatchers.IO).launch {
      val diff = GitRepositoryManager.getDiff(rootDir)
      withContext(Dispatchers.Main) {
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("Git Diff")
          .setMessage(diff.ifBlank { "No uncommitted modifications." })
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }
}
