package org.heidiverse.heidi.issuer.extensions.staticoffer

import org.heidiverse.heidi.issuer.model.FlowVariant
import org.heidiverse.heidi.issuer.model.api.CredentialOfferRequest
import org.heidiverse.heidi.issuer.model.api.CredentialOfferResponse
import org.heidiverse.heidi.issuer.service.IssuanceService
import org.heidiverse.heidi.issuer.service.ProcessTokenDecoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StaticOfferService(
    private val staticOffers: StaticOfferRepository,
    private val tokenDecoder: ProcessTokenDecoder,
    private val tokenCrypto: StaticOfferTokenCrypto,
    private val issuance: IssuanceService,
) {
    @Transactional
    fun create(issuerSlug: String, processToken: String): UUID =
        tokenDecoder.decode(processToken).let { data ->
            require(data.issuerSlug == null || data.issuerSlug == issuerSlug) {
                "Process token issuer '${data.issuerSlug}' does not match route issuer '$issuerSlug'"
            }
            val id = UUID.randomUUID()
            staticOffers.save(StaticOfferEntity().apply {
                this.id = id
                this.issuerSlug = issuerSlug
                this.processToken = tokenCrypto.encrypt(processToken, id)
            }).id
        }

    @Transactional
    fun resolve(id: UUID, issuerSlug: String, variant: FlowVariant): CredentialOfferResponse {
        val static = staticOffers.findById(id)
            .orElseThrow { NoSuchElementException("Static credential offer not found: $id") }
        require(static.issuerSlug == issuerSlug) { "Static credential offer issuer mismatch" }
        val processToken = tokenCrypto.decrypt(static.processToken, static.id)
        return issuance.createOffer(issuerSlug, variant, CredentialOfferRequest(processToken, null))
    }
}
