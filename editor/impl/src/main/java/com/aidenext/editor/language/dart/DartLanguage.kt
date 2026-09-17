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

package com.aidenext.editor.language.dart

import android.os.Bundle
import com.aidenext.editor.language.IDELanguage
import com.aidenext.editor.language.newline.BracketsNewlineHandler
import com.aidenext.editor.language.utils.CommonSymbolPairs
import io.github.rosemoe.sora.lang.Language.INTERRUPTION_LEVEL_STRONG
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

class DartLanguage : IDELanguage() {

  private var analyzer: DartAnalyzer? = DartAnalyzer()
  private val completer = DartAutoComplete()
  private val newlineHandlers = arrayOf<NewlineHandler>(
    BracketsNewlineHandler({ line -> getIndentAdvance(line.orEmpty()) }, this::useTab)
  )
  private val symbolPairs = CommonSymbolPairs()

  override fun getAnalyzeManager(): AnalyzeManager {
    return analyzer ?: DartAnalyzer().also { analyzer = it }
  }

  override fun getInterruptionLevel(): Int = INTERRUPTION_LEVEL_STRONG

  @Throws(CompletionCancelledException::class)
  override fun requireAutoComplete(
    content: ContentReference,
    position: CharPosition,
    publisher: CompletionPublisher,
    extraArguments: Bundle
  ) {
    completer.complete(content, position, publisher, extraArguments)
  }

  override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
    return getIndentAdvance(content.getLine(line).substring(0, column))
  }

  override fun getIndentAdvance(line: String): Int {
    var advance = 0
    for (c in line) {
      if (c == '{' || c == '(' || c == '[') advance++
      else if (c == '}' || c == ')' || c == ']') advance--
    }
    return Math.max(0, advance) * getTabSize()
  }

  override fun getSymbolPairs(): SymbolPairMatch = symbolPairs

  override fun getNewlineHandlers(): Array<NewlineHandler> = newlineHandlers

  override fun destroy() {
    analyzer = null
  }
}
