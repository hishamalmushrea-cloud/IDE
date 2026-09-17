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

package com.aidenext.adb

import com.aidenext.shell.executeProcessAsync
import com.aidenext.utils.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File

data class AdbDevice(
  val id: String,
  val model: String,
  val state: DeviceState,
  val isLocal: Boolean = false
) {

  /** Alias for [id]. The serial number of the device as reported by `adb`. */
  val serial: String
    get() = id
}

enum class DeviceState {
  ONLINE,
  OFFLINE,
  UNAUTHORIZED,
  UNKNOWN
}

object AdbManager {

  private val log = LoggerFactory.getLogger(AdbManager::class.java)

  private fun getAdbExecutable(): File {
    val sdkAdb = File(Environment.ANDROID_HOME, "platform-tools/adb")
    if (sdkAdb.exists() && sdkAdb.canExecute()) return sdkAdb
    val sysAdb = File(Environment.BIN_DIR, "adb")
    if (sysAdb.exists() && sysAdb.canExecute()) return sysAdb
    return sdkAdb
  }

  fun isAdbAvailable(): Boolean {
    val bin = getAdbExecutable()
    return bin.exists() && bin.canExecute()
  }

  fun getConnectedDevices(): List<AdbDevice> {
    val list = mutableListOf<AdbDevice>()

    // Always include the current local Android device hosting the IDE
    list.add(
      AdbDevice(
        id = "local-device",
        model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (This Device)",
        state = DeviceState.ONLINE,
        isLocal = true
      )
    )

    if (!isAdbAvailable()) return list

    try {
      val process = executeProcessAsync {
        this.command = listOf(getAdbExecutable().absolutePath, "devices", "-l")
        this.redirectErrorStream = true
      }

      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()

      output.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("List of devices") || trimmed.isBlank()) return@forEach
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size >= 2) {
          val id = parts[0]
          val stateStr = parts[1]
          val state = when (stateStr.lowercase()) {
            "device" -> DeviceState.ONLINE
            "offline" -> DeviceState.OFFLINE
            "unauthorized" -> DeviceState.UNAUTHORIZED
            else -> DeviceState.UNKNOWN
          }
          val model = parts.find { it.startsWith("model:") }?.substringAfter("model:") ?: id
          list.add(AdbDevice(id, model, state, isLocal = false))
        }
      }
    } catch (e: Exception) {
      log.warn("Failed to query adb devices", e)
    }

    return list
  }

  fun installApk(device: AdbDevice, apk: File): Pair<Boolean, String> {
    if (!apk.exists()) return false to "APK file does not exist"
    if (device.isLocal) {
      return true to "Ready for on-device installation"
    }

    return try {
      val process = executeProcessAsync {
        this.command = listOf(getAdbExecutable().absolutePath, "-s", device.id, "install", "-r", apk.absolutePath)
        this.redirectErrorStream = true
      }
      val out = process.inputStream.bufferedReader().readText()
      val code = process.waitFor()
      (code == 0 && out.contains("Success")) to out
    } catch (e: Exception) {
      false to (e.message ?: "ADB install exception")
    }
  }

  fun launchApp(device: AdbDevice, packageName: String, launchActivity: String?): Pair<Boolean, String> {
    val target = if (launchActivity != null) "$packageName/$launchActivity" else packageName
    val cmd = if (launchActivity != null) {
      listOf("am", "start", "-n", target)
    } else {
      listOf("monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1")
    }

    return if (device.isLocal) {
      try {
        val process = executeProcessAsync {
          this.command = cmd
          this.redirectErrorStream = true
        }
        val out = process.inputStream.bufferedReader().readText()
        (process.waitFor() == 0) to out
      } catch (e: Exception) {
        false to (e.message ?: "Failed to start activity")
      }
    } else {
      try {
        val adbCmd = mutableListOf(getAdbExecutable().absolutePath, "-s", device.id, "shell")
        adbCmd.addAll(cmd)
        val process = executeProcessAsync {
          this.command = adbCmd
          this.redirectErrorStream = true
        }
        val out = process.inputStream.bufferedReader().readText()
        (process.waitFor() == 0) to out
      } catch (e: Exception) {
        false to (e.message ?: "Failed to start activity via ADB")
      }
    }
  }

  /** Connects to a device over TCP/IP, for example `192.168.1.10:5555`. */
  fun connectTcp(target: String): Pair<Boolean, String> {
    return try {
      val process = executeProcessAsync {
        this.command = listOf(getAdbExecutable().absolutePath, "connect", target)
        this.redirectErrorStream = true
      }
      val out = process.inputStream.bufferedReader().readText()
      val code = process.waitFor()
      (code == 0 && !out.contains("failed", ignoreCase = true)) to out.trim()
    } catch (e: Exception) {
      false to (e.message ?: "ADB connect exception")
    }
  }

  /** Captures the last [lines] lines of the logcat buffer. */
  fun captureLogcat(lines: Int = 100): String {
    return try {
      val cmd = if (isAdbAvailable()) {
        listOf(getAdbExecutable().absolutePath, "logcat", "-d", "-t", lines.toString())
      } else {
        listOf("logcat", "-d", "-t", lines.toString())
      }

      val process = executeProcessAsync {
        this.command = cmd
        this.redirectErrorStream = true
      }
      val out = process.inputStream.bufferedReader().readText()
      process.waitFor()
      out
    } catch (e: Exception) {
      log.warn("Failed to capture logcat", e)
      ""
    }
  }

  /** Terminates the ADB server. */
  fun killServer(): Boolean {
    return try {
      val process = executeProcessAsync {
        this.command = listOf(getAdbExecutable().absolutePath, "kill-server")
        this.redirectErrorStream = true
      }
      process.waitFor() == 0
    } catch (e: Exception) {
      log.warn("Failed to kill the ADB server", e)
      false
    }
  }

  fun startLogcatStream(
    device: AdbDevice,
    packageName: String? = null,
    logLevel: String = "V",
    onLineReceived: (String) -> Unit
  ): Job {
    return CoroutineScope(Dispatchers.IO).launch {
      try {
        val cmd = mutableListOf<String>()
        if (device.isLocal) {
          cmd.add("logcat")
          cmd.add("-v")
          cmd.add("time")
          cmd.add("*:$logLevel")
        } else {
          cmd.add(getAdbExecutable().absolutePath)
          cmd.add("-s")
          cmd.add(device.id)
          cmd.add("logcat")
          cmd.add("-v")
          cmd.add("time")
          cmd.add("*:$logLevel")
        }

        val process = executeProcessAsync {
          this.command = cmd
          this.redirectErrorStream = true
        }

        process.inputStream.bufferedReader().forEachLine { line ->
          if (packageName.isNullOrBlank() || line.contains(packageName)) {
            onLineReceived(line)
          }
        }
      } catch (e: Exception) {
        log.warn("Logcat stream ended or failed", e)
      }
    }
  }
}
