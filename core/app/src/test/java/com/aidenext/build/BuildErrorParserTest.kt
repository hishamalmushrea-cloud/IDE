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

import com.aidenext.build.errors.BuildErrorParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildErrorParserTest {

  @Test
  fun testParseMissingPlatform() {
    val errorLog = """
      FAILURE: Build failed with an exception.
      * What went wrong:
      A problem occurred configuring project ':app'.
      > Failed to find target with hash string 'android-35' in: /home/user/android-sdk
    """.trimIndent()

    val errors = BuildErrorParser.parse(errorLog)
    assertEquals(1, errors.size)
    assertEquals("Missing Android SDK Platform 35", errors[0].problem)
    assertEquals("platforms;android-35", errors[0].requiredComponent)
    assertTrue(errors[0].suggestedAction!!.contains("Platform 35"))
  }

  @Test
  fun testParseMissingBuildTools() {
    val errorLog = """
      FAILURE: Build failed with an exception.
      * What went wrong:
      Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
      > Failed to find Build Tools revision 34.0.0
    """.trimIndent()

    val errors = BuildErrorParser.parse(errorLog)
    assertEquals(1, errors.size)
    assertEquals("Missing Android SDK Build-Tools 34.0.0", errors[0].problem)
    assertEquals("build-tools;34.0.0", errors[0].requiredComponent)
  }

  @Test
  fun testParseJavaIncompatibility() {
    val errorLog = """
      Unsupported Java version: 21. Android Gradle plugin requires Java 17.
    """.trimIndent()

    val errors = BuildErrorParser.parse(errorLog)
    assertEquals(1, errors.size)
    assertEquals("Java Version Incompatibility", errors[0].problem)
    assertNotNull(errors[0].suggestedAction)
  }

  @Test
  fun testParseMissingCMake() {
    val errorLog = """
      FAILURE: Build failed with an exception.
      > CMake '3.22.1' was not found in PATH or by cmake.dir.
    """.trimIndent()

    val errors = BuildErrorParser.parse(errorLog)
    assertEquals(1, errors.size)
    assertEquals("CMake Not Found", errors[0].problem)
    assertEquals("cmake;3.22.1", errors[0].requiredComponent)
  }

  @Test
  fun testParseOutOfMemory() {
    val errorLog = """
      Expiring Daemon because JVM heap space is exhausted
      java.lang.OutOfMemoryError: Java heap space
    """.trimIndent()

    val errors = BuildErrorParser.parse(errorLog)
    assertEquals(1, errors.size)
    assertEquals("Build Daemon Out Of Memory", errors[0].problem)
    assertTrue(errors[0].suggestedAction!!.contains("jvmargs"))
  }
}
