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

package com.aidenext.editor.language.dart;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.aidenext.editor.language.IDELanguage;
import com.aidenext.editor.language.newline.BracketsNewlineHandler;
import com.aidenext.editor.language.utils.CommonSymbolPairs;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class DartLanguage extends IDELanguage {

  private DartAnalyzer analyzer = new DartAnalyzer();
  private final DartAutoComplete completer = new DartAutoComplete();
  private final NewlineHandler[] newlineHandlers = new NewlineHandler[] {
    new BracketsNewlineHandler(this::getIndentAdvance, this::useTab)
  };
  private final CommonSymbolPairs symbolPairs = new CommonSymbolPairs();

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    if (analyzer == null) {
      analyzer = new DartAnalyzer();
    }
    return analyzer;
  }

  @Override
  public int getInterruptionLevel() {
    return Language.INTERRUPTION_LEVEL_STRONG;
  }

  @Override
  public void requireAutoComplete(
    @NonNull ContentReference content,
    @NonNull CharPosition position,
    @NonNull CompletionPublisher publisher,
    @NonNull Bundle extraArguments
  ) throws CompletionCancelledException {
    completer.complete(content, position, publisher, extraArguments);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
    return getIndentAdvance(content.getLine(line).substring(0, column));
  }

  @Override
  public int getIndentAdvance(@NonNull String line) {
    int advance = 0;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '{' || c == '(' || c == '[') advance++;
      else if (c == '}' || c == ')' || c == ']') advance--;
    }
    return Math.max(0, advance) * getTabSize();
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return symbolPairs;
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
  }

  @Override
  public void destroy() {
    analyzer = null;
  }
}
