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

import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowEntity;
import org.heidiverse.heidi.entity.model.credential.CredentialData;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StaticFlowOverview(List<StaticFlowElement> staticFlows) {

    public record CredentialSchemeReference(
            @NotNull UUID id,
            String credentialIdentifier,
            String version,
            String issuanceProfileId) {}

    public record StaticFlowElement(
            UUID uuid,
            CredentialSchemeReference credentialScheme,
            String issuerSlug,
            String displayName,
            Map<String, CredentialData.AttributeDefinition> attributes,
            String txCode) {

        public static StaticFlowElement fromEntity(StaticFlowEntity entity) {
            var entityScheme = entity.getCredentialScheme();
            return new StaticFlowElement(
                    entity.getUuid(),
                    new CredentialSchemeReference(
                            entityScheme.getUuid(),
                            entityScheme.getCredentialIdentifier(),
                            entityScheme.getVersion(),
                            entityScheme.getIssuanceProfileId()),
                    entity.getIssuerSlug(),
                    entity.getDisplayName(),
                    entity.getAttributes(),
                    entity.getTxCode());
        }
    }
}
