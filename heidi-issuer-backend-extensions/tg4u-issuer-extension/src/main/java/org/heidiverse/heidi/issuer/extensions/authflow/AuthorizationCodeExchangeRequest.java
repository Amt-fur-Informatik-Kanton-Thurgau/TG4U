package org.heidiverse.heidi.issuer.extensions.authflow;

public record AuthorizationCodeExchangeRequest(String state, String code) {}
