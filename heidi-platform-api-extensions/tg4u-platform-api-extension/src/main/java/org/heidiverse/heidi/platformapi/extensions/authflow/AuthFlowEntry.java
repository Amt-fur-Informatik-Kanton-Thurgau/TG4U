package org.heidiverse.heidi.platformapi.extensions.authflow;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

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
        String audience) {}
