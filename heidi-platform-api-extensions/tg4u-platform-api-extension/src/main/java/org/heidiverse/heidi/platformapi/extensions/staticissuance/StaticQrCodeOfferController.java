package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.coordinator.model.api.InitializeProcessRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

/** Service-to-service endpoint for creating a TG4U static credential-offer URL. */
@RestController
@RequestMapping("/internal/platform/v1/static-qr-code")
public class StaticQrCodeOfferController {

    private final StaticQrCodeProcessExtension staticQrCodeProcessExtension;

    public StaticQrCodeOfferController(
            StaticQrCodeProcessExtension staticQrCodeProcessExtension) {
        this.staticQrCodeProcessExtension = staticQrCodeProcessExtension;
    }

    @PostMapping("/credential-offer")
    public Tg4uStaticCredentialOfferResponse createCredentialOffer(
            @RequestBody InitializeProcessRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        ZonedDateTime issuedAt = ZonedDateTime.now();
        return staticQrCodeProcessExtension.createStaticCredentialOffer(
                request, authorizationHeader, issuedAt, issuedAt.plusMinutes(5));
    }
}
