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
 *  along with AIDE Next.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aidenext.utils

import java.io.File

/**
 * A JDK distribution which can be installed in the IDE's prefix directory.
 *
 * The JDK distributions are installed inside the `$PREFIX/opt` directory. The name of the
 * directory is defined by [directoryName].
 *
 * @property version The major Java version of this distribution, for example `17`.
 * @property directoryName The name of the directory which holds the JDK distribution inside
 *   `$PREFIX/opt`.
 * @author AIDE Next
 */
enum class JdkDistribution(
  val version: String,
  val directoryName: String
) {

  /** OpenJDK 11. */
  JDK_11("11", "openjdk-11"),

  /** OpenJDK 17. This is the default JDK distribution. */
  JDK_17("17", "openjdk-17"),

  /** OpenJDK 21. */
  JDK_21("21", "openjdk-21");

  /**
   * The directory in which this JDK distribution is installed. The path is resolved against the
   * given [prefix] directory. Note that the directory may not exist.
   */
  fun getDirectory(prefix: File): File = File(File(prefix, "opt"), directoryName)

  companion object {

    /** The JDK distribution which is used when no other distribution is available. */
    @JvmField
    val DEFAULT = JDK_17

    /**
     * Returns the [JdkDistribution] for the given [version]. If the given version does not
     * correspond to any known distribution, then [DEFAULT] is returned.
     */
    @JvmStatic
    fun forVersion(version: String?): JdkDistribution {
      if (version.isNullOrBlank()) {
        return DEFAULT
      }

      return entries.firstOrNull { it.version == version.trim() } ?: DEFAULT
    }
  }
}
