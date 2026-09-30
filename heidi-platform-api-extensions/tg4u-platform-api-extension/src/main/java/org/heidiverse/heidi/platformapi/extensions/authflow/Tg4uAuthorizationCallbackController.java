package org.heidiverse.heidi.platformapi.extensions.authflow;

import org.heidiverse.heidi.coordinator.model.CredentialContextResponse;
import org.heidiverse.heidi.coordinator.model.oid4vci.Action;
import org.heidiverse.heidi.coordinator.model.oid4vci.ActionPayload;
import org.heidiverse.heidi.coordinator.model.oid4vci.PreAuthIssuanceData;
import org.heidiverse.heidi.coordinator.model.oid4vci.SignedData;
import org.heidiverse.heidi.coordinator.model.issuance.IssuanceData;
import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.coordinator.service.CoordinatorEntityGateway;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/** Public callback for the external provider; all mutable auth state remains in the issuer. */
@RestController
public class Tg4uAuthorizationCallbackController {
    private final Tg4uIssuerAuthFeignClient issuer;
    private final CoordinatorEntityGateway entity;
    private final TokenSignatureService tokenSignatureService;
    private final String issuerApiKey;

    public Tg4uAuthorizationCallbackController(
            Tg4uIssuerAuthFeignClient issuer,
            CoordinatorEntityGateway entity,
            TokenSignatureService tokenSignatureService,
            @Value("${HEIDI_ISSUER_API_KEY}") String issuerApiKey) {
        this.issuer = issuer;
        this.entity = entity;
        this.tokenSignatureService = tokenSignatureService;
        this.issuerApiKey = issuerApiKey;
    }

    @GetMapping("/v1/oidc/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state) {
        AuthorizationCallbackContext context = issuer.context(issuerApiKey, state);
        Map<String, Object> claims = issuer.exchange(
                issuerApiKey, new AuthorizationCodeExchangeRequest(state, code));
        Set<String> claimNames = credentialClaimNames(
                context.credentialIdentifier(), context.credentialVersion());
        var credentialScheme = entity.getCredentialSchemeIssuer(
                context.credentialIdentifier(), context.credentialVersion());
        Map<String, Object> values = claims.entrySet().stream()
                .filter(entry -> claimNames.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (claimNames.contains("issuedAt")) values.put("issuedAt", ZonedDateTime.now().toString());

        PreAuthIssuanceData data = new PreAuthIssuanceData(
                new IssuanceData.SchemaIdentifier(
                        context.credentialIdentifier(), context.credentialVersion()),
                values,
                null,
                context.issuerSlug(),
                false,
                credentialScheme.credentialOfferType(),
                credentialScheme.issuanceProfileId());
        SignedData signed = new SignedData(
                ZonedDateTime.now(),
                ZonedDateTime.now().plusMinutes(5),
                new ActionPayload(Action.PRE_AUTH_ISSUANCE, data, null));
        SignatureTokenWithTxCode processToken = tokenSignatureService.generateToken(
                signed, context.credentialIdentifier(), false);
        OidcAuthSuccessResponse response = issuer.success(
                issuerApiKey, new OidcAuthSuccessRequest(context.connectionId(), processToken.token()));
        if (response == null || response.redirectUri() == null || response.redirectUri().isBlank()) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Issuer returned no wallet redirect");
        }
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, response.redirectUri()).build();
    }

    private Set<String> credentialClaimNames(String identifier, String version) {
        CredentialContextResponse response = entity.getCredentialContext(identifier, version);
        if (response == null || response.context() == null) return Set.of();
        Set<String> result = new HashSet<>();
        for (CredentialContextResponse.CredentialContext context : response.context().values()) {
            if (context != null && context.claims() != null) result.addAll(context.claims().keySet());
        }
        return result;
    }
}
