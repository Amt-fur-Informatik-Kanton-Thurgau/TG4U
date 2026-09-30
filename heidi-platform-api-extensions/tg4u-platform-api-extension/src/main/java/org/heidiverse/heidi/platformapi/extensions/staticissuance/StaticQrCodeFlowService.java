/*
 * Copyright 2026 Ubique Innovation AG
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

import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowDataService;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowEntity;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.CreateStaticQrCodeFlowElement;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowElementOverview;
import org.heidiverse.heidi.entity.service.utils.JwtUtils;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Service
public class StaticQrCodeFlowService {

    private final StaticQrCodeFlowDataService staticQrCodeFlowDataService;

    public StaticQrCodeFlowService(StaticQrCodeFlowDataService staticQrCodeFlowDataService) {
        this.staticQrCodeFlowDataService = staticQrCodeFlowDataService;
    }

    @Transactional
    public List<StaticQrCodeFlowElementOverview> getOverview(String tenantId) {
        String validatedTenantId = JwtUtils.validateAndResolveTenantId(tenantId);
        List<StaticQrCodeFlowEntity> entities =
                staticQrCodeFlowDataService.findAllForTenantId(validatedTenantId);
        return entities.stream().map(StaticQrCodeFlowElementOverview::from).toList();
    }

    @Transactional
    public StaticQrCodeFlowElementOverview insertStaticQrCodeFlow(
            @Valid @RequestBody CreateStaticQrCodeFlowElement createStaticQrCodeFlowElement) {
        // Validate and resolve tenantId from JWT
        String validatedTenantId =
                JwtUtils.validateAndResolveTenantId(createStaticQrCodeFlowElement.tenantId());

        var entity =
                staticQrCodeFlowDataService.insertStaticQrCodeFlow(
                        createStaticQrCodeFlowElement.actionPayload(),
                        createStaticQrCodeFlowElement.displayName(),
                        validatedTenantId);

        return StaticQrCodeFlowElementOverview.from(entity);
    }

    @Transactional
    public boolean updateStaticQrCodeFlow(
            @Valid @RequestBody StaticQrCodeFlowElementOverview staticQrCodeFlowElementOverview) {
        // First verify the flow exists and user has access
        var existingEntity =
                staticQrCodeFlowDataService.findByUuid(staticQrCodeFlowElementOverview.uuid());
        if (existingEntity == null) {
            throw new RuntimeException("Static QR code flow not found");
        }
        JwtUtils.validateAndResolveTenantId(existingEntity.getTenantId());

        // Validate the tenantId in the update request matches
        String validatedTenantId =
                JwtUtils.validateAndResolveTenantId(staticQrCodeFlowElementOverview.tenantId());

        var entity =
                staticQrCodeFlowDataService.updateStaticQrCodeFlow(
                        staticQrCodeFlowElementOverview.uuid(),
                        staticQrCodeFlowElementOverview.actionPayload(),
                        staticQrCodeFlowElementOverview.displayName(),
                        validatedTenantId);
        return entity != null;
    }

    @Transactional
    public boolean deleteStaticQrCodeFlow(UUID uuid) {
        var entity = staticQrCodeFlowDataService.findByUuid(uuid);
        if (entity == null) {
            return false;
        }
        // Verify user has access to this tenant's flows
        JwtUtils.validateAndResolveTenantId(entity.getTenantId());
        return staticQrCodeFlowDataService.deleteStaticQrCodeFlow(uuid);
    }

    @Transactional
    public StaticQrCodeFlowElementOverview getStaticQrCodeFlow(UUID uuid) {
        var entity = staticQrCodeFlowDataService.findByUuid(uuid);
        if (entity == null) {
            throw new RuntimeException("Static QR code flow not found");
        }
        // Ensure user can only access flows from their tenant
        JwtUtils.validateAndResolveTenantId(entity.getTenantId());
        return StaticQrCodeFlowElementOverview.from(entity);
    }
}
