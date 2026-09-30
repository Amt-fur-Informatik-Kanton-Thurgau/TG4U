package org.heidiverse.heidi.issuer.extensions.staticoffer;

import org.heidiverse.heidi.issuer.model.FlowVariant;
import org.heidiverse.heidi.issuer.model.api.CredentialOfferResponse;
import org.heidiverse.heidi.issuer.service.IssuerProperties;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class StaticOfferController {
    private final StaticOfferService staticOffers;
    private final IssuerProperties properties;

    public StaticOfferController(StaticOfferService staticOffers, IssuerProperties properties) {
        this.staticOffers = staticOffers;
        this.properties = properties;
    }

    @PostMapping("/{issuerSlug}/{variantPath}/getStaticCredentialOfferURL")
    public StaticCredentialOfferResponse staticOffer(
            @PathVariable String issuerSlug,
            @PathVariable String variantPath,
            @Valid @RequestBody StaticCredentialOfferRequest request) {
        FlowVariant.fromPath(variantPath);
        UUID id = staticOffers.create(issuerSlug, request.processToken());
        return new StaticCredentialOfferResponse(
                properties.getPublicUrl().replaceAll("/$", "")
                        + "/"
                        + issuerSlug
                        + "/"
                        + variantPath
                        + "/getCredentialOfferForUUID?uuid="
                        + id);
    }

    @GetMapping("/{issuerSlug}/{variantPath}/getCredentialOfferForUUID")
    public CredentialOfferResponse staticOffer(
            @PathVariable String issuerSlug,
            @PathVariable String variantPath,
            @RequestParam UUID uuid) {
        return staticOffers.resolve(uuid, issuerSlug, FlowVariant.fromPath(variantPath));
    }
}
