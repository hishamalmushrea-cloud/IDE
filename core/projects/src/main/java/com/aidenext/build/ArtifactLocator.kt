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

package com.aidenext.build

import com.aidenext.projects.AndroidModule
import com.aidenext.projects.Project
import org.slf4j.LoggerFactory
import java.io.File

object ArtifactLocator {

  private val log = LoggerFactory.getLogger(ArtifactLocator::class.java)

  fun locateArtifacts(project: Project, module: AndroidModule?, buildType: BuildType): List<BuildArtifact> {
    val artifacts = mutableListOf<BuildArtifact>()

    val searchDirs = mutableListOf<File>()
    if (module != null) {
      searchDirs.add(module.projectDir)
    } else {
      searchDirs.add(project.rootDirectory)
      project.rootDirectory.listFiles()?.filter { it.isDirectory }?.forEach { searchDirs.add(it) }
    }

    for (dir in searchDirs) {
      // 1. APKs
      val apkOut = File(dir, "build/outputs/apk")
      if (apkOut.exists()) {
        apkOut.walkTopDown().filter { it.isFile && it.extension == "apk" }.forEach { f ->
          val isDebug = f.name.contains("debug")
          val isUnsigned = f.name.contains("unsigned")
          artifacts.add(
            BuildArtifact(
              kind = ArtifactKind.APK,
              file = f,
              sizeBytes = f.length(),
              variant = if (isDebug) "debug" else "release",
              isSigned = isDebug || !isUnsigned
            )
          )
        }
      }

      // 2. Bundles (AAB)
      val bundleOut = File(dir, "build/outputs/bundle")
      if (bundleOut.exists()) {
        bundleOut.walkTopDown().filter { it.isFile && it.extension == "aab" }.forEach { f ->
          val isDebug = f.name.contains("debug")
          val isUnsigned = f.name.contains("unsigned")
          artifacts.add(
            BuildArtifact(
              kind = ArtifactKind.AAB,
              file = f,
              sizeBytes = f.length(),
              variant = if (isDebug) "debug" else "release",
              isSigned = isDebug || !isUnsigned
            )
          )
        }
      }

      // 3. Native .so
      val nativeOut = File(dir, "build/intermediates/stripped_native_libs")
      if (nativeOut.exists()) {
        nativeOut.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { f ->
          artifacts.add(
            BuildArtifact(
              kind = ArtifactKind.NATIVE_SO,
              file = f,
              sizeBytes = f.length(),
              architecture = f.parentFile?.name
            )
          )
        }
      }
    }

    return artifacts.distinctBy { it.file.absolutePath }
  }
}
