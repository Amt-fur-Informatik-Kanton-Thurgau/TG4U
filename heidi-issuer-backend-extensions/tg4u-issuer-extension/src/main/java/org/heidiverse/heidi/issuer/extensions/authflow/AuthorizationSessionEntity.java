package org.heidiverse.heidi.issuer.extensions.authflow;

import org.heidiverse.heidi.issuer.model.FlowVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tg4u_authorization_session")
public class AuthorizationSessionEntity {
    @Id private UUID id;
    @Column(name = "auth_flow_id", nullable = false) private UUID authFlowId;
    @Column(nullable = false) private String issuerSlug;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FlowVariant variant;
    @Column(nullable = false) private String credentialIdentifier;
    @Column(nullable = false) private String credentialVersion;
    @Column(name = "issuer_state", nullable = false, unique = true) private String issuerState;
    @Column(name = "external_state", nullable = false, unique = true) private String externalState;
    @Column(name = "connection_id", nullable = false, unique = true) private String connectionId;
    private String redirectUri;
    private String clientState;
    private String codeChallenge;
    private String requestedDpopJkt;
    @Column(unique = true) private String authorizationCode;
    private String preAuthorizedCode;
    private String issuerConnectionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AuthorizationSessionStatus status;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist
    void initialize() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = AuthorizationSessionStatus.OFFER_CREATED;
    }

    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getAuthFlowId() { return authFlowId; }
    public void setAuthFlowId(UUID value) { authFlowId = value; }
    public String getIssuerSlug() { return issuerSlug; }
    public void setIssuerSlug(String value) { issuerSlug = value; }
    public FlowVariant getVariant() { return variant; }
    public void setVariant(FlowVariant value) { variant = value; }
    public String getCredentialIdentifier() { return credentialIdentifier; }
    public void setCredentialIdentifier(String value) { credentialIdentifier = value; }
    public String getCredentialVersion() { return credentialVersion; }
    public void setCredentialVersion(String value) { credentialVersion = value; }
    public String getIssuerState() { return issuerState; }
    public void setIssuerState(String value) { issuerState = value; }
    public String getExternalState() { return externalState; }
    public void setExternalState(String value) { externalState = value; }
    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String value) { connectionId = value; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String value) { redirectUri = value; }
    public String getClientState() { return clientState; }
    public void setClientState(String value) { clientState = value; }
    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String value) { codeChallenge = value; }
    public String getRequestedDpopJkt() { return requestedDpopJkt; }
    public void setRequestedDpopJkt(String value) { requestedDpopJkt = value; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String value) { authorizationCode = value; }
    public String getPreAuthorizedCode() { return preAuthorizedCode; }
    public void setPreAuthorizedCode(String value) { preAuthorizedCode = value; }
    public String getIssuerConnectionId() { return issuerConnectionId; }
    public void setIssuerConnectionId(String value) { issuerConnectionId = value; }
    public AuthorizationSessionStatus getStatus() { return status; }
    public void setStatus(AuthorizationSessionStatus value) { status = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
