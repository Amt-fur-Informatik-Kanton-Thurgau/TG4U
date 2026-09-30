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

import org.heidiverse.heidi.coordinator.model.api.InitializeProcessRequest;
import org.heidiverse.heidi.coordinator.model.oid4vci.ActionPayload;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowRepository;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowEntity;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StaticQrCodeFlowDataService {

    private final StaticQrCodeFlowRepository staticQrCodeFlowRepository;

    public StaticQrCodeFlowDataService(
            StaticQrCodeFlowRepository staticQrCodeFlowRepository
    ) {
        this.staticQrCodeFlowRepository = staticQrCodeFlowRepository;
    }


    @Transactional
    public StaticQrCodeFlowEntity findByUuid(UUID uuid) {
        return staticQrCodeFlowRepository.findByUuid(uuid);
    }

    @Transactional
    public StaticQrCodeFlowEntity insertStaticQrCodeFlow(
            InitializeProcessRequest actionPayload,
            String displayName,
            String tenantId
    ) {
        StaticQrCodeFlowEntity entity = new StaticQrCodeFlowEntity();
        entity.setUuid(UUID.randomUUID());
        entity.setActionPayload(actionPayload);
        entity.setDisplayName(displayName);
        entity.setTenantId(tenantId);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        return staticQrCodeFlowRepository.save(entity);
    }

    @Transactional
    public StaticQrCodeFlowEntity updateStaticQrCodeFlow(
            UUID uuid,
            InitializeProcessRequest actionPayload,
            String displayName,
            String tenantId
    ) {

        StaticQrCodeFlowEntity entity = staticQrCodeFlowRepository.findByUuid(uuid);
        if (entity == null) {
            throw new RuntimeException("Static QR code flow not found");
        }
        entity.setActionPayload(actionPayload);
        entity.setDisplayName(displayName);
        entity.setTenantId(tenantId);
        entity.setUpdatedAt(Instant.now());

        return staticQrCodeFlowRepository.save(entity);
    }

    @Transactional
    public boolean deleteStaticQrCodeFlow(UUID uuid) {
        StaticQrCodeFlowEntity entity = staticQrCodeFlowRepository.findByUuid(uuid);
        if (entity == null) {
            return false;
        }
        staticQrCodeFlowRepository.delete(entity);
        return true;
    }

    @Transactional
    public List<StaticQrCodeFlowEntity> findAllForTenantId(String tenantId) {
        return staticQrCodeFlowRepository.findByTenantId(tenantId);
    }
}
