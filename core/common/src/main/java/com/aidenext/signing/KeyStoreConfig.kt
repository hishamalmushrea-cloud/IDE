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

package com.aidenext.signing

import java.io.File

data class KeyStoreConfig(
  val keystoreFile: File,
  val storePassword: CharArray,
  val keyAlias: String,
  val keyPassword: CharArray,
  val v1SigningEnabled: Boolean = true,
  val v2SigningEnabled: Boolean = true
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is KeyStoreConfig) return false
    return keystoreFile == other.keystoreFile && keyAlias == other.keyAlias
  }

  override fun hashCode(): Int {
    var result = keystoreFile.hashCode()
    result = 31 * result + keyAlias.hashCode()
    return result
  }
}
