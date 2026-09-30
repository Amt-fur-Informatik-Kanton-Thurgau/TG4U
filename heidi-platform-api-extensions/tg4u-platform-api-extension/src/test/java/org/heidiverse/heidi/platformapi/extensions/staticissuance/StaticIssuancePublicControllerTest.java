// SPDX-FileCopyrightText: 2026 Ubique Innovation AG and Heidi contributors
// SPDX-License-Identifier: Apache-2.0

package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StaticIssuancePublicControllerTest {

    @Test
    void redirectsToFreshIssuerOffer() {
        var flowId = UUID.randomUUID();
        var issuerOffer = URI.create("https://issuer.example/acme/c/getCredentialOfferForUUID?uuid=offer");
        var service = mock(StaticFlowService.class);
        when(service.getCredentialOfferUri(flowId)).thenReturn(issuerOffer);

        var response = new StaticIssuancePublicController(
                service, mock(StaticQrCodeFlowDataService.class)).getCredentialOffer(flowId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(issuerOffer);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
