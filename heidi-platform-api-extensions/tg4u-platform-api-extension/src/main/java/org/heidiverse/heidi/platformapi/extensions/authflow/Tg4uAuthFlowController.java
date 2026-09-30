package org.heidiverse.heidi.platformapi.extensions.authflow;

import org.heidiverse.heidi.coordinator.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** TG4U management API retained after the OSS auth-flow removal. */
@RestController
@RequestMapping("/management/v1/authflow")
public class Tg4uAuthFlowController {
    private final Tg4uIssuerAuthFeignClient issuer;
    private final AuthenticationService authentication;
    private final String issuerApiKey;

    public Tg4uAuthFlowController(
            Tg4uIssuerAuthFeignClient issuer,
            AuthenticationService authentication,
            @Value("${HEIDI_ISSUER_API_KEY}") String issuerApiKey) {
        this.issuer = issuer;
        this.authentication = authentication;
        this.issuerApiKey = issuerApiKey;
    }

    @PostMapping
    public AuthFlowEntry create(@RequestBody AuthFlowEntry request) {
        authentication.performJwtAuthentication(request.credentialIdentifier());
        return issuer.create(issuerApiKey, AuthFlowWriteRequest.from(request));
    }

    @PutMapping("/{uuid}")
    public AuthFlowEntry update(@PathVariable UUID uuid, @RequestBody AuthFlowEntry request) {
        authentication.performJwtAuthentication(request.credentialIdentifier());
        return issuer.update(issuerApiKey, uuid, AuthFlowWriteRequest.from(request));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        AuthFlowEntry current = issuer.get(uuid);
        authentication.performJwtAuthentication(current.credentialIdentifier());
        issuer.delete(issuerApiKey, uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuid}")
    public AuthFlowEntry get(@PathVariable UUID uuid) {
        AuthFlowEntry current = issuer.get(uuid);
        authentication.performJwtAuthentication(current.credentialIdentifier());
        return current;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public List<AuthFlowEntry> all(@RequestParam(value = "hasDcqlQuery", defaultValue = "false") boolean hasDcqlQuery) {
        return issuer.all(hasDcqlQuery);
    }
}
