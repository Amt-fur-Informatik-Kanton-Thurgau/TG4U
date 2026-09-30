package org.heidiverse.heidi.issuer.extensions.authflow;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** JSON shape compatible with the former auth-flow API. */
public record AuthFlowEntry(
        UUID uuid,
        String credentialIdentifier,
        String credentialVersion,
        @JsonAlias({"presentationDefinition", "presentation_definition", "dcql_query"}) String dcqlQuery,
        String credentialMapping,
        String displayName,
        String clientId,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String clientSecret,
        String authorizeEndpoint,
        String tokenEndpoint,
        String scope,
        String jwksEndpoint,
        String providerIssuer,
        String audience) {
    public static AuthFlowEntry fromEntity(AuthFlowEntity entity) {
        return new AuthFlowEntry(
                entity.getUuid(), entity.getCredentialIdentifier(), entity.getCredentialVersion(),
                entity.getDcqlQuery(), entity.getCredentialMapping(), entity.getDisplayName(),
                entity.getClientId(), null, entity.getAuthorizeEndpoint(), entity.getTokenEndpoint(), entity.getScope(),
                entity.getJwksEndpoint(), entity.getProviderIssuer(), entity.getAudience());
    }

    public AuthFlowEntity toEntity() {
        AuthFlowEntity entity = new AuthFlowEntity();
        entity.setUuid(uuid);
        entity.setCredentialIdentifier(credentialIdentifier);
        entity.setCredentialVersion(credentialVersion);
        entity.setDcqlQuery(dcqlQuery);
        entity.setCredentialMapping(credentialMapping);
        entity.setDisplayName(displayName);
        entity.setClientId(clientId);
        entity.setAuthorizeEndpoint(authorizeEndpoint);
        entity.setTokenEndpoint(tokenEndpoint);
        entity.setScope(scope);
        entity.setJwksEndpoint(jwksEndpoint);
        entity.setProviderIssuer(providerIssuer);
        entity.setAudience(audience);
        return entity;
    }
}
