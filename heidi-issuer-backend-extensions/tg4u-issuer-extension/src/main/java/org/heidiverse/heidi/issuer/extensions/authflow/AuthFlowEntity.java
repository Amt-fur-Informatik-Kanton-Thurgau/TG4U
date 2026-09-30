package org.heidiverse.heidi.issuer.extensions.authflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Tg4u-only configuration for an external OIDC authorization provider. */
@Entity
@Table(name = "tg4u_auth_flow")
public class AuthFlowEntity {
    @Id
    private UUID uuid;
    @Column(name = "credential_identifier", nullable = false)
    private String credentialIdentifier;
    @Column(name = "credential_version", nullable = false)
    private String credentialVersion;
    @Column(name = "dcql_query", columnDefinition = "text")
    private String dcqlQuery;
    @Column(columnDefinition = "text")
    private String credentialMapping;
    private String displayName;
    private String clientId;
    @Column(name = "client_secret_ciphertext", nullable = false, columnDefinition = "text")
    private String clientSecretCiphertext;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String scope;
    private String jwksEndpoint;
    private String providerIssuer;
    private String audience;

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public String getCredentialIdentifier() { return credentialIdentifier; }
    public void setCredentialIdentifier(String value) { credentialIdentifier = value; }
    public String getCredentialVersion() { return credentialVersion; }
    public void setCredentialVersion(String value) { credentialVersion = value; }
    public String getDcqlQuery() { return dcqlQuery; }
    public void setDcqlQuery(String value) { dcqlQuery = value; }
    public String getCredentialMapping() { return credentialMapping; }
    public void setCredentialMapping(String value) { credentialMapping = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getClientId() { return clientId; }
    public void setClientId(String value) { clientId = value; }
    public String getClientSecretCiphertext() { return clientSecretCiphertext; }
    public void setClientSecretCiphertext(String value) { clientSecretCiphertext = value; }
    public String getAuthorizeEndpoint() { return authorizeEndpoint; }
    public void setAuthorizeEndpoint(String value) { authorizeEndpoint = value; }
    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String value) { tokenEndpoint = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public String getJwksEndpoint() { return jwksEndpoint; }
    public void setJwksEndpoint(String value) { jwksEndpoint = value; }
    public String getProviderIssuer() { return providerIssuer; }
    public void setProviderIssuer(String value) { providerIssuer = value; }
    public String getAudience() { return audience; }
    public void setAudience(String value) { audience = value; }
}
