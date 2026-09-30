package org.heidiverse.heidi.issuer.extensions.staticoffer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "static_offer")
public class StaticOfferEntity {
    @Id private UUID id;
    @Column(nullable = false, columnDefinition = "text") private String processToken;
    @Column(nullable = false) private String issuerSlug;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist
    void initialize() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getProcessToken() { return processToken; }
    public void setProcessToken(String processToken) { this.processToken = processToken; }
    public String getIssuerSlug() { return issuerSlug; }
    public void setIssuerSlug(String issuerSlug) { this.issuerSlug = issuerSlug; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
