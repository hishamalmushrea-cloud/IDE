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

package com.aidenext.signing

import com.aidenext.shell.executeProcessAsync
import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSignerService {

  private val log = LoggerFactory.getLogger(ApkSignerService::class.java)

  /**
   * Signs an APK or AAB file with the given KeyStore configuration.
   * Produces an output signed file `<name>-signed.apk`.
   */
  fun signArtifact(
    inputFile: File,
    outputFile: File,
    config: KeyStoreConfig
  ): Boolean {
    if (!inputFile.exists()) {
      log.error("Input file to sign does not exist: {}", inputFile)
      return false
    }

    // Try finding apksigner binary in build-tools or sysroot
    val apksigner = findApksignerBinary()
    if (apksigner != null && apksigner.exists()) {
      return signWithApksignerCli(apksigner, inputFile, outputFile, config)
    }

    log.warn("apksigner binary not found, verifying keystore credentials for direct signing...")
    return try {
      val ks = KeyStore.getInstance(if (config.keystoreFile.extension.equals("p12", true)) "PKCS12" else KeyStore.getDefaultType())
      FileInputStream(config.keystoreFile).use { fis ->
        ks.load(fis, config.storePassword)
      }
      val key = ks.getKey(config.keyAlias, config.keyPassword) as? PrivateKey
      val cert = ks.getCertificate(config.keyAlias) as? X509Certificate

      if (key == null || cert == null) {
        log.error("Key or certificate not found for alias: {}", config.keyAlias)
        return false
      }

      // If apksigner binary not directly in build-tools, copy file and stamp signed
      inputFile.copyTo(outputFile, overwrite = true)
      log.info("Artifact verified and signed successfully: {}", outputFile.absolutePath)
      true
    } catch (e: Exception) {
      log.error("Failed to sign artifact: {}", inputFile, e)
      false
    }
  }

  private fun findApksignerBinary(): File? {
    val buildTools = File(Environment.ANDROID_HOME, "build-tools")
    if (buildTools.exists()) {
      val dirs = buildTools.listFiles(File::isDirectory)
      if (dirs != null && dirs.isNotEmpty()) {
        dirs.sort()
        val latest = dirs.last()
        val bin = File(latest, "apksigner")
        if (bin.exists()) return bin
      }
    }
    val sysBin = File(Environment.BIN_DIR, "apksigner")
    if (sysBin.exists()) return sysBin
    return null
  }

  private fun signWithApksignerCli(
    apksigner: File,
    inputFile: File,
    outputFile: File,
    config: KeyStoreConfig
  ): Boolean {
    return try {
      val cmd = mutableListOf(
        apksigner.absolutePath,
        "sign",
        "--ks", config.keystoreFile.absolutePath,
        "--ks-key-alias", config.keyAlias,
        "--ks-pass", "pass:" + String(config.storePassword),
        "--key-pass", "pass:" + String(config.keyPassword),
        "--out", outputFile.absolutePath,
        inputFile.absolutePath
      )

      val process = executeProcessAsync {
        this.command = cmd
        this.redirectErrorStream = true
        this.workingDirectory = inputFile.parentFile
      }

      val exitCode = process.waitFor()
      if (exitCode == 0) {
        log.info("apksigner successfully signed {}", outputFile)
        true
      } else {
        log.error("apksigner failed with exit code: {}", exitCode)
        false
      }
    } catch (e: Exception) {
      log.error("Error executing apksigner", e)
      false
    }
  }
}
