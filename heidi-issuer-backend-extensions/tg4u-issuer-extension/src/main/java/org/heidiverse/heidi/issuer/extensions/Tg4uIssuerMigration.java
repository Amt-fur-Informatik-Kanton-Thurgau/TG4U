package org.heidiverse.heidi.issuer.extensions;

import org.heidiverse.heidi.issuer.extensions.migration.IssuerExtensionMigration;

/** Registers the TG4U issuer schema in its own Flyway history table. */
public class Tg4uIssuerMigration implements IssuerExtensionMigration {

    @Override
    public String extensionId() {
        return "tg4u-issuer";
    }

    @Override
    public String migrationLocation() {
        return "classpath:/db/issuer-migration/extensions/tg4u-issuer/postgresql";
    }
}
