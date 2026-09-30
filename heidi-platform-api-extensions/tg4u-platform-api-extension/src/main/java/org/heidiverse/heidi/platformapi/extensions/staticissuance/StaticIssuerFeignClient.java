package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.platformapi.extensions.Tg4uIssuerFeignClientConfiguration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "staticIssuerFeignClient",
        url = "${heidi.platform.issuer-internal-base-url}",
        configuration = Tg4uIssuerFeignClientConfiguration.class)
public interface StaticIssuerFeignClient {

    @PostMapping("/{issuerSlug}/{variantPath}/getStaticCredentialOfferURL")
    Tg4uStaticCredentialOfferResponse getStaticCredentialOfferURL(
            @PathVariable String issuerSlug,
            @PathVariable String variantPath,
            @RequestBody Tg4uStaticCredentialOfferRequest body);
}
