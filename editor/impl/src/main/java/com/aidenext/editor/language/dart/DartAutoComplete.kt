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
import com.aidenext.editor.language.utils.CompletionHelper
import com.blankj.utilcode.util.StringUtils
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import java.util.Locale

class DartAutoComplete {

  private val dartKeywords = listOf(
    "abstract", "as", "assert", "async", "await", "break", "case", "catch", "class",
    "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else",
    "enum", "export", "extends", "extension", "external", "factory", "false", "final",
    "finally", "for", "Function", "get", "hide", "if", "implements", "import", "in",
    "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator",
    "part", "required", "rethrow", "return", "set", "show", "static", "super", "switch",
    "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield"
  )

  private val dartTypes = listOf(
    "int", "double", "num", "String", "bool", "List", "Map", "Set", "Future", "Stream",
    "Iterable", "Duration", "DateTime", "Uri", "Object"
  )

  private val flutterWidgets = listOf(
    "StatelessWidget", "StatefulWidget", "Widget", "BuildContext", "State",
    "MaterialApp", "Scaffold", "AppBar", "Container", "Row", "Column", "Text",
    "Center", "Padding", "Expanded", "SizedBox", "ListView", "Navigator", "Icon",
    "IconButton", "ElevatedButton", "TextButton", "OutlinedButton", "FloatingActionButton",
    "TextField", "TextFormField", "Card", "Divider", "Spacer", "Stack", "Positioned",
    "Wrap", "SingleChildScrollView", "GestureDetector", "InkWell", "CircularProgressIndicator",
    "LinearProgressIndicator", "AlertDialog", "SnackBar", "Theme", "ThemeData",
    "Colors", "EdgeInsets", "TextStyle", "FontWeight", "MainAxisAlignment", "CrossAxisAlignment",
    "BoxDecoration", "BorderRadius", "setState", "initState", "dispose", "build"
  )

  fun complete(
    content: ContentReference,
    position: CharPosition,
    publisher: CompletionPublisher,
    extraArguments: Bundle
  ) {
    publisher.setUpdateThreshold(0)
    val prefix = CompletionHelper.computePrefix(
      content,
      position
    ) { c -> MyCharacter.isJavaIdentifierPart(c) || c == '@' }

    if (StringUtils.isTrimEmpty(prefix)) return

    val lower = prefix.lowercase(Locale.ROOT)

    // Complete Flutter widgets and classes
    for (widget in flutterWidgets) {
      if (widget.lowercase(Locale.ROOT).startsWith(lower)) {
        val item = SimpleCompletionItem(widget, "Flutter", prefix.length, widget)
        item.kind(CompletionItemKind.Class)
        item.setMatchLevel(CompletionItem.matchLevel(widget, prefix))
        publisher.addItem(item)
      }
    }

    // Complete Dart types
    for (type in dartTypes) {
      if (type.lowercase(Locale.ROOT).startsWith(lower)) {
        val item = SimpleCompletionItem(type, "Dart Type", prefix.length, type)
        item.kind(CompletionItemKind.TypeParameter)
        item.setMatchLevel(CompletionItem.matchLevel(type, prefix))
        publisher.addItem(item)
      }
    }

    // Complete Keywords
    for (kw in dartKeywords) {
      if (kw.lowercase(Locale.ROOT).startsWith(lower)) {
        val item = SimpleCompletionItem(kw, "Keyword", prefix.length, kw)
        item.kind(CompletionItemKind.Keyword)
        item.setMatchLevel(CompletionItem.matchLevel(kw, prefix))
        publisher.addItem(item)
      }
    }
  }
}
