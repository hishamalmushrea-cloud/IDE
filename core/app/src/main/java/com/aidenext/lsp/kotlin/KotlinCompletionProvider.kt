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

import com.aidenext.lsp.models.CompletionItem
import com.aidenext.lsp.models.CompletionItemKind
import com.aidenext.lsp.models.CompletionParams
import com.aidenext.lsp.models.CompletionResult
import com.aidenext.lsp.models.InsertTextFormat
import com.aidenext.lsp.models.MatchLevel
import java.nio.file.Files
import java.util.regex.Pattern

object KotlinCompletionProvider {

  private val KOTLIN_KEYWORDS = listOf(
    "package", "import", "class", "interface", "object", "companion object", "enum class", "sealed class", "data class",
    "fun", "val", "var", "override", "open", "abstract", "final", "private", "protected", "public", "internal",
    "suspend", "inline", "noinline", "crossinline", "reified", "tailrec", "operator", "infix",
    "typealias", "constructor", "init", "this", "super", "null", "true", "false",
    "if", "else", "when", "for", "while", "do", "try", "catch", "finally", "throw", "return", "break", "continue",
    "is", "as", "in", "!in", "!is", "by", "lazy", "lateinit", "const"
  )

  private val COMPOSE_ITEMS = listOf(
    "Composable", "Preview", "remember", "rememberSaveable", "mutableStateOf",
    "LaunchedEffect", "DisposableEffect", "SideEffect", "derivedStateOf",
    "Column", "Row", "Box", "Spacer", "Text", "Button", "OutlinedButton", "IconButton",
    "TextField", "OutlinedTextField", "Card", "Surface", "Scaffold", "TopAppBar",
    "LazyColumn", "LazyRow", "LazyVerticalGrid", "items", "itemsIndexed",
    "Modifier", "Alignment", "Arrangement", "MaterialTheme", "LocalContext"
  )

  private val ANDROID_ITEMS = listOf(
    "onCreate(savedInstanceState: Bundle?)", "onStart()", "onResume()", "onPause()", "onStop()", "onDestroy()",
    "findViewById", "setContentView", "finish()", "startActivity", "intent", "applicationContext"
  )

  private val FUN_PATTERN = Pattern.compile("fun\\s+([a-zA-Z0-9_]+)\\s*\\(")
  private val VAL_PATTERN = Pattern.compile("(val|var)\\s+([a-zA-Z0-9_]+)\\s*[:=]")
  private val CLASS_PATTERN = Pattern.compile("(class|interface|object)\\s+([a-zA-Z0-9_]+)")

  fun complete(params: CompletionParams): CompletionResult {
    val items = mutableListOf<CompletionItem>()
    val prefix = params.prefix?.lowercase() ?: ""

    // 1. Add Kotlin keywords
    for (kw in KOTLIN_KEYWORDS) {
      if (prefix.isEmpty() || kw.lowercase().startsWith(prefix)) {
        items.add(
          CompletionItem(
            ideLabel = kw,
            detail = "Kotlin keyword",
            insertText = kw,
            insertTextFormat = InsertTextFormat.PLAIN_TEXT,
            sortText = "0_$kw",
            command = null,
            completionKind = CompletionItemKind.KEYWORD,
            matchLevel = if (kw.lowercase() == prefix) MatchLevel.CASE_INSENSITIVE_EQUAL else MatchLevel.CASE_INSENSITIVE_PREFIX,
            additionalTextEdits = null,
            data = null
          )
        )
      }
    }

    // 2. Add Compose components & primitives
    for (composeItem in COMPOSE_ITEMS) {
      if (prefix.isEmpty() || composeItem.lowercase().startsWith(prefix)) {
        val kind = if (composeItem.startsWith("remember") || composeItem.startsWith("mutable") || composeItem.contains("Effect")) {
          CompletionItemKind.FUNCTION
        } else if (composeItem == "Composable" || composeItem == "Preview") {
          CompletionItemKind.ANNOTATION_TYPE
        } else {
          CompletionItemKind.CLASS
        }

        items.add(
          CompletionItem(
            ideLabel = composeItem,
            detail = "Jetpack Compose",
            insertText = composeItem,
            insertTextFormat = InsertTextFormat.PLAIN_TEXT,
            sortText = "1_$composeItem",
            command = null,
            completionKind = kind,
            matchLevel = MatchLevel.CASE_INSENSITIVE_PREFIX,
            additionalTextEdits = null,
            data = null
          )
        )
      }
    }

    // 3. Add Android standard functions
    for (androidItem in ANDROID_ITEMS) {
      val simpleName = androidItem.substringBefore("(")
      if (prefix.isEmpty() || simpleName.lowercase().startsWith(prefix)) {
        items.add(
          CompletionItem(
            ideLabel = androidItem,
            detail = "Android lifecycle / API",
            insertText = androidItem,
            insertTextFormat = InsertTextFormat.PLAIN_TEXT,
            sortText = "2_$simpleName",
            command = null,
            completionKind = CompletionItemKind.METHOD,
            matchLevel = MatchLevel.CASE_INSENSITIVE_PREFIX,
            additionalTextEdits = null,
            data = null
          )
        )
      }
    }

    // 4. Extract symbols from current source file
    try {
      if (Files.exists(params.file)) {
        val lines = Files.readAllLines(params.file)
        for (line in lines) {
          // Functions
          val funMatcher = FUN_PATTERN.matcher(line)
          while (funMatcher.find()) {
            val name = funMatcher.group(1)
            if (name.lowercase().startsWith(prefix)) {
              items.add(
                CompletionItem(
                  ideLabel = "$name()",
                  detail = "Function in file",
                  insertText = "$name()",
                  insertTextFormat = InsertTextFormat.PLAIN_TEXT,
                  sortText = "3_$name",
                  command = null,
                  completionKind = CompletionItemKind.FUNCTION,
                  matchLevel = MatchLevel.CASE_INSENSITIVE_PREFIX,
                  additionalTextEdits = null,
                  data = null
                )
              )
            }
          }

          // Variables
          val valMatcher = VAL_PATTERN.matcher(line)
          while (valMatcher.find()) {
            val kind = valMatcher.group(1)
            val name = valMatcher.group(2)
            if (name.lowercase().startsWith(prefix)) {
              items.add(
                CompletionItem(
                  ideLabel = name,
                  detail = "$kind in file",
                  insertText = name,
                  insertTextFormat = InsertTextFormat.PLAIN_TEXT,
                  sortText = "3_$name",
                  command = null,
                  completionKind = CompletionItemKind.VARIABLE,
                  matchLevel = MatchLevel.CASE_INSENSITIVE_PREFIX,
                  additionalTextEdits = null,
                  data = null
                )
              )
            }
          }

          // Classes
          val classMatcher = CLASS_PATTERN.matcher(line)
          while (classMatcher.find()) {
            val kind = classMatcher.group(1)
            val name = classMatcher.group(2)
            if (name.lowercase().startsWith(prefix)) {
              items.add(
                CompletionItem(
                  ideLabel = name,
                  detail = "$kind in file",
                  insertText = name,
                  insertTextFormat = InsertTextFormat.PLAIN_TEXT,
                  sortText = "3_$name",
                  command = null,
                  completionKind = CompletionItemKind.CLASS,
                  matchLevel = MatchLevel.CASE_INSENSITIVE_PREFIX,
                  additionalTextEdits = null,
                  data = null
                )
              )
            }
          }
        }
      }
    } catch (_: Exception) {}

    // Deduplicate items by label
    val uniqueItems = items.distinctBy { it.ideLabel }
    return CompletionResult(false, uniqueItems)
  }
}
