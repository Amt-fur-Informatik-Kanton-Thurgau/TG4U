package org.heidiverse.heidi.issuer.extensions.staticoffer;

import jakarta.validation.constraints.NotBlank;

public record StaticCredentialOfferRequest(@NotBlank String processToken) {}
