package org.heidiverse.heidi.issuer.extensions;

import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticCredentialOfferRequest;
import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticOfferController;
import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticOfferEntity;
import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticOfferRepository;
import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticOfferService;
import org.heidiverse.heidi.issuer.extensions.staticoffer.StaticOfferTokenCrypto;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthFlowController;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthFlowEntity;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthFlowRepository;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthFlowService;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthorizationSessionEntity;
import org.heidiverse.heidi.issuer.extensions.authflow.AuthorizationSessionRepository;
import org.heidiverse.heidi.issuer.extensions.authflow.Tg4uAuthSecretCrypto;
import org.heidiverse.heidi.issuer.extensions.authflow.Tg4uAuthorizationController;
import org.heidiverse.heidi.issuer.extensions.authflow.Tg4uAuthorizationService;
import org.heidiverse.heidi.issuer.extensions.authflow.PushedAuthorizationRequestEntity;
import org.heidiverse.heidi.issuer.extensions.authflow.PushedAuthorizationRequestRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Registers the TG4U issuer features when this JAR is on the application classpath. */
@AutoConfiguration
@EntityScan(basePackageClasses = {
        StaticOfferEntity.class,
        AuthFlowEntity.class,
        AuthorizationSessionEntity.class,
        PushedAuthorizationRequestEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        StaticOfferRepository.class,
        AuthFlowRepository.class,
        AuthorizationSessionRepository.class,
        PushedAuthorizationRequestRepository.class
})
@Import({
        Tg4uIssuerMigration.class,
        AuthFlowController.class,
        AuthFlowService.class,
        Tg4uAuthSecretCrypto.class,
        Tg4uAuthorizationController.class,
        Tg4uAuthorizationService.class,
        StaticOfferController.class,
        StaticOfferService.class,
        StaticOfferTokenCrypto.class
})
public class Tg4uIssuerAutoConfiguration {
}
