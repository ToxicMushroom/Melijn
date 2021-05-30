package me.melijn.melijnbot.internals.utils

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptUtil {

    private const val algorithm = "AES/CBC/PKCS5Padding"
    const val split = "−"

    fun getKeyFromPassword(password: String, salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 65536, 256)
        return SecretKeySpec(
            factory.generateSecret(spec).encoded, "AES"
        )
    }

    private fun generateIv(): IvParameterSpec {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }

    fun encrypt(input: String, key: SecretKey, iv: IvParameterSpec = generateIv()): String {
        val cipher = Cipher.getInstance(algorithm)

        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText = cipher.doFinal(input.toByteArray())
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(iv.iv) + split + encoder.encodeToString(cipherText)
    }

    fun decrypt(cipherText: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(algorithm)
        val parts = cipherText.split(split)
        val decoder = Base64.getDecoder()
        val ivArr = decoder.decode(parts[0])
        val iv = IvParameterSpec(ivArr)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(decoder.decode(parts[1]))
        return String(plainText)
    }
}