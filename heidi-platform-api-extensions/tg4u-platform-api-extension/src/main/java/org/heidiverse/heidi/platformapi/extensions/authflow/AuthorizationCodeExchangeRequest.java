package org.heidiverse.heidi.platformapi.extensions.authflow;

public record AuthorizationCodeExchangeRequest(String state, String code) {}
