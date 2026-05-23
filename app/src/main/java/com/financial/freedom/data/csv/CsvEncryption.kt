package com.financial.freedom.data.csv

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 加密/解密 CSV 数据，使用 PIN 码作为密钥来源。
 *
 * 文件格式:
 *   [salt: 32 bytes][IV: 12 bytes][ciphertext + 16-byte GCM tag]
 */
object CsvEncryption {
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 32
    private const val PBKDF2_ITERATIONS = 100_000

    fun encrypt(plainBytes: ByteArray, pin: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pin, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)

        // salt + iv + ciphertext
        return salt + iv + encrypted
    }

    fun decrypt(encryptedBytes: ByteArray, pin: String): ByteArray {
        val salt = encryptedBytes.copyOfRange(0, SALT_LENGTH)
        val iv = encryptedBytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = encryptedBytes.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, encryptedBytes.size)

        val key = deriveKey(pin, salt)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(ciphertext)
    }

    fun isEncrypted(bytes: ByteArray): Boolean {
        // 未加密的 CSV 以文本字符开头，加密的文件则是二进制
        if (bytes.size < 4) return false
        // 检查前 4 字节是否可打印 ASCII 字符
        return !(bytes[0] in 0x20..0x7E && bytes[1] in 0x20..0x7E &&
                 bytes[2] in 0x20..0x7E && bytes[3] in 0x20..0x7E)
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
