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

import org.heidiverse.heidi.entity.model.credential.CredentialData;
import org.heidiverse.heidi.entity.model.entity.CredentialSchemeEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "t_static_flow")
public class StaticFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk_static_flow", nullable = false, unique = true)
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @OneToOne
    @JoinColumn(name = "fk_credential_scheme")
    private CredentialSchemeEntity credentialScheme;

    @Column(name = "attributes")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, CredentialData.AttributeDefinition> attributes;

    @Column(name = "issuer_slug", nullable = false)
    private String issuerSlug;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "tx_code")
    private String txCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public CredentialSchemeEntity getCredentialScheme() {
        return credentialScheme;
    }

    public void setCredentialScheme(CredentialSchemeEntity credentialScheme) {
        this.credentialScheme = credentialScheme;
    }

    public Map<String, CredentialData.AttributeDefinition> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, CredentialData.AttributeDefinition> attributes) {
        this.attributes = attributes;
    }

    public String getIssuerSlug() {
        return issuerSlug;
    }

    public void setIssuerSlug(String issuerSlug) {
        this.issuerSlug = issuerSlug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTxCode() {
        return txCode;
    }

    public void setTxCode(String txCode) {
        this.txCode = txCode;
    }
}
