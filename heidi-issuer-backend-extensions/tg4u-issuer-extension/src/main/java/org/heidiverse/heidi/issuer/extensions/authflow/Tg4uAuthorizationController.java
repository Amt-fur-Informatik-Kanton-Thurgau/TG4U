package org.heidiverse.heidi.issuer.extensions.authflow;

import org.heidiverse.heidi.issuer.model.FlowVariant;
import org.heidiverse.heidi.issuer.model.api.CredentialOfferResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
public class Tg4uAuthorizationController {
    private final Tg4uAuthorizationService service;
    private final String apiKey;

    public Tg4uAuthorizationController(
            Tg4uAuthorizationService service,
            @Value("${heidi.issuer.api-key}") String apiKey) {
        this.service = service;
        this.apiKey = apiKey;
    }

    @GetMapping("/{issuerSlug}/{variantPath}/credential-offer")
    public CredentialOfferResponse credentialOffer(
            @PathVariable String issuerSlug,
            @PathVariable String variantPath,
            @RequestParam UUID authFlowId) {
        return service.createOffer(issuerSlug, FlowVariant.fromPath(variantPath), authFlowId);
    }

    @GetMapping("/v1/oidc/context/{externalState}")
    public AuthorizationCallbackContext context(
            @RequestHeader("X-API-KEY") String supplied,
            @PathVariable String externalState) {
        authenticate(supplied);
        return service.context(externalState);
    }

    @PostMapping("/oidc/auth/exchange")
    public Map<String, Object> exchange(
            @RequestHeader("X-API-KEY") String supplied,
            @RequestBody AuthorizationCodeExchangeRequest request) {
        authenticate(supplied);
        return service.exchange(request);
    }

    @PostMapping("/oidc/auth/success")
    public OidcAuthSuccessResponse success(
            @RequestHeader("X-API-KEY") String supplied,
            @RequestBody OidcAuthSuccessRequest request) {
        authenticate(supplied);
        return service.complete(request.connectionId(), request.processToken());
    }

    private void authenticate(String supplied) {
        if (apiKey == null || supplied == null
                || !MessageDigest.isEqual(
                        apiKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Unauthorized: invalid issuer API key");
        }
    }
}
