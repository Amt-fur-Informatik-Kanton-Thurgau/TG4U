// SPDX-FileCopyrightText: 2026 Ubique Innovation AG and Heidi contributors
// SPDX-License-Identifier: Apache-2.0

package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;

import org.heidiverse.heidi.coordinator.data.service.ProtocolProcessDataService;
import org.heidiverse.heidi.coordinator.model.api.InitializeProcessRequest;
import org.heidiverse.heidi.coordinator.model.CredentialSchemeTenantResponse;
import org.heidiverse.heidi.coordinator.model.issuance.IssuanceData;
import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.coordinator.model.oid4vci.SignedData;
import org.heidiverse.heidi.coordinator.service.AuthenticationService;
import org.heidiverse.heidi.coordinator.service.CoordinatorEntityGateway;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class StaticQrCodeProcessExtensionTest {

    @Test
    void createsStaticOfferWithoutCreatingCoordinatorProcess() {
        var objectMapper = new ObjectMapper();
        var staticData = new Tg4uStaticQrCodeData(
                new IssuanceData.SchemaIdentifier("card", "1.0"),
                "acme",
                "https://issuer.example/attribute/1",
                "SWISS_ISSUANCE_2026_1");
        var request = new InitializeProcessRequest();
        request.setAction(StaticQrCodeProcessExtension.ACTION);
        request.setExtensionData(
                StaticQrCodeProcessExtension.DATA_FIELD, objectMapper.valueToTree(staticData));

        var tokenSignature = mock(TokenSignatureService.class);
        when(tokenSignature.generateToken(any(SignedData.class), anyString(), anyBoolean()))
                .thenReturn(new SignatureTokenWithTxCode("process-token", null));
        var issuer = mock(StaticIssuerFeignClient.class);
        when(issuer.getStaticCredentialOfferURL(
                        eq("acme"), eq("c"), any(Tg4uStaticCredentialOfferRequest.class)))
                .thenReturn(new Tg4uStaticCredentialOfferResponse(
                        "https://issuer.example/acme/c/getCredentialOfferForUUID?uuid=offer"));
        var processData = mock(ProtocolProcessDataService.class);
        var entityGateway = mock(CoordinatorEntityGateway.class);
        when(entityGateway.getCredentialSchemeTenant("card"))
                .thenReturn(new CredentialSchemeTenantResponse("tenant", "card"));
        var extension = new StaticQrCodeProcessExtension(
                mock(AuthenticationService.class),
                entityGateway,
                tokenSignature,
                issuer,
                processData,
                objectMapper);

        var response = extension.createStaticCredentialOffer(
                request,
                "Basic service-credentials",
                ZonedDateTime.now(),
                ZonedDateTime.now().plusMinutes(5));

        assertThat(response.credentialOfferURl()).contains("getCredentialOfferForUUID");
        verify(processData, never()).createProcess(anyString(), any(), anyString());
    }
}
