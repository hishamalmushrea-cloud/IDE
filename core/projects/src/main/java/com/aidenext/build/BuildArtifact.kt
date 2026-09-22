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

package com.aidenext.build

import java.io.File
import java.text.DecimalFormat

enum class ArtifactKind {
  APK,
  AAB,
  NATIVE_SO,
  MAPPING_TXT
}

data class BuildArtifact(
  val kind: ArtifactKind,
  val file: File,
  val sizeBytes: Long = if (file.exists()) file.length() else 0L,
  val variant: String? = null,
  val architecture: String? = null,
  val isSigned: Boolean = false
) {
  val fileName: String
    get() = file.name

  val formattedSize: String
    get() {
      if (sizeBytes <= 0) return "0 B"
      val units = arrayOf("B", "KB", "MB", "GB")
      val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
      return DecimalFormat("#,##0.#").format(sizeBytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
