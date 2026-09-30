/*
 * Copyright 2025 Ubique Innovation AG
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.coordinator.model.issuance.IssuanceData;
import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.coordinator.model.oid4vci.Action;
import org.heidiverse.heidi.coordinator.model.oid4vci.ActionPayload;
import org.heidiverse.heidi.coordinator.model.oid4vci.SignedData;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;
import org.heidiverse.heidi.entity.data.repository.CredentialSchemeRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowDataService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowEntity;
import org.heidiverse.heidi.entity.model.credential.CredentialData;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.CreateStaticFlowElement;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowOverview;
import org.heidiverse.heidi.entity.service.utils.JwtUtils;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StaticFlowService {

    private static final String CREDENTIAL_VARIANT = "c";

    private final StaticFlowDataService staticFlowDataService;
    private final StaticFlowRepository staticFlowRepository;
    private final CredentialSchemeRepository credentialSchemeRepository;
    private final TokenSignatureService tokenSignatureService;
    private final StaticIssuerFeignClient issuerFeignClient;
    private final String platformBaseUrl;

    public StaticFlowService(
            StaticFlowDataService staticFlowDataService,
            StaticFlowRepository staticFlowRepository,
            CredentialSchemeRepository credentialSchemeRepository,
            TokenSignatureService tokenSignatureService,
            StaticIssuerFeignClient issuerFeignClient,
            @Value("${heidi.platform.public-base-url}") String platformBaseUrl) {
        this.staticFlowDataService = staticFlowDataService;
        this.staticFlowRepository = staticFlowRepository;
        this.credentialSchemeRepository = credentialSchemeRepository;
        this.tokenSignatureService = tokenSignatureService;
        this.issuerFeignClient = issuerFeignClient;
        this.platformBaseUrl = platformBaseUrl;
    }

    @Transactional
    public StaticFlowOverview getOverview(String tenantId) {
        String validatedTenantId = JwtUtils.validateAndResolveTenantId(tenantId);
        List<StaticFlowEntity> entities =
                staticFlowRepository.findByCredentialScheme_TenantId(validatedTenantId);
        return new StaticFlowOverview(
                entities.stream().map(StaticFlowOverview.StaticFlowElement::fromEntity).toList());
    }

    @Transactional
    public StaticFlowOverview.StaticFlowElement insertStaticFlow(
            @Valid @RequestBody CreateStaticFlowElement createStaticFlowElement) {
        var credentialSchemeId = createStaticFlowElement.credentialScheme().id();
        var credentialScheme =
                credentialSchemeRepository
                        .findByUuid(credentialSchemeId)
                        .orElseThrow(() -> new RuntimeException("Credential scheme not found"));
        JwtUtils.validateAndResolveTenantId(credentialScheme.getTenantId());
        var entity =
                staticFlowDataService.insertStaticFlow(
                        credentialSchemeId,
                        createStaticFlowElement.issuerSlug(),
                        createStaticFlowElement.displayName(),
                        createStaticFlowElement.attributes(),
                        createStaticFlowElement.includeTxCode());
        return StaticFlowOverview.StaticFlowElement.fromEntity(entity);
    }

    @Transactional
    public boolean updateStaticFlow(
            @Valid @RequestBody StaticFlowOverview.StaticFlowElement staticFlowElement) {
        var credentialSchemeId = staticFlowElement.credentialScheme().id();
        var credentialScheme =
                credentialSchemeRepository
                        .findByUuid(credentialSchemeId)
                        .orElseThrow(() -> new RuntimeException("Credential scheme not found"));
        JwtUtils.validateAndResolveTenantId(credentialScheme.getTenantId());
        var entity =
                staticFlowDataService.updateStaticFlow(
                        staticFlowElement.uuid(),
                        credentialSchemeId,
                        staticFlowElement.issuerSlug(),
                        staticFlowElement.displayName(),
                        staticFlowElement.attributes());
        return entity != null;
    }

    @Transactional
    public boolean deleteStaticFlow(UUID uuid) {
        var entity = staticFlowRepository.findByUuid(uuid);
        if (entity == null) {
            return false;
        }
        var credentialScheme =
                credentialSchemeRepository
                        .findByUuid(entity.getCredentialScheme().getUuid())
                        .orElseThrow(() -> new RuntimeException("Credential scheme not found"));
        JwtUtils.validateAndResolveTenantId(credentialScheme.getTenantId());
        return staticFlowDataService.deleteStaticFlow(uuid);
    }

    @Transactional
    public StaticFlowOverview.StaticFlowElement getStaticFlow(UUID uuid) {
        var entity = staticFlowRepository.findByUuid(uuid);
        if (entity == null) {
            throw new RuntimeException("Static flow not found");
        }
        return StaticFlowOverview.StaticFlowElement.fromEntity(entity);
    }

    @Transactional
    public CredentialData getAttributesByUuid(UUID uuid) {
        var entity = staticFlowRepository.findByUuid(uuid);
        if (entity == null) {
            throw new RuntimeException("Static flow not found");
        }
        var scheme = entity.getCredentialScheme();
        return new CredentialData(
                new CredentialData.SchemaIdentifier(
                        scheme.getCredentialIdentifier(), scheme.getVersion()),
                entity.getAttributes());
    }

    @Transactional
    public SignatureTokenWithTxCode getProcessToken(UUID uuid) {
        return generateProcessToken(getStaticFlow(uuid));
    }

    @Transactional
    public URI getCredentialOfferUri(UUID uuid) {
        var staticFlow = getStaticFlow(uuid);
        var processToken = generateProcessToken(staticFlow);
        return URI.create(
                issuerFeignClient
                        .getStaticCredentialOfferURL(
                                staticFlow.issuerSlug(),
                                CREDENTIAL_VARIANT,
                                new Tg4uStaticCredentialOfferRequest(processToken.token()))
                        .credentialOfferURl());
    }

    private SignatureTokenWithTxCode generateProcessToken(
            StaticFlowOverview.StaticFlowElement staticFlow) {
        var scheme = staticFlow.credentialScheme();
        var attributeUrl =
                String.format(
                        "%s/public/v1/static-flow/attribute/%s",
                        platformBaseUrl,
                        staticFlow.uuid());
        var staticQrCodeData =
                new Tg4uStaticQrCodeData(
                        new IssuanceData.SchemaIdentifier(
                                scheme.credentialIdentifier(), scheme.version()),
                        staticFlow.issuerSlug(),
                        attributeUrl,
                        scheme.issuanceProfileId());
        var signedData =
                new SignedData(
                        ZonedDateTime.now(),
                        ZonedDateTime.now().plusMinutes(30),
                        new ActionPayload("static_qr_code", staticQrCodeData, null));

        var txCode = staticFlow.txCode();
        return tokenSignatureService.generateToken(
                signedData, scheme.credentialIdentifier(), txCode != null, txCode);
    }
}
