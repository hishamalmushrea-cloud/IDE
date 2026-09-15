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

package com.aidenext.build.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aidenext.build.ArtifactKind
import com.aidenext.build.BuildExecutionResult
import com.aidenext.signing.ApkSignerService
import com.aidenext.signing.KeyStoreManager
import com.aidenext.utils.ApkInstaller
import com.aidenext.utils.DialogUtils
import com.aidenext.utils.flashError
import com.aidenext.utils.flashSuccess
import java.io.File

object BuildResultDialogHelper {

  fun showBuildResultDialog(activity: Activity, result: BuildExecutionResult) {
    val title = if (result.isSuccessful) "Build Succeeded (${result.durationFormatted})" else "Build Failed (${result.durationFormatted})"

    val msg = buildString {
      if (result.isSuccessful) {
        appendLine("Build completed successfully!")
        if (result.artifacts.isNotEmpty()) {
          appendLine("\nGenerated Artifacts:")
          for (art in result.artifacts) {
            appendLine("• ${art.fileName} (${art.formattedSize})")
            appendLine("  Path: ${art.file.absolutePath}")
            appendLine("  Signed: ${if (art.isSigned) "Yes" else "No (Unsigned)"}")
          }
        } else {
          appendLine("\nNo output APK/AAB located in default paths.")
        }
      } else {
        appendLine("Exit code: ${result.exitCode}")
        if (result.errors.isNotEmpty()) {
          appendLine("\nDetected Problems & Fixes:")
          for (err in result.errors) {
            appendLine("❌ ${err.problem}")
            if (err.cause != null) appendLine("   Cause: ${err.cause}")
            if (err.suggestedAction != null) appendLine("   → Fix: ${err.suggestedAction}")
          }
        } else {
          appendLine("\n${result.stderr.takeLast(500)}")
        }
      }
    }

    val builder = DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle(title)
      .setMessage(msg)

    val primaryApk = result.artifacts.firstOrNull { it.kind == ArtifactKind.APK }

    if (primaryApk != null) {
      builder.setPositiveButton("Install APK") { _, _ ->
        installApk(activity, primaryApk.file)
      }

      builder.setNeutralButton("Share") { _, _ ->
        shareArtifact(activity, primaryApk.file)
      }

      if (!primaryApk.isSigned && KeyStoreManager.activeConfig != null) {
        builder.setNegativeButton("Sign & Save") { _, _ ->
          signArtifact(activity, primaryApk.file)
        }
      } else {
        builder.setNegativeButton(android.R.string.ok, null)
      }
    } else {
      builder.setPositiveButton(android.R.string.ok, null)
    }

    builder.show()
  }

  private fun installApk(activity: Activity, apkFile: File) {
    try {
      ApkInstaller.installApk(activity, apkFile)
    } catch (e: Exception) {
      activity.flashError("Failed to launch APK installer: ${e.message}")
    }
  }

  private fun shareArtifact(activity: Activity, file: File) {
    try {
      val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.android.package-archive"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      activity.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    } catch (e: Exception) {
      activity.flashError("Failed to share file: ${e.message}")
    }
  }

  private fun signArtifact(activity: Activity, file: File) {
    val config = KeyStoreManager.activeConfig
    if (config == null) {
      activity.flashError("No active keystore selected.")
      return
    }

    val signedFile = File(file.parentFile, file.nameWithoutExtension + "-signed.apk")
    val ok = ApkSignerService.signApk(file, signedFile, config)
    if (ok) {
      activity.flashSuccess("Signed APK generated: ${signedFile.name}")
    } else {
      activity.flashError("Failed to sign APK.")
    }
  }
}
