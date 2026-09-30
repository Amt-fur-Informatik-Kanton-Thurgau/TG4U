package org.heidiverse.heidi.platformapi.extensions.authflow;

import org.heidiverse.heidi.platformapi.extensions.Tg4uIssuerFeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "tg4uIssuerAuthFeignClient",
        url = "${heidi.platform.issuer-internal-base-url}",
        configuration = Tg4uIssuerFeignClientConfiguration.class)
public interface Tg4uIssuerAuthFeignClient {
    @PostMapping(value = "/v1/authflow", consumes = "application/json")
    AuthFlowEntry create(@RequestHeader("X-API-KEY") String apiKey, @RequestBody AuthFlowWriteRequest request);
    @PutMapping(value = "/v1/authflow/{uuid}", consumes = "application/json")
    AuthFlowEntry update(@RequestHeader("X-API-KEY") String apiKey, @PathVariable UUID uuid, @RequestBody AuthFlowWriteRequest request);
    @DeleteMapping("/v1/authflow/{uuid}")
    void delete(@RequestHeader("X-API-KEY") String apiKey, @PathVariable UUID uuid);
    @GetMapping("/v1/authflow/{uuid}")
    AuthFlowEntry get(@PathVariable UUID uuid);
    @GetMapping("/v1/authflow")
    List<AuthFlowEntry> all(@RequestParam(value = "hasDcqlQuery", defaultValue = "false") boolean hasDcqlQuery);
    @GetMapping("/{issuerSlug}/{variantPath}/credential-offer")
    Map<String, Object> credentialOffer(
            @PathVariable String issuerSlug,
            @PathVariable String variantPath,
            @RequestParam UUID authFlowId);
    @GetMapping("/v1/oidc/context/{externalState}")
    AuthorizationCallbackContext context(@RequestHeader("X-API-KEY") String apiKey, @PathVariable String externalState);
    @PostMapping(value = "/oidc/auth/exchange", consumes = "application/json")
    Map<String, Object> exchange(@RequestHeader("X-API-KEY") String apiKey, @RequestBody AuthorizationCodeExchangeRequest request);
    @PostMapping(value = "/oidc/auth/success", consumes = "application/json")
    OidcAuthSuccessResponse success(@RequestHeader("X-API-KEY") String apiKey, @RequestBody OidcAuthSuccessRequest request);
}
