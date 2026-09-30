package org.heidiverse.heidi.issuer.extensions.authflow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushedAuthorizationRequestRepository
        extends JpaRepository<PushedAuthorizationRequestEntity, String> {
    java.util.Optional<PushedAuthorizationRequestEntity> findByRequestUri(String requestUri);
}
