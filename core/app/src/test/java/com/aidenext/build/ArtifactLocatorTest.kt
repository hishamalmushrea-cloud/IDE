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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArtifactLocatorTest {

  @Test
  fun testArtifactModelAndFormatting() {
    val dummyFile = File("testing/resources/dummy-test.apk")
    val artifact = BuildArtifact(
      kind = ArtifactKind.APK,
      file = dummyFile,
      sizeBytes = 18 * 1024 * 1024L,
      variant = "debug",
      isSigned = true
    )

    assertEquals("dummy-test.apk", artifact.fileName)
    assertTrue(artifact.formattedSize.contains("18"))
    assertTrue(artifact.formattedSize.contains("MB"))
    assertTrue(artifact.isSigned)
  }
}
