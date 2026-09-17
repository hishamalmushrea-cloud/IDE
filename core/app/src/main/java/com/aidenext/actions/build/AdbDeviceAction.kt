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
import androidx.core.content.ContextCompat
import com.aidenext.actions.ActionData
import com.aidenext.actions.EditorActivityAction
import com.aidenext.adb.AdbManager
import com.aidenext.resources.R
import com.aidenext.utils.DialogUtils
import com.aidenext.utils.flashError
import com.aidenext.utils.flashSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdbDeviceAction(context: Context, override val order: Int) : EditorActivityAction() {

  init {
    label = "ADB & Devices"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_terminal)
  }

  override val id: String = "ide.editor.tools.adb"
  override var requiresUIThread: Boolean = true

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()

    val options = arrayOf(
      "List Connected Devices",
      "Connect via Wireless ADB (IP:Port)",
      "View Real-Time Logcat (All)",
      "Kill ADB Server"
    )

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("ADB & Device Management")
      .setItems(options) { _, which ->
        when (which) {
          0 -> listDevices(activity)
          1 -> connectWireless(activity)
          2 -> viewLogcat(activity)
          3 -> killServer(activity)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()

    return true
  }

  private fun listDevices(activity: Activity) {
    CoroutineScope(Dispatchers.IO).launch {
      val devices = AdbManager.getConnectedDevices()
      withContext(Dispatchers.Main) {
        val msg = if (devices.isEmpty()) {
          "No connected ADB devices found.\nEnable Wireless Debugging or connect via USB OTG."
        } else {
          buildString {
            appendLine("Connected Devices:")
            devices.forEach { appendLine("• ${it.serial} [${it.state}] ${it.model ?: ""}") }
          }
        }
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("ADB Devices")
          .setMessage(msg)
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }

  private fun connectWireless(activity: Activity) {
    val input = EditText(activity).apply {
      hint = "192.168.1.X:5555"
      setText("localhost:5555")
    }

    DialogUtils.newMaterialDialogBuilder(activity)
      .setTitle("Connect Wireless ADB")
      .setView(input)
      .setPositiveButton("Connect") { _, _ ->
        val target = input.text.toString().trim()
        if (target.isNotEmpty()) {
          CoroutineScope(Dispatchers.IO).launch {
            val (ok, out) = AdbManager.connectTcp(target)
            withContext(Dispatchers.Main) {
              if (ok) activity.flashSuccess("Connected to $target")
              else activity.flashError("Failed: $out")
            }
          }
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun viewLogcat(activity: Activity) {
    CoroutineScope(Dispatchers.IO).launch {
      val logs = AdbManager.captureLogcat(lines = 100)
      withContext(Dispatchers.Main) {
        DialogUtils.newMaterialDialogBuilder(activity)
          .setTitle("ADB Logcat")
          .setMessage(logs.ifBlank { "No logs captured." })
          .setPositiveButton(android.R.string.ok, null)
          .show()
      }
    }
  }

  private fun killServer(activity: Activity) {
    CoroutineScope(Dispatchers.IO).launch {
      AdbManager.killServer()
      withContext(Dispatchers.Main) {
        activity.flashSuccess("ADB server terminated.")
      }
    }
  }
}
