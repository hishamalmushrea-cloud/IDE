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

package com.aidenext.sdk

import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * Metadata representation of an Android SDK component extracted from package.xml or repository2-3.xml.
 */
data class SdkPackageMetadata(
  val path: String, // e.g. "platforms;android-35" or "build-tools;35.0.0"
  val displayName: String,
  val version: String,
  val sizeBytes: Long = 0L,
  val downloadUrl: String? = null,
  val isInstalled: Boolean = false,
  val installDirectory: File? = null,
  val diskUsageBytes: Long = 0L
)

/**
 * Parser for local package.xml and remote repository2-3.xml metadata.
 */
object SdkRepositoryParser {

  private val log = LoggerFactory.getLogger(SdkRepositoryParser::class.java)

  /**
   * Scans an installed package directory and parses its package.xml metadata.
   */
  fun parseLocalPackageXml(dir: File): SdkPackageMetadata? {
    val pkgXml = File(dir, "package.xml")
    if (!pkgXml.exists() || !pkgXml.isFile) return null

    return try {
      val xmlContent = pkgXml.readText()
      val factory = XmlPullParserFactory.newInstance()
      factory.isNamespaceAware = false
      val parser = factory.newPullParser()
      parser.setInput(StringReader(xmlContent))

      var path: String? = null
      var displayName: String? = null
      var major = ""
      var minor = ""
      var micro = ""
      var preview = ""

      var eventType = parser.eventType
      var currentTag = ""

      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            currentTag = parser.name
            if (currentTag == "localPackage") {
              path = parser.getAttributeValue(null, "path")
            }
          }
          XmlPullParser.TEXT -> {
            val text = parser.text.trim()
            if (text.isNotEmpty()) {
              when (currentTag) {
                "display-name" -> displayName = text
                "major" -> major = text
                "minor" -> minor = text
                "micro" -> micro = text
                "preview" -> preview = text
              }
            }
          }
        }
        eventType = parser.next()
      }

      val versionStr = buildString {
        if (major.isNotEmpty()) append(major)
        if (minor.isNotEmpty()) append(".$minor")
        if (micro.isNotEmpty()) append(".$micro")
        if (preview.isNotEmpty()) append(" rc$preview")
      }

      val diskUsage = calculateDirectorySize(dir)

      SdkPackageMetadata(
        path = path ?: dir.name,
        displayName = displayName ?: dir.name,
        version = versionStr.ifEmpty { "1.0.0" },
        sizeBytes = diskUsage,
        isInstalled = true,
        installDirectory = dir,
        diskUsageBytes = diskUsage
      )
    } catch (e: Exception) {
      log.warn("Failed parsing package.xml in ${dir.absolutePath}", e)
      null
    }
  }

  /**
   * Parses remote repository XML format (e.g. repository2-3.xml) from string content.
   */
  fun parseRemoteRepositoryXml(xmlContent: String): List<SdkPackageMetadata> {
    val results = mutableListOf<SdkPackageMetadata>()
    try {
      val factory = XmlPullParserFactory.newInstance()
      factory.isNamespaceAware = false
      val parser = factory.newPullParser()
      parser.setInput(StringReader(xmlContent))

      var path = ""
      var displayName = ""
      var major = ""
      var minor = ""
      var micro = ""
      var downloadUrl: String? = null
      var sizeBytes = 0L

      var eventType = parser.eventType
      var currentTag = ""

      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            currentTag = parser.name
            if (currentTag == "remotePackage") {
              path = parser.getAttributeValue(null, "path") ?: ""
              displayName = ""
              major = ""
              minor = ""
              micro = ""
              downloadUrl = null
              sizeBytes = 0L
            }
          }
          XmlPullParser.TEXT -> {
            val text = parser.text.trim()
            if (text.isNotEmpty()) {
              when (currentTag) {
                "display-name" -> displayName = text
                "major" -> major = text
                "minor" -> minor = text
                "micro" -> micro = text
                "url" -> downloadUrl = text
                "size" -> sizeBytes = text.toLongOrNull() ?: 0L
              }
            }
          }
          XmlPullParser.END_TAG -> {
            if (parser.name == "remotePackage" && path.isNotEmpty()) {
              val versionStr = buildString {
                if (major.isNotEmpty()) append(major)
                if (minor.isNotEmpty()) append(".$minor")
                if (micro.isNotEmpty()) append(".$micro")
              }
              results.add(
                SdkPackageMetadata(
                  path = path,
                  displayName = displayName.ifEmpty { path },
                  version = versionStr.ifEmpty { "1.0.0" },
                  sizeBytes = sizeBytes,
                  downloadUrl = downloadUrl,
                  isInstalled = false
                )
              )
            }
          }
        }
        eventType = parser.next()
      }
    } catch (e: Exception) {
      log.error("Failed parsing remote repository XML", e)
    }
    return results
  }

  private fun calculateDirectorySize(dir: File): Long {
    var size = 0L
    dir.walkTopDown().forEach {
      if (it.isFile) size += it.length()
    }
    return size
  }
}
