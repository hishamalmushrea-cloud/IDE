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

enum class BuildType(val displayName: String, val defaultTasks: List<String>) {
  DEBUG_APK("Build Debug APK", listOf("assembleDebug")),
  RELEASE_APK("Build Release APK", listOf("assembleRelease")),
  ALL_APKS("Build All APKs", listOf("assemble")),
  BUNDLE_DEBUG("Build Debug App Bundle (AAB)", listOf("bundleDebug")),
  BUNDLE_RELEASE("Build Release App Bundle (AAB)", listOf("bundleRelease")),
  CLEAN("Clean Project", listOf("clean")),
  REBUILD("Rebuild Project", listOf("clean", "assembleDebug")),
  NATIVE_BUILD("Build Native C/C++ Libraries", listOf("externalNativeBuildDebug")),
  FLUTTER_BUILD_APK("Flutter Build APK", emptyList()),
  FLUTTER_BUILD_AAB("Flutter Build App Bundle", emptyList());

  val isBundle: Boolean
    get() = this == BUNDLE_DEBUG || this == BUNDLE_RELEASE || this == FLUTTER_BUILD_AAB

  val isFlutter: Boolean
    get() = this == FLUTTER_BUILD_APK || this == FLUTTER_BUILD_AAB
}
