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

package com.aidenext.flutter

/**
 * Formatter for Dart source files on Android mobile devices.
 */
object DartSourceFormatter {

  private const val INDENT_SIZE = 2

  /**
   * Formats the given Dart source code string with standard Flutter 2-space indentation,
   * proper bracket alignment, and clean newline separation.
   */
  fun format(source: String): String {
    val lines = source.lines()
    val formatted = StringBuilder()
    var indentLevel = 0

    for (rawLine in lines) {
      val trimmed = rawLine.trim()
      if (trimmed.isEmpty()) {
        formatted.appendLine()
        continue
      }

      // Check if closing bracket decreases indent level before line content
      var lineStartsClosing = false
      if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
        lineStartsClosing = true
      }

      var currentLineIndent = indentLevel
      if (lineStartsClosing && currentLineIndent > 0) {
        currentLineIndent--
      }

      val indentSpaces = " ".repeat(currentLineIndent * INDENT_SIZE)
      formatted.append(indentSpaces).appendLine(trimmed)

      // Calculate net change in brackets for subsequent lines
      var openCount = 0
      var closeCount = 0
      var inString = false
      var stringQuote = ' '

      var i = 0
      while (i < trimmed.length) {
        val c = trimmed[i]
        if (inString) {
          if (c == '\\') {
            i++ // skip escaped char
          } else if (c == stringQuote) {
            inString = false
          }
        } else {
          if (c == '\'' || c == '\"') {
            inString = true
            stringQuote = c
          } else if (c == '/' && i + 1 < trimmed.length && trimmed[i + 1] == '/') {
            break // line comment, ignore remainder of line
          } else if (c in "({[") {
            openCount++
          } else if (c in ")}]") {
            closeCount++
          }
        }
        i++
      }

      val netChange = openCount - closeCount
      indentLevel = maxOf(0, indentLevel + netChange)
    }

    return formatted.toString().trimEnd() + "\n"
  }
}
