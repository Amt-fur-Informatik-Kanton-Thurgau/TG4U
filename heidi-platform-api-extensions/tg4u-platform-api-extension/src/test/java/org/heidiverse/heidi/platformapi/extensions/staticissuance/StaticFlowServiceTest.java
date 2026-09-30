// SPDX-FileCopyrightText: 2026 Ubique Innovation AG and Heidi contributors
// SPDX-License-Identifier: Apache-2.0

package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.coordinator.model.oid4vci.ActionPayload;
import org.heidiverse.heidi.coordinator.model.oid4vci.SignedData;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;
import org.heidiverse.heidi.entity.data.repository.CredentialSchemeRepository;
import org.heidiverse.heidi.entity.model.entity.CredentialSchemeEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StaticFlowServiceTest {

    @Test
    void createsIssuerOfferFromFreshProcessToken() {
        var flowId = UUID.randomUUID();
        var scheme = new CredentialSchemeEntity();
        scheme.setUuid(UUID.randomUUID());
        scheme.setCredentialIdentifier("card");
        scheme.setVersion("1.0");
        scheme.setIssuanceProfileId("SWISS_ISSUANCE_2026_1");

        var flow = new StaticFlowEntity();
        flow.setUuid(flowId);
        flow.setIssuerSlug("acme");
        flow.setCredentialScheme(scheme);

        var flows = mock(StaticFlowRepository.class);
        when(flows.findByUuid(flowId)).thenReturn(flow);

        var tokenSignature = mock(TokenSignatureService.class);
        when(tokenSignature.generateToken(
                        any(SignedData.class), anyString(), anyBoolean(), nullable(String.class)))
                .thenReturn(new SignatureTokenWithTxCode("process-token", null));

        var issuer = mock(StaticIssuerFeignClient.class);
        when(issuer.getStaticCredentialOfferURL(
                        eq("acme"),
                        eq("c"),
                        any(Tg4uStaticCredentialOfferRequest.class)))
                .thenReturn(new Tg4uStaticCredentialOfferResponse(
                        "https://issuer.example/acme/c/getCredentialOfferForUUID?uuid=offer"));

        var service = new StaticFlowService(
                mock(StaticFlowDataService.class),
                flows,
                mock(CredentialSchemeRepository.class),
                tokenSignature,
                issuer,
                "https://platform.example");

        assertThat(service.getCredentialOfferUri(flowId))
                .hasToString("https://issuer.example/acme/c/getCredentialOfferForUUID?uuid=offer");
        var signedData = ArgumentCaptor.forClass(SignedData.class);
        verify(tokenSignature).generateToken(
                signedData.capture(), eq("card"), eq(false), isNull(String.class));
        var qrCodeData = (Tg4uStaticQrCodeData)
                ((ActionPayload) signedData.getValue().data()).getData();
        assertThat(qrCodeData.issuanceProfileId()).isEqualTo("SWISS_ISSUANCE_2026_1");
        verify(issuer).getStaticCredentialOfferURL(
                eq("acme"), eq("c"), any(Tg4uStaticCredentialOfferRequest.class));
    }
}
