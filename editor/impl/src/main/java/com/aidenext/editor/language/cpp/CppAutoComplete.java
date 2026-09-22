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

package com.aidenext.editor.language.cpp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.aidenext.editor.language.utils.CompletionHelper;
import com.blankj.utilcode.util.StringUtils;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.CompletionItemKind;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CppAutoComplete {

  private static final List<String> CPP_KEYWORDS = List.of(
    "auto", "bool", "break", "case", "catch", "char", "class", "const", "constexpr",
    "continue", "default", "delete", "do", "double", "else", "enum", "explicit", "export",
    "extern", "false", "final", "float", "for", "friend", "goto", "if", "inline", "int",
    "long", "mutable", "namespace", "new", "noexcept", "nullptr", "operator", "override",
    "private", "protected", "public", "register", "reinterpret_cast", "return", "short",
    "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch",
    "template", "this", "thread_local", "throw", "true", "try", "typedef", "typeid",
    "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "while"
  );

  private static final List<String> JNI_SYMBOLS = List.of(
    "JNIEXPORT", "JNICALL", "JNIEnv", "JavaVM", "jobject", "jclass", "jstring", "jarray",
    "jboolean", "jbyte", "jchar", "jshort", "jint", "jlong", "jfloat", "jdouble", "jsize",
    "jthrowable", "jbooleanArray", "jbyteArray", "jcharArray", "jshortArray", "jintArray",
    "jlongArray", "jfloatArray", "jdoubleArray", "jobjectArray",
    "__android_log_print", "ANDROID_LOG_INFO", "ANDROID_LOG_ERROR", "ANDROID_LOG_DEBUG", "ANDROID_LOG_WARN",
    "#include <jni.h>", "#include <android/log.h>", "#include <string>", "#include <vector>",
    "#include <memory>", "#include <iostream>", "#include <map>", "#include <utility>",
    "std::string", "std::vector", "std::make_shared", "std::make_unique", "std::shared_ptr", "std::unique_ptr"
  );

  public void complete(
    @NonNull ContentReference content,
    @NonNull CharPosition position,
    @NonNull CompletionPublisher publisher,
    @NonNull Bundle extraArguments
  ) {
    publisher.setUpdateThreshold(0);
    final var prefix = CompletionHelper.computePrefix(content, position,
      c -> MyCharacter.isJavaIdentifierPart(c) || c == '#' || c == ':' || c == '<' || c == '>');

    if (StringUtils.isTrimEmpty(prefix)) {
      return;
    }

    final var lowerPrefix = prefix.toLowerCase(Locale.ROOT);

    for (String jni : JNI_SYMBOLS) {
      if (jni.toLowerCase(Locale.ROOT).contains(lowerPrefix)) {
        final var item = new SimpleCompletionItem(jni, "JNI / NDK", prefix.length(), jni);
        item.kind(CompletionItemKind.Keyword);
        item.setMatchLevel(CompletionItem.matchLevel(jni, prefix));
        publisher.addItem(item);
      }
    }

    for (String kw : CPP_KEYWORDS) {
      if (kw.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
        final var item = new SimpleCompletionItem(kw, "Keyword", prefix.length(), kw);
        item.kind(CompletionItemKind.Keyword);
        item.setMatchLevel(CompletionItem.matchLevel(kw, prefix));
        publisher.addItem(item);
      }
    }
  }
}
