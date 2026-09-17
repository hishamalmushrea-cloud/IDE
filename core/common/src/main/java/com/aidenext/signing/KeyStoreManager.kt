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

import com.aidenext.utils.Environment
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date

object KeyStoreManager {

  private val log = LoggerFactory.getLogger(KeyStoreManager::class.java)

  var activeConfig: KeyStoreConfig? = null

  /**
   * Lists all keystore files in the default keystores directory and project dir.
   */
  fun listKnownKeyStores(): List<File> {
    val list = mutableListOf<File>()
    val dir = Environment.KEYSTORES_DIR
    if (dir != null && dir.exists()) {
      dir.listFiles()?.filter { it.isFile && (it.extension in listOf("jks", "keystore", "p12")) }?.let {
        list.addAll(it)
      }
    }
    return list
  }

  /**
   * Loads an existing KeyStore and returns all alias names.
   */
  fun getAliases(keystoreFile: File, storePassword: CharArray): List<String> {
    val ks = KeyStore.getInstance(if (keystoreFile.extension.equals("p12", true)) "PKCS12" else KeyStore.getDefaultType())
    FileInputStream(keystoreFile).use { fis ->
      ks.load(fis, storePassword)
    }
    val aliases = mutableListOf<String>()
    val enum = ks.aliases()
    while (enum.hasMoreElements()) {
      aliases.add(enum.nextElement())
    }
    return aliases
  }

  /**
   * Creates a new KeyStore and self-signed certificate for release signing.
   */
  fun createKeyStore(
    targetFile: File,
    storePassword: CharArray,
    alias: String,
    keyPassword: CharArray,
    dname: String = "CN=AndroidIDE, O=Developer, C=US",
    validityYears: Int = 25
  ): Boolean {
    return try {
      val keyPairGen = KeyPairGenerator.getInstance("RSA")
      keyPairGen.initialize(2048, SecureRandom())
      val keyPair = keyPairGen.generateKeyPair()

      val ks = KeyStore.getInstance("PKCS12")
      ks.load(null, null)

      // Generate self-signed certificate using Java reflection to avoid direct sun.security imports
      val cert = generateSelfSignedCertificate(dname, keyPair, validityYears)
      ks.setKeyEntry(alias, keyPair.private, keyPassword, arrayOf<Certificate>(cert))

      targetFile.parentFile?.mkdirs()
      FileOutputStream(targetFile).use { fos ->
        ks.store(fos, storePassword)
      }
      log.info("Created keystore at {}", targetFile.absolutePath)
      true
    } catch (e: Exception) {
      log.error("Failed to create keystore: {}", targetFile, e)
      false
    }
  }

  private fun generateSelfSignedCertificate(
    dname: String,
    keyPair: java.security.KeyPair,
    validityYears: Int
  ): X509Certificate {
    val now = System.currentTimeMillis()
    val from = Date(now)
    val to = Date(now + validityYears.toLong() * 365 * 24 * 60 * 60 * 1000)

    try {
      // Use BouncyCastle if available, otherwise sun.security via reflection
      val certInfoClass = Class.forName("sun.security.x509.X509CertInfo")
      val certInfo = certInfoClass.getConstructor().newInstance()

      val x500NameClass = Class.forName("sun.security.x509.X500Name")
      val owner = x500NameClass.getConstructor(String::class.java).newInstance(dname)

      val certificateValidityClass = Class.forName("sun.security.x509.CertificateValidity")
      val interval = certificateValidityClass.getConstructor(Date::class.java, Date::class.java).newInstance(from, to)

      val certSerialClass = Class.forName("sun.security.x509.CertificateSerialNumber")
      val serial = certSerialClass.getConstructor(BigInteger::class.java).newInstance(BigInteger(64, SecureRandom()))

      val certVersionClass = Class.forName("sun.security.x509.CertificateVersion")
      val version = certVersionClass.getConstructor(Int::class.javaPrimitiveType).newInstance(2) // V3

      val algorithmIdClass = Class.forName("sun.security.x509.AlgorithmId")
      val algoGet = algorithmIdClass.getMethod("get", String::class.java).invoke(null, "SHA256withRSA")
      val certAlgoIdClass = Class.forName("sun.security.x509.CertificateAlgorithmId")
      val algoId = certAlgoIdClass.getConstructor(algorithmIdClass).newInstance(algoGet)

      val certSubjectClass = Class.forName("sun.security.x509.CertificateX509Key")
      val subjectKey = certSubjectClass.getConstructor(java.security.PublicKey::class.java).newInstance(keyPair.public)

      val setMethod = certInfoClass.getMethod("set", String::class.java, Any::class.java)
      setMethod.invoke(certInfo, "validity", interval)
      setMethod.invoke(certInfo, "serialNumber", serial)
      setMethod.invoke(certInfo, "subject", owner)
      setMethod.invoke(certInfo, "issuer", owner)
      setMethod.invoke(certInfo, "key", subjectKey)
      setMethod.invoke(certInfo, "version", version)
      setMethod.invoke(certInfo, "algorithmID", algoId)

      val certImplClass = Class.forName("sun.security.x509.X509CertImpl")
      val cert = certImplClass.getConstructor(certInfoClass).newInstance(certInfo)
      val signMethod = certImplClass.getMethod("sign", java.security.PrivateKey::class.java, String::class.java)
      signMethod.invoke(cert, keyPair.private, "SHA256withRSA")

      return cert as X509Certificate
    } catch (e: Throwable) {
      log.warn("sun.security reflection failed, attempting fallback certificate generation", e)
      throw UnsupportedOperationException("Standard X509 certificate generation unavailable on this JVM runtime: " + e.message)
    }
  }
}
