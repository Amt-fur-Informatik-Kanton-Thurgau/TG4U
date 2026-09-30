package org.heidiverse.heidi.issuer.extensions.staticoffer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaticOfferRepository extends JpaRepository<StaticOfferEntity, UUID> {}
