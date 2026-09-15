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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.aidenext.actions.ActionData
import com.aidenext.actions.EditorActivityAction
import com.aidenext.resources.R
import com.aidenext.signing.KeyStoreConfig
import com.aidenext.signing.KeyStoreManager
import com.aidenext.utils.DialogUtils
import com.aidenext.utils.Environment
import com.aidenext.utils.flashError
import com.aidenext.utils.flashSuccess
import java.io.File
import java.util.Arrays

class KeyStoreAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "Keystore & Signing"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_archive)
  }

  override val id: String = "ide.editor.tools.keystore"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()

    val currentStatus = if (KeyStoreManager.activeConfig != null) {
      "Active: ${KeyStoreManager.activeConfig?.keyAlias}"
    } else {
      "No keystore active (default debug keys used)"
    }

    val options = arrayOf(
      "Current Status: $currentStatus",
      "Generate New Keystore (PKCS12 / RSA 2048)",
      "Select Existing Keystore",
      "Clear Active Keystore"
    )

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Keystore & Signing Management")
      .setItems(options) { _, which ->
        when (which) {
          1 -> showGenerateKeystoreDialog(activity)
          2 -> showSelectKeystoreDialog(activity)
          3 -> {
            KeyStoreManager.activeConfig = null
            activity.flashSuccess("Cleared active keystore configuration.")
          }
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()

    return true
  }

  private fun showGenerateKeystoreDialog(activity: android.app.Activity) {
    val layout = LinearLayout(activity).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(50, 40, 50, 20)
    }

    val nameInput = EditText(activity).apply { hint = "Keystore File Name (e.g. release.jks)" }
    val aliasInput = EditText(activity).apply { hint = "Key Alias (e.g. mykey)" }
    val passInput = EditText(activity).apply {
      hint = "Password"
      inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    layout.addView(nameInput)
    layout.addView(aliasInput)
    layout.addView(passInput)

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Generate Signing Keystore")
      .setView(layout)
      .setPositiveButton("Generate") { _, _ ->
        val fileName = nameInput.text.toString().trim()
        val alias = aliasInput.text.toString().trim()
        val passChars = passInput.text.toString().toCharArray()

        if (fileName.isEmpty() || alias.isEmpty() || passChars.isEmpty()) {
          activity.flashError("Please fill all keystore fields.")
          Arrays.fill(passChars, '0')
          return@setPositiveButton
        }

        val targetFile = File(Environment.KEYSTORES_DIR, if (fileName.endsWith(".jks") || fileName.endsWith(".p12")) fileName else "$fileName.jks")
        val success = KeyStoreManager.generateKeyStore(
          file = targetFile,
          storePass = passChars,
          alias = alias,
          keyPass = passChars,
          dname = "CN=AIDE Next Developer, OU=Mobile Development, O=AIDE Next, C=US",
          validityDays = 25 * 365
        )

        Arrays.fill(passChars, '0')

        if (success) {
          activity.flashSuccess("Keystore generated successfully at ${targetFile.name}")
        } else {
          activity.flashError("Failed to generate keystore.")
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showSelectKeystoreDialog(activity: android.app.Activity) {
    val keystores = KeyStoreManager.listAvailableKeyStores()
    if (keystores.isEmpty()) {
      activity.flashError("No keystores found in ${Environment.KEYSTORES_DIR.name}.")
      return
    }

    val names = keystores.map { it.name }.toTypedArray()
    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Select Active Keystore")
      .setItems(names) { _, which ->
        val selected = keystores[which]
        showUnlockKeyDialog(activity, selected)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showUnlockKeyDialog(activity: android.app.Activity, file: File) {
    val layout = LinearLayout(activity).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(50, 40, 50, 20)
    }

    val aliasInput = EditText(activity).apply { hint = "Key Alias" }
    val passInput = EditText(activity).apply {
      hint = "Password"
      inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    layout.addView(aliasInput)
    layout.addView(passInput)

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Unlock Keystore: ${file.name}")
      .setView(layout)
      .setPositiveButton("Activate") { _, _ ->
        val alias = aliasInput.text.toString().trim()
        val passChars = passInput.text.toString().toCharArray()

        KeyStoreManager.activeConfig = KeyStoreConfig(
          storeFile = file,
          storePassword = passChars,
          keyAlias = alias,
          keyPassword = passChars
        )
        activity.flashSuccess("Active keystore set to ${file.name} ($alias)")
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
