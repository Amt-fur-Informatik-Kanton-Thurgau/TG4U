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

import org.heidiverse.heidi.entity.data.repository.CredentialSchemeRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowRepository;
import org.heidiverse.heidi.entity.model.entity.CredentialSchemeEntity;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowEntity;
import org.heidiverse.heidi.entity.model.credential.CredentialData;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class StaticFlowDataService {

    private final StaticFlowRepository staticFlowRepository;
    private final CredentialSchemeRepository credentialSchemeRepository;

    public StaticFlowDataService(
            StaticFlowRepository staticFlowRepository,
            CredentialSchemeRepository credentialSchemeRepository) {
        this.staticFlowRepository = staticFlowRepository;
        this.credentialSchemeRepository = credentialSchemeRepository;
    }

    @Transactional
    public StaticFlowEntity insertStaticFlow(
            UUID credentialSchemeUuid,
            String issuerSlug,
            String displayName,
            Map<String, CredentialData.AttributeDefinition> attributes,
            boolean includeTxCode) {
        StaticFlowEntity entity = new StaticFlowEntity();
        entity.setUuid(UUID.randomUUID());
        entity.setIssuerSlug(issuerSlug);
        entity.setDisplayName(displayName);
        entity.setAttributes(attributes);
        entity.setCreatedAt(Instant.now());
        if (includeTxCode) {
            entity.setTxCode(generateTransactionCode());
        }
        CredentialSchemeEntity credentialScheme =
                credentialSchemeRepository
                        .findByUuid(credentialSchemeUuid)
                        .orElseThrow(() -> new RuntimeException("Credential scheme not found"));
        entity.setCredentialScheme(credentialScheme);
        return staticFlowRepository.save(entity);
    }

    @Transactional
    public StaticFlowEntity updateStaticFlow(
            UUID uuid,
            UUID credentialSchemeUuid,
            String issuerSlug,
            String displayName,
            Map<String, CredentialData.AttributeDefinition> attributes) {
        StaticFlowEntity entity = staticFlowRepository.findByUuid(uuid);
        if (entity == null) {
            throw new RuntimeException("Static flow not found");
        }
        CredentialSchemeEntity credentialScheme =
                credentialSchemeRepository
                        .findByUuid(credentialSchemeUuid)
                        .orElseThrow(() -> new RuntimeException("Credential scheme not found"));
        entity.setCredentialScheme(credentialScheme);
        entity.setIssuerSlug(issuerSlug);
        entity.setDisplayName(displayName);
        entity.setAttributes(attributes);
        entity.setUpdatedAt(Instant.now());
        return staticFlowRepository.save(entity);
    }

    @Transactional
    public boolean deleteStaticFlow(UUID uuid) {
        StaticFlowEntity entity = staticFlowRepository.findByUuid(uuid);
        if (entity == null) {
            return false;
        }
        staticFlowRepository.delete(entity);
        return true;
    }

    private String generateTransactionCode() {
        return String.format("%06d", (int) (Math.random() * 1_000_000));
    }
}
