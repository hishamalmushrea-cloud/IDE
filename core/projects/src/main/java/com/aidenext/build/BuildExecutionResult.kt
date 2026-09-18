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

import com.aidenext.build.errors.ParsedBuildError

data class BuildExecutionResult(
  val buildType: BuildType,
  val isSuccessful: Boolean,
  val exitCode: Int,
  val durationMillis: Long,
  val tasksExecuted: List<String>,
  val artifacts: List<BuildArtifact>,
  val errors: List<ParsedBuildError> = emptyList(),
  val warnings: List<String> = emptyList(),
  val stdout: String = "",
  val stderr: String = ""
) {
  val durationFormatted: String
    get() {
      val seconds = durationMillis / 1000.0
      return String.format("%.1f s", seconds)
    }
}
