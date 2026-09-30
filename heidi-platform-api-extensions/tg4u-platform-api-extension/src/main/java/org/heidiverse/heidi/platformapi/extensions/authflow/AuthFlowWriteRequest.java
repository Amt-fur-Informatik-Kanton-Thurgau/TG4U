package org.heidiverse.heidi.platformapi.extensions.authflow;

import java.util.UUID;

/** Internal platform-to-issuer request; this type is never used as an API response. */
public record AuthFlowWriteRequest(
        UUID uuid,
        String credentialIdentifier,
        String credentialVersion,
        String dcqlQuery,
        String credentialMapping,
        String displayName,
        String clientId,
        String clientSecret,
        String authorizeEndpoint,
        String tokenEndpoint,
        String scope,
        String jwksEndpoint,
        String providerIssuer,
        String audience) {
    public static AuthFlowWriteRequest from(AuthFlowEntry entry) {
        return new AuthFlowWriteRequest(
                entry.uuid(), entry.credentialIdentifier(), entry.credentialVersion(),
                entry.dcqlQuery(), entry.credentialMapping(), entry.displayName(),
                entry.clientId(), entry.clientSecret(), entry.authorizeEndpoint(), entry.tokenEndpoint(),
                entry.scope(), entry.jwksEndpoint(), entry.providerIssuer(), entry.audience());
    }
}
