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

package com.aidenext.toolchain

import java.io.File

data class ToolchainComponent(
  val id: String,
  val name: String,
  val type: ToolchainType,
  val version: String,
  val path: File?,
  val isInstalled: Boolean,
  val statusDescription: String,
  val isHealthy: Boolean = isInstalled,
  val fixActionText: String? = null
) {

  /** Alias for [path]. The directory in which this component is installed. */
  val homePath: File?
    get() = path
}
