package org.heidiverse.heidi.platformapi.extensions.authflow;

import java.util.UUID;

public record AuthorizationCallbackContext(
        UUID authFlowId,
        String connectionId,
        String issuerSlug,
        String credentialIdentifier,
        String credentialVersion,
        String clientId,
        String redirectUri) {}
