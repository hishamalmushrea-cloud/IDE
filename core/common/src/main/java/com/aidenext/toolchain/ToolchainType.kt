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

package com.aidenext.toolchain

enum class ToolchainType(val displayName: String) {
  JDK("Java Development Kit (JDK)"),
  ANDROID_SDK("Android SDK"),
  ANDROID_NDK("Android NDK"),
  CMAKE("CMake & Ninja"),
  KOTLIN("Kotlin"),
  FLUTTER("Flutter SDK"),
  DART("Dart SDK"),
  GIT("Git VCS"),
  ADB("Android Debug Bridge (ADB)")
}
