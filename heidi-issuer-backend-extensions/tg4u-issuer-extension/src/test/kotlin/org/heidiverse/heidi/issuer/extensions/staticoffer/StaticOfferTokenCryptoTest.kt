package org.heidiverse.heidi.issuer.extensions.staticoffer

import org.heidiverse.heidi.issuer.service.SessionEncryptionProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class StaticOfferTokenCryptoTest {
    private val crypto = StaticOfferTokenCrypto(
        SessionEncryptionProperties("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
    )

    @Test
    fun `encrypted token round trips`() {
        val offerId = UUID.randomUUID()
        val encrypted = crypto.encrypt("process-token", offerId)

        assertEquals("process-token", crypto.decrypt(encrypted, offerId))
    }

    @Test
    fun `plaintext token is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            crypto.decrypt("plaintext-token", UUID.randomUUID())
        }
    }

    @Test
    fun `damaged encrypted token is rejected`() {
        val offerId = UUID.randomUUID()
        val encrypted = crypto.encrypt("process-token", offerId)
        val damaged = encrypted.toCharArray().also {
            val index = it.lastIndex - 1
            it[index] = if (it[index] == 'A') 'B' else 'A'
        }.concatToString()
        assertThrows(Exception::class.java) {
            crypto.decrypt(damaged, offerId)
        }
    }
}
