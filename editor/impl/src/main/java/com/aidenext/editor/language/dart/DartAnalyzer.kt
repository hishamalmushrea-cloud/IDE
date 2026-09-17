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

package com.aidenext.editor.language.dart

import com.aidenext.syntax.colorschemes.SchemeAndroidIDE
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.Content
import java.util.ArrayDeque

/**
 * A lightweight, incremental Dart syntax analyzer which provides syntax highlighting and code
 * block (folding) information for Dart files.
 *
 * This analyzer is purely syntactic: it does not resolve symbols or types. Semantic information is
 * provided by the language server (when available).
 *
 * @author AIDE Next
 */
class DartAnalyzer : AsyncIncrementalAnalyzeManager<DartAnalyzer.State, DartAnalyzer.DartToken>() {

  /**
   * The state of the analyzer at the end of a line.
   *
   * @param inBlockComment Whether the line ends in an unterminated block comment.
   */
  data class State(val inBlockComment: Boolean = false)

  /**
   * A token produced by [tokenizeLine].
   *
   * @param start The start index of the token in the line.
   * @param length The length of the token.
   * @param type The color id of the token, see [SchemeAndroidIDE].
   */
  data class DartToken(val start: Int, val length: Int, val type: Int)

  companion object {

    private val KEYWORDS = setOf(
      "abstract", "as", "assert", "async", "await", "break", "case", "catch", "class",
      "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else",
      "enum", "export", "extends", "extension", "external", "factory", "false", "final",
      "finally", "for", "Function", "get", "hide", "if", "implements", "import", "in",
      "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator",
      "part", "required", "rethrow", "return", "set", "show", "static", "super", "switch",
      "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield"
    )

    private val TYPES = setOf(
      "int", "double", "num", "String", "bool", "List", "Map", "Set", "Future", "Stream",
      "Iterable", "Duration", "DateTime", "Uri", "Object", "Widget", "BuildContext", "State"
    )
  }

  override fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock> {
    val blocks = mutableListOf<CodeBlock>()
    val stack = ArrayDeque<Int>()

    for (line in 0 until text.lineCount) {
      if (delegate.isCancelled) {
        break
      }

      val str = text.getLineString(line)
      for (i in str.indices) {
        val c = str[i]
        if (c == '{') {
          stack.addLast(line)
        } else if (c == '}' && stack.isNotEmpty()) {
          val start = stack.removeLast()
          val block = CodeBlock()
          block.startLine = start
          block.endLine = line
          blocks.add(block)
        }
      }
    }

    return blocks
  }

  override fun getInitialState(): State = State()

  override fun stateEquals(state: State, another: State): Boolean = state == another

  override fun tokenizeLine(
    line: CharSequence,
    state: State,
    lineIndex: Int
  ): LineTokenizeResult<State, DartToken> {
    val tokens = mutableListOf<DartToken>()
    var inComment = state.inBlockComment
    var idx = 0
    val len = line.length

    while (idx < len) {
      if (inComment) {
        val end = line.indexOf("*", idx).takeIf { it >= 0 && it + 1 < len && line[it + 1] == '/' }
        if (end == null) {
          tokens.add(DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT))
          idx = len
        } else {
          tokens.add(DartToken(idx, end + 2 - idx, SchemeAndroidIDE.COMMENT))
          idx = end + 2
          inComment = false
        }
        continue
      }

      val c = line[idx]

      if (c.isWhitespace()) {
        idx++
        continue
      }

      // Line comment
      if (c == '/' && idx + 1 < len && line[idx + 1] == '/') {
        tokens.add(DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT))
        break
      }

      // Block comment start
      if (c == '/' && idx + 1 < len && line[idx + 1] == '*') {
        var end = -1
        var search = idx + 2
        while (search + 1 < len) {
          if (line[search] == '*' && line[search + 1] == '/') {
            end = search
            break
          }
          search++
        }

        if (end == -1) {
          tokens.add(DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT))
          idx = len
          inComment = true
        } else {
          tokens.add(DartToken(idx, end + 2 - idx, SchemeAndroidIDE.COMMENT))
          idx = end + 2
        }
        continue
      }

      // String literal
      if (c == '"' || c == '\'') {
        val quote = c
        var end = idx + 1
        var escaped = false
        while (end < len) {
          val ch = line[end]
          if (ch == '\\' && !escaped) {
            escaped = true
          } else if (ch == quote && !escaped) {
            end++
            break
          } else {
            escaped = false
          }
          end++
        }
        tokens.add(DartToken(idx, end - idx, SchemeAndroidIDE.LITERAL))
        idx = end
        continue
      }

      // Number
      if (c.isDigit()) {
        var end = idx + 1
        while (end < len &&
          (line[end].isDigit() || line[end] == '.' || line[end] == 'x' || line[end].isLetter())
        ) {
          end++
        }
        tokens.add(DartToken(idx, end - idx, SchemeAndroidIDE.LITERAL))
        idx = end
        continue
      }

      // Identifier / keyword
      if (c.isJavaIdentifierStart() || c == '@') {
        var end = idx + 1
        while (end < len && line[end].isJavaIdentifierPart()) {
          end++
        }

        val word = line.subSequence(idx, end).toString()
        val type = when {
          word.startsWith("@") -> SchemeAndroidIDE.ANNOTATION
          KEYWORDS.contains(word) -> SchemeAndroidIDE.KEYWORD
          TYPES.contains(word) -> SchemeAndroidIDE.TYPE_NAME
          word.first().isUpperCase() -> SchemeAndroidIDE.TYPE_NAME
          else -> SchemeAndroidIDE.TEXT_NORMAL
        }
        tokens.add(DartToken(idx, end - idx, type))
        idx = end
        continue
      }

      // Operators and everything else
      tokens.add(DartToken(idx, 1, SchemeAndroidIDE.OPERATOR))
      idx++
    }

    return LineTokenizeResult(State(inComment), tokens)
  }

  override fun generateSpansForLine(tokens: LineTokenizeResult<State, DartToken>): List<Span> {
    val spans = mutableListOf<Span>()
    spans.add(Span.obtain(0, TextStyle.makeStyle(SchemeAndroidIDE.TEXT_NORMAL)))

    val lineTokens = tokens.tokens ?: return spans
    for (token in lineTokens) {
      val style = when (token.type) {
        SchemeAndroidIDE.KEYWORD -> SchemeAndroidIDE.forKeyword()
        SchemeAndroidIDE.COMMENT -> SchemeAndroidIDE.forComment()
        SchemeAndroidIDE.TYPE_NAME -> TextStyle.makeStyle(SchemeAndroidIDE.TYPE_NAME)
        SchemeAndroidIDE.ANNOTATION -> TextStyle.makeStyle(SchemeAndroidIDE.ANNOTATION)
        SchemeAndroidIDE.LITERAL -> SchemeAndroidIDE.forString()
        SchemeAndroidIDE.OPERATOR -> TextStyle.makeStyle(SchemeAndroidIDE.OPERATOR)
        else -> TextStyle.makeStyle(SchemeAndroidIDE.TEXT_NORMAL)
      }
      spans.add(Span.obtain(token.start, style))
    }

    return spans
  }
}
