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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectEnvironmentDetectorTest {

  @Test
  fun testDetectKotlinApp() {
    val dir = File("testing/resources/kotlin-app").canonicalFile
    val req = ProjectEnvironmentDetector.detect(dir)

    assertEquals(DetectedProjectType.ANDROID_GRADLE, req.projectType)
    assertEquals(35, req.compileSdk)
    assertEquals(24, req.minSdk)
    assertEquals(35, req.targetSdk)
    assertFalse(req.hasCpp)
    assertFalse(req.hasFlutter)
  }

  @Test
  fun testDetectComposeApp() {
    val dir = File("testing/resources/compose-app").canonicalFile
    val req = ProjectEnvironmentDetector.detect(dir)

    assertEquals(DetectedProjectType.ANDROID_COMPOSE, req.projectType)
    assertTrue(req.hasCompose)
    assertEquals(35, req.compileSdk)
    assertEquals(26, req.minSdk)
  }

  @Test
  fun testDetectJavaApp() {
    val dir = File("testing/resources/java-app").canonicalFile
    val req = ProjectEnvironmentDetector.detect(dir)

    assertEquals(DetectedProjectType.ANDROID_GRADLE, req.projectType)
    assertEquals(34, req.compileSdk)
    assertEquals(21, req.minSdk)
    assertFalse(req.hasCompose)
    assertFalse(req.hasCpp)
  }

  @Test
  fun testDetectNativeNdkApp() {
    val dir = File("testing/resources/native-ndk-app").canonicalFile
    val req = ProjectEnvironmentDetector.detect(dir)

    assertEquals(DetectedProjectType.ANDROID_NDK, req.projectType)
    assertTrue(req.hasCpp)
    assertEquals("27.2.12479018", req.ndkVersion)
    assertEquals("3.22.1", req.cmakeVersion)
  }

  @Test
  fun testDetectFlutterApp() {
    val dir = File("testing/resources/flutter-app").canonicalFile
    val req = ProjectEnvironmentDetector.detect(dir)

    assertEquals(DetectedProjectType.FLUTTER, req.projectType)
    assertTrue(req.hasFlutter)
    assertNotNull(req.dartVersion)
    assertTrue(req.dartVersion!!.contains("3.0.0"))
  }
}
