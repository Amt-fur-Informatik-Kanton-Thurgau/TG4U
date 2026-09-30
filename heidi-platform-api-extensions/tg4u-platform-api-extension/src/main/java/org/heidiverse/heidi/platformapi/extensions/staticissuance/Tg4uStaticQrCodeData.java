package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.coordinator.model.issuance.IssuanceData;

public record Tg4uStaticQrCodeData(
        IssuanceData.SchemaIdentifier schemaIdentifier,
        String issuerSlug,
        String attributeUrl,
        String issuanceProfileId) {}
