package org.heidiverse.heidi.issuer.extensions.authflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AuthorizationSessionRepository extends JpaRepository<AuthorizationSessionEntity, UUID> {
    Optional<AuthorizationSessionEntity> findByIssuerState(String issuerState);
    Optional<AuthorizationSessionEntity> findByExternalState(String externalState);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthorizationSessionEntity> findByAuthorizationCode(String authorizationCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthorizationSessionEntity> findByConnectionId(String connectionId);
}
