package org.heidiverse.heidi.issuer.extensions.authflow;

import java.util.UUID;

/** Internal callback context; the provider secret never leaves the issuer service. */
public record AuthorizationCallbackContext(
        UUID authFlowId,
        String connectionId,
        String issuerSlug,
        String credentialIdentifier,
        String credentialVersion,
        String clientId,
        String redirectUri) {}
