package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.platformapi.extensions.migration.PlatformApiExtensionMigration;

import org.springframework.stereotype.Component;

@Component
public class StaticIssuanceMigration implements PlatformApiExtensionMigration {

    @Override
    public String extensionId() {
        return "static-issuance";
    }

    @Override
    public String migrationLocation() {
        return "classpath:/db/migration/extensions/static-issuance/postgresql";
    }
}
