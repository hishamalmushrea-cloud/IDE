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

package com.aidenext.lsp.kotlin

import com.aidenext.lsp.models.CodeFormatResult
import com.aidenext.lsp.models.FormatCodeParams
import com.aidenext.lsp.models.Position
import com.aidenext.lsp.models.Range
import com.aidenext.lsp.models.TextEdit

object KotlinFormatProvider {

  fun format(params: FormatCodeParams?): CodeFormatResult {
    if (params == null) {
      return CodeFormatResult(false, mutableListOf())
    }

    val content = params.content
    val lines = content.lines()
    val edits = mutableListOf<TextEdit>()

    var indentLevel = 0
    val indentUnit = "  " // 2 or 4 spaces

    for (lineIdx in lines.indices) {
      val rawLine = lines[lineIdx]
      val trimmed = rawLine.trim()

      if (trimmed.isEmpty()) {
        continue
      }

      val startsClosing = trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")
      var currentIndent = indentLevel
      if (startsClosing && currentIndent > 0) {
        currentIndent--
      }

      val desiredLine = indentUnit.repeat(currentIndent) + trimmed
      if (desiredLine != rawLine) {
        edits.add(
          TextEdit(
            range = Range(
              start = Position(lineIdx, 0),
              end = Position(lineIdx, rawLine.length)
            ),
            newText = desiredLine
          )
        )
      }

      // calculate net brackets change
      var open = 0
      var close = 0
      for (c in trimmed) {
        if (c in "{([") open++
        else if (c in "})]") close++
      }
      indentLevel = maxOf(0, indentLevel + (open - close))
    }

    return CodeFormatResult(true, edits)
  }
}
