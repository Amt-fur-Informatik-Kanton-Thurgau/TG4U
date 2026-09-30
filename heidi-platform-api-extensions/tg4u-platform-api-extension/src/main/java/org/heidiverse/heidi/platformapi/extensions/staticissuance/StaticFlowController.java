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

import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.entity.model.credential.CredentialData;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.CreateStaticFlowElement;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowOverview;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticFlowService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/management/v1/static-flow")
@CrossOrigin(originPatterns = "*")
public class StaticFlowController {

    private final StaticFlowService staticFlowService;

    @Value("${HEIDI_ISSUER_API_KEY}")
    private String heidiIssuerApiKey;

    public StaticFlowController(StaticFlowService staticFlowService) {
        this.staticFlowService = staticFlowService;
    }

    @Operation(summary = "Get static flow overview, optionally filtered by tenant")
    @GetMapping("/overview")
    public ResponseEntity<StaticFlowOverview> getStaticFlowOverview(
            @RequestParam(required = false) String tenantId) {
        return ResponseEntity.ok(this.staticFlowService.getOverview(tenantId));
    }

    @Operation(summary = "Retrieve details about one static flow")
    @GetMapping("/{uuid}")
    public ResponseEntity<StaticFlowOverview.StaticFlowElement> getStaticFlow(
            @PathVariable UUID uuid) {
        return ResponseEntity.ok(this.staticFlowService.getStaticFlow(uuid));
    }

    @Operation(summary = "Create a new static flow")
    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<StaticFlowOverview.StaticFlowElement> createStaticFlow(
            @Valid @RequestBody CreateStaticFlowElement createStaticFlowElement) {
        var created = this.staticFlowService.insertStaticFlow(createStaticFlowElement);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a static flow")
    @PutMapping("")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<StaticFlowOverview.StaticFlowElement> updateStaticFlow(
            @RequestBody StaticFlowOverview.StaticFlowElement staticFlowElement) {
        var success = this.staticFlowService.updateStaticFlow(staticFlowElement);
        if (success) {
            return ResponseEntity.ok(staticFlowElement);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<String> deleteStaticFlow(@PathVariable UUID uuid) {
        boolean result = staticFlowService.deleteStaticFlow(uuid);
        return result
                ? ResponseEntity.ok("Ok")
                : ResponseEntity.badRequest().body("Deletion failed");
    }

    @Operation(summary = "Retrieve attributes for a static flow")
    @GetMapping("/attribute/{uuid}")
    public ResponseEntity<CredentialData> getAttributes(@PathVariable UUID uuid) {
        return ResponseEntity.ok(this.staticFlowService.getAttributesByUuid(uuid));
    }

    @Operation(summary = "Generate process token for a static flow")
    @GetMapping("/token/{uuid}")
    public ResponseEntity<SignatureTokenWithTxCode> getProcessToken(
            @PathVariable UUID uuid, @RequestHeader("X-API-KEY") String apiKey) {
        if (!heidiIssuerApiKey.equals(apiKey)) {
            throw new IllegalArgumentException("Unauthorized: Invalid API key");
        }
        return ResponseEntity.ok(this.staticFlowService.getProcessToken(uuid));
    }
}
