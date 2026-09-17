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

import com.aidenext.syntax.colorschemes.SchemeAndroidIDE;
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.text.Content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public class DartAnalyzer extends AsyncIncrementalAnalyzeManager<DartAnalyzer.State, DartAnalyzer.DartToken> {

  public static class State {
    public final boolean inBlockComment;
    public State() { this.inBlockComment = false; }
    public State(boolean inBlockComment) { this.inBlockComment = inBlockComment; }
  }

  public static class DartToken {
    public final int start;
    public final int length;
    public final int type;
    public DartToken(int start, int length, int type) {
      this.start = start;
      this.length = length;
      this.type = type;
    }
  }

  private static final Set<String> KEYWORDS = Set.of(
    "abstract", "as", "assert", "async", "await", "break", "case", "catch", "class",
    "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else",
    "enum", "export", "extends", "extension", "external", "factory", "false", "final",
    "finally", "for", "Function", "get", "hide", "if", "implements", "import", "in",
    "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator",
    "part", "required", "rethrow", "return", "set", "show", "static", "super", "switch",
    "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield"
  );

  private static final Set<String> TYPES = Set.of(
    "int", "double", "num", "String", "bool", "List", "Map", "Set", "Future", "Stream",
    "Iterable", "Duration", "DateTime", "Uri", "Object", "Widget", "BuildContext", "State"
  );

  @Override
  public List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate) {
    List<CodeBlock> blocks = new ArrayList<>();
    Deque<Integer> stack = new ArrayDeque<>();

    for (int line = 0; line < text.getLineCount(); line++) {
      String str = text.getLineString(line);
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (c == '{') {
          stack.push(line);
        } else if (c == '}' && !stack.isEmpty()) {
          int start = stack.pop();
          CodeBlock block = new CodeBlock();
          block.startLine = start;
          block.endLine = line;
          blocks.add(block);
        }
      }
    }
    return blocks;
  }

  @Override
  public State getInitialState() {
    return new State();
  }

  @Override
  public LineTokenizeResult<State, DartToken> tokenizeLine(
    CharSequence line,
    State state,
    int lineIndex
  ) {
    List<DartToken> tokens = new ArrayList<>();
    boolean inComment = state != null && state.inBlockComment;
    int idx = 0;
    int len = line.length();

    while (idx < len) {
      if (inComment) {
        int end = line.toString().indexOf("*/", idx);
        if (end == -1) {
          tokens.add(new DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT));
          idx = len;
        } else {
          tokens.add(new DartToken(idx, end + 2 - idx, SchemeAndroidIDE.COMMENT));
          idx = end + 2;
          inComment = false;
        }
        continue;
      }

      char c = line.charAt(idx);

      if (Character.isWhitespace(c)) {
        idx++;
        continue;
      }

      // Line comment
      if (c == '/' && idx + 1 < len && line.charAt(idx + 1) == '/') {
        tokens.add(new DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT));
        break;
      }

      // Block comment
      if (c == '/' && idx + 1 < len && line.charAt(idx + 1) == '*') {
        int end = line.toString().indexOf("*/", idx + 2);
        if (end == -1) {
          tokens.add(new DartToken(idx, len - idx, SchemeAndroidIDE.COMMENT));
          idx = len;
          inComment = true;
        } else {
          tokens.add(new DartToken(idx, end + 2 - idx, SchemeAndroidIDE.COMMENT));
          idx = end + 2;
        }
        continue;
      }

      // String literal
      if (c == '"' || c == '\'') {
        char quote = c;
        int end = idx + 1;
        boolean escaped = false;
        while (end < len) {
          char ch = line.charAt(end);
          if (ch == '\\' && !escaped) {
            escaped = true;
          } else if (ch == quote && !escaped) {
            end++;
            break;
          } else {
            escaped = false;
          }
          end++;
        }
        tokens.add(new DartToken(idx, end - idx, SchemeAndroidIDE.LITERAL));
        idx = end;
        continue;
      }

      // Number literal
      if (Character.isDigit(c)) {
        int end = idx + 1;
        while (end < len && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.' || line.charAt(end) == 'x' || Character.isLetter(line.charAt(end)))) {
          end++;
        }
        tokens.add(new DartToken(idx, end - idx, SchemeAndroidIDE.LITERAL));
        idx = end;
        continue;
      }

      // Identifier / Keyword
      if (Character.isJavaIdentifierStart(c) || c == '@') {
        int end = idx + 1;
        while (end < len && Character.isJavaIdentifierPart(line.charAt(end))) {
          end++;
        }
        String word = line.subSequence(idx, end).toString();
        int type;
        if (word.startsWith("@")) {
          type = SchemeAndroidIDE.ANNOTATION;
        } else if (KEYWORDS.contains(word)) {
          type = SchemeAndroidIDE.KEYWORD;
        } else if (TYPES.contains(word) || Character.isUpperCase(word.charAt(0))) {
          type = SchemeAndroidIDE.TYPE_NAME;
        } else {
          type = SchemeAndroidIDE.TEXT_NORMAL;
        }
        tokens.add(new DartToken(idx, end - idx, type));
        idx = end;
        continue;
      }

      // Operators
      tokens.add(new DartToken(idx, 1, SchemeAndroidIDE.OPERATOR));
      idx++;
    }

    return new LineTokenizeResult<>(new State(inComment), tokens);
  }

  @Override
  public List<Span> generateSpansForLine(LineTokenizeResult<State, DartToken> tokens) {
    List<Span> spans = new ArrayList<>();
    spans.add(Span.obtain(0, TextStyle.makeStyle(SchemeAndroidIDE.TEXT_NORMAL)));

    if (tokens != null && tokens.tokens != null) {
      for (DartToken token : tokens.tokens) {
        long style;
        switch (token.type) {
          case SchemeAndroidIDE.KEYWORD:
            style = SchemeAndroidIDE.forKeyword();
            break;
          case SchemeAndroidIDE.COMMENT:
            style = SchemeAndroidIDE.forComment();
            break;
          case SchemeAndroidIDE.TYPE_NAME:
            style = TextStyle.makeStyle(SchemeAndroidIDE.TYPE_NAME);
            break;
          case SchemeAndroidIDE.ANNOTATION:
            style = TextStyle.makeStyle(SchemeAndroidIDE.ANNOTATION);
            break;
          case SchemeAndroidIDE.LITERAL:
            style = TextStyle.makeStyle(SchemeAndroidIDE.LITERAL);
            break;
          case SchemeAndroidIDE.OPERATOR:
            style = TextStyle.makeStyle(SchemeAndroidIDE.OPERATOR);
            break;
          default:
            style = TextStyle.makeStyle(SchemeAndroidIDE.TEXT_NORMAL);
            break;
        }
        spans.add(Span.obtain(token.start, style));
      }
    }
    return spans;
  }
}
