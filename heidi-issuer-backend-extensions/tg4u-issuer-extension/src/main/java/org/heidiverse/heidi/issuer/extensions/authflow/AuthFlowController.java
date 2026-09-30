package org.heidiverse.heidi.issuer.extensions.authflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/authflow")
public class AuthFlowController {
    private final AuthFlowService service;
    private final String apiKey;

    public AuthFlowController(
            AuthFlowService service,
            @Value("${heidi.issuer.api-key}") String apiKey) {
        this.service = service;
        this.apiKey = apiKey;
    }

    @PostMapping
    public AuthFlowEntry create(@RequestHeader("X-API-KEY") String supplied, @RequestBody AuthFlowEntry request) {
        authenticate(supplied);
        return AuthFlowEntry.fromEntity(service.create(request));
    }

    @PutMapping("/{uuid}")
    public AuthFlowEntry update(
            @RequestHeader("X-API-KEY") String supplied,
            @PathVariable UUID uuid,
            @RequestBody AuthFlowEntry request) {
        authenticate(supplied);
        return AuthFlowEntry.fromEntity(service.update(uuid, request));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-API-KEY") String supplied,
            @PathVariable UUID uuid) {
        authenticate(supplied);
        service.delete(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuid}")
    public AuthFlowEntry get(@PathVariable UUID uuid) {
        return AuthFlowEntry.fromEntity(service.get(uuid));
    }

    @GetMapping
    public List<AuthFlowEntry> all(
            @RequestParam(value = "hasDcqlQuery", defaultValue = "false") boolean hasDcqlQuery) {
        return service.all(hasDcqlQuery).stream().map(AuthFlowEntry::fromEntity).toList();
    }

    private void authenticate(String supplied) {
        if (apiKey == null || supplied == null
                || !MessageDigest.isEqual(
                        apiKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Unauthorized: invalid issuer API key");
        }
    }
}
