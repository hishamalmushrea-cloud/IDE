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

import java.io.File

/**
 * Represents parsed data from a Flutter pubspec.yaml file.
 */
data class PubspecModel(
  val name: String,
  val description: String?,
  val version: String?,
  val environmentSdk: String?,
  val dependencies: Map<String, String>,
  val devDependencies: Map<String, String>,
  val hasFlutterDependency: Boolean,
  val usesMaterialDesign: Boolean,
  val assets: List<String>
)

/**
 * Parser for Flutter `pubspec.yaml` files.
 */
object PubspecParser {

  fun parse(file: File): PubspecModel? {
    if (!file.exists() || !file.isFile) return null
    return try {
      parseText(file.readText())
    } catch (e: Exception) {
      null
    }
  }

  fun parseText(text: String): PubspecModel {
    var name = "unknown"
    var description: String? = null
    var version: String? = null
    var environmentSdk: String? = null
    val dependencies = mutableMapOf<String, String>()
    val devDependencies = mutableMapOf<String, String>()
    var usesMaterialDesign = false
    val assets = mutableListOf<String>()

    var currentSection = ""

    val lines = text.lines()
    for (rawLine in lines) {
      val line = rawLine.trimEnd()
      if (line.isBlank() || line.trimStart().startsWith("#")) continue

      val indent = line.length - line.trimStart().length
      val trimmed = line.trim()

      if (indent == 0 && trimmed.contains(":")) {
        val colonIdx = trimmed.indexOf(":")
        val key = trimmed.substring(0, colonIdx).trim()
        val value = trimmed.substring(colonIdx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
        currentSection = key
        when (key) {
          "name" -> name = value
          "description" -> description = value
          "version" -> version = value
        }
        continue
      }

      if (indent > 0 && trimmed.contains(":")) {
        val colonIdx = trimmed.indexOf(":")
        val subKey = trimmed.substring(0, colonIdx).trim()
        val subValue = trimmed.substring(colonIdx + 1).trim().removeSurrounding("\"").removeSurrounding("'")

        when (currentSection) {
          "environment" -> {
            if (subKey == "sdk") environmentSdk = subValue
          }
          "dependencies" -> {
            if (subKey.isNotEmpty()) dependencies[subKey] = subValue
          }
          "dev_dependencies" -> {
            if (subKey.isNotEmpty()) devDependencies[subKey] = subValue
          }
          "flutter" -> {
            if (subKey == "uses-material-design") {
              usesMaterialDesign = subValue.equals("true", ignoreCase = true)
            }
          }
        }
      } else if (currentSection == "flutter" && trimmed.startsWith("- ")) {
        val item = trimmed.removePrefix("- ").trim().removeSurrounding("\"").removeSurrounding("'")
        if (item.isNotEmpty()) {
          assets.add(item)
        }
      }
    }

    val hasFlutter = dependencies.containsKey("flutter") || devDependencies.containsKey("flutter")

    return PubspecModel(
      name = name,
      description = description,
      version = version,
      environmentSdk = environmentSdk,
      dependencies = dependencies,
      devDependencies = devDependencies,
      hasFlutterDependency = hasFlutter,
      usesMaterialDesign = usesMaterialDesign,
      assets = assets
    )
  }
}
