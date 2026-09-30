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

import org.heidiverse.heidi.platformapi.extensions.staticissuance.CreateStaticQrCodeFlowElement;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowElementOverview;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowOverview;
import org.heidiverse.heidi.platformapi.extensions.staticissuance.StaticQrCodeFlowService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/management/v1/static-qr-code-flow")
@CrossOrigin(originPatterns = "*")
public class StaticQrCodeFlowController {

    private final StaticQrCodeFlowService staticQrCodeFlowService;

    public StaticQrCodeFlowController(StaticQrCodeFlowService staticQrCodeFlowService) {
        this.staticQrCodeFlowService = staticQrCodeFlowService;
    }

    @Operation(summary = "Get static QR code flow overview, optionally filtered by tenant")
    @GetMapping("/overview")
    public ResponseEntity<StaticQrCodeFlowOverview> getOverview(
            @RequestParam(required = false) String tenantId) {
        var flows = staticQrCodeFlowService.getOverview(tenantId);
        return ResponseEntity.ok(new StaticQrCodeFlowOverview(flows));
    }

    @Operation(summary = "Retrieve details about one static QR code flow")
    @GetMapping("/{uuid}")
    public ResponseEntity<StaticQrCodeFlowElementOverview> getFlow(@PathVariable UUID uuid) {
        try {
            return ResponseEntity.ok(staticQrCodeFlowService.getStaticQrCodeFlow(uuid));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create a new static QR code flow")
    @PostMapping("")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<StaticQrCodeFlowElementOverview> createFlow(
            @Valid @RequestBody CreateStaticQrCodeFlowElement createRequest) {
        try {
            var created = staticQrCodeFlowService.insertStaticQrCodeFlow(createRequest);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a static QR code flow")
    @PutMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<StaticQrCodeFlowElementOverview> updateFlow(
            @PathVariable UUID uuid,
            @Valid @RequestBody StaticQrCodeFlowElementOverview updateRequest) {
        try {
            var success = staticQrCodeFlowService.updateStaticQrCodeFlow(updateRequest);
            return success
                    ? ResponseEntity.ok(updateRequest)
                    : ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAnyAuthority('EDITOR', 'ADMIN', 'MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<String> deleteFlow(@PathVariable UUID uuid) {
        boolean result = staticQrCodeFlowService.deleteStaticQrCodeFlow(uuid);
        return result
                ? ResponseEntity.ok("Successfully deleted")
                : ResponseEntity.badRequest().body("Deletion failed");
    }
}
