package org.heidiverse.heidi.issuer.extensions.authflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthFlowService {
    private final AuthFlowRepository repository;
    private final Tg4uAuthSecretCrypto secretCrypto;

    public AuthFlowService(AuthFlowRepository repository, Tg4uAuthSecretCrypto secretCrypto) {
        this.repository = repository;
        this.secretCrypto = secretCrypto;
    }

    @Transactional
    public AuthFlowEntity create(AuthFlowEntry entry) {
        AuthFlowEntity entity = entry.toEntity();
        if (entity.getUuid() == null) entity.setUuid(UUID.randomUUID());
        if (repository.existsById(entity.getUuid())) throw new IllegalArgumentException("Auth flow already exists");
        applySecret(entity, entry.clientSecret());
        validate(entity);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public AuthFlowEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Auth flow not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AuthFlowEntity> all(boolean withPresentationDefinition) {
        return withPresentationDefinition ? repository.findByDcqlQueryIsNotNull() : repository.findAll();
    }

    @Transactional
    public AuthFlowEntity update(UUID id, AuthFlowEntry entry) {
        AuthFlowEntity old = get(id);
        AuthFlowEntity replacement = entry.toEntity();
        replacement.setUuid(id);
        if (entry.clientSecret() == null || entry.clientSecret().isBlank()) {
            replacement.setClientSecretCiphertext(old.getClientSecretCiphertext());
        } else {
            applySecret(replacement, entry.clientSecret());
        }
        validate(replacement);
        return repository.save(replacement);
    }

    @Transactional
    public void delete(UUID id) { repository.delete(get(id)); }

    public String clientSecret(AuthFlowEntity entity) {
        return secretCrypto.decrypt(entity.getClientSecretCiphertext());
    }

    private void applySecret(AuthFlowEntity entity, String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("clientSecret is required");
        entity.setClientSecretCiphertext(secretCrypto.encrypt(secret));
    }

    private void validate(AuthFlowEntity entity) {
        require(entity.getCredentialIdentifier(), "credentialIdentifier");
        require(entity.getCredentialVersion(), "credentialVersion");
        require(entity.getClientId(), "clientId");
        require(entity.getClientSecretCiphertext(), "clientSecret");
        require(entity.getAuthorizeEndpoint(), "authorizeEndpoint");
        require(entity.getTokenEndpoint(), "tokenEndpoint");
        require(entity.getJwksEndpoint(), "jwksEndpoint");
        require(entity.getProviderIssuer(), "providerIssuer");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
