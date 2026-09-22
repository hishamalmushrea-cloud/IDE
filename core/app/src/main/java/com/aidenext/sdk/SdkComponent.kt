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

package com.aidenext.sdk

import java.io.File

enum class SdkCategory(val title: String) {
  PLATFORMS("SDK Platforms"),
  BUILD_TOOLS("Build Tools"),
  PLATFORM_TOOLS("Platform Tools"),
  CMDLINE_TOOLS("Command-line Tools"),
  CMAKE("CMake"),
  NDK("Android NDK")
}

data class SdkPackage(
  val id: String,
  val name: String,
  val category: SdkCategory,
  val version: String,
  val isInstalled: Boolean,
  val installDir: File? = null,
  val sizeBytes: Long = 0L,
  val description: String = ""
)
