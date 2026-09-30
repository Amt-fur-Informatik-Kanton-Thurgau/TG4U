package org.heidiverse.heidi.issuer.extensions.staticoffer

import org.heidiverse.heidi.issuer.service.SessionEncryptionProperties
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class StaticOfferTokenCrypto(properties: SessionEncryptionProperties) {
    private val random = SecureRandom()
    private val masterKey = runCatching { HexFormat.of().parseHex(properties.encryptionMasterKey) }
        .getOrElse { throw IllegalArgumentException("heidi.issuer.session.encryption-master-key must be hex", it) }
        .also { require(it.size == 32) { "heidi.issuer.session.encryption-master-key must contain 32 bytes" } }

    fun encrypt(token: String, offerId: UUID): String =
        PREFIX + encryptValue(token, "heidi-static-offer:$offerId:v1".encodeToByteArray())

    fun decrypt(token: String, offerId: UUID): String {
        require(token.startsWith(PREFIX)) { "Static offer token must use encrypted format" }

        return decryptValue(
            token.removePrefix(PREFIX),
            "heidi-static-offer:$offerId:v1".encodeToByteArray(),
        )
    }

    private fun encryptValue(plaintext: String, aad: ByteArray): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            byteArrayOf(1) + iv + cipher.doFinal(plaintext.encodeToByteArray()),
        )
    }

    private fun decryptValue(value: String, aad: ByteArray): String {
        val bytes = Base64.getUrlDecoder().decode(value)
        require(bytes.size > 29 && bytes[0] == 1.toByte()) { "Invalid encrypted value" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(masterKey, "AES"),
            GCMParameterSpec(128, bytes.copyOfRange(1, 13)),
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size)).decodeToString()
    }

    private companion object { const val PREFIX = "enc.v1." }
}
