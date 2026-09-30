package org.heidiverse.heidi.issuer.extensions.authflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthFlowRepository extends JpaRepository<AuthFlowEntity, UUID> {
    List<AuthFlowEntity> findByDcqlQueryIsNotNull();
    boolean existsByCredentialIdentifierAndCredentialVersion(
            String credentialIdentifier, String credentialVersion);
}
