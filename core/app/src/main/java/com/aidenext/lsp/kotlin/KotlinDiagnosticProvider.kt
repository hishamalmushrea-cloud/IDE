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

import com.aidenext.lsp.models.DiagnosticItem
import com.aidenext.lsp.models.DiagnosticResult
import com.aidenext.lsp.models.DiagnosticSeverity
import com.aidenext.models.Position
import com.aidenext.models.Range
import java.nio.file.Files
import java.nio.file.Path

object KotlinDiagnosticProvider {

  fun analyze(file: Path): DiagnosticResult {
    if (!Files.exists(file)) {
      return DiagnosticResult(file, emptyList())
    }

    val diagnostics = mutableListOf<DiagnosticItem>()
    val lines = Files.readAllLines(file)

    val bracketStack = ArrayDeque<Triple<Char, Int, Int>>() // char, lineIndex, colIndex

    for (lineIdx in lines.indices) {
      val line = lines[lineIdx]
      val trimmed = line.trim()

      if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
        continue
      }

      var inString = false
      var stringQuote = ' '
      var stringStartCol = 0

      var col = 0
      while (col < line.length) {
        val c = line[col]

        if (inString) {
          if (c == '\\') {
            col += 2 // skip escaped character
            continue
          } else if (c == stringQuote) {
            inString = false
          }
        } else {
          if (c == '\"' || c == '\'') {
            inString = true
            stringQuote = c
            stringStartCol = col
          } else if (c == '/' && col + 1 < line.length && line[col + 1] == '/') {
            break // line comment, skip rest of line
          } else if (c in "({[") {
            bracketStack.addLast(Triple(c, lineIdx, col))
          } else if (c in ")}]") {
            if (bracketStack.isEmpty()) {
              diagnostics.add(
                DiagnosticItem(
                  message = "Unexpected closing bracket '$c'",
                  code = "KOTLIN_UNEXPECTED_BRACKET",
                  range = Range(Position(lineIdx, col), Position(lineIdx, col + 1)),
                  source = "KotlinParser",
                  severity = DiagnosticSeverity.ERROR
                )
              )
            } else {
              val top = bracketStack.removeLast()
              val expected = when (top.first) {
                '(' -> ')'
                '{' -> '}'
                '[' -> ']'
                else -> ' '
              }
              if (expected != c) {
                diagnostics.add(
                  DiagnosticItem(
                    message = "Mismatched bracket: expected '$expected' but found '$c'",
                    code = "KOTLIN_MISMATCHED_BRACKET",
                    range = Range(Position(lineIdx, col), Position(lineIdx, col + 1)),
                    source = "KotlinParser",
                    severity = DiagnosticSeverity.ERROR
                  )
                )
              }
            }
          }
        }
        col++
      }

      if (inString && !trimmed.endsWith("\"\"\"")) {
        diagnostics.add(
          DiagnosticItem(
            message = "Unclosed string literal",
            code = "KOTLIN_UNCLOSED_STRING",
            range = Range(Position(lineIdx, stringStartCol), Position(lineIdx, line.length)),
            source = "KotlinParser",
            severity = DiagnosticSeverity.ERROR
          )
        )
      }
    }

    // Unclosed open brackets
    while (bracketStack.isNotEmpty()) {
      val unclosed = bracketStack.removeLast()
      diagnostics.add(
        DiagnosticItem(
          message = "Unclosed bracket '${unclosed.first}'",
          code = "KOTLIN_UNCLOSED_BRACKET",
          range = Range(Position(unclosed.second, unclosed.third), Position(unclosed.second, unclosed.third + 1)),
          source = "KotlinParser",
          severity = DiagnosticSeverity.ERROR
        )
      )
    }

    return DiagnosticResult(file, diagnostics)
  }
}
