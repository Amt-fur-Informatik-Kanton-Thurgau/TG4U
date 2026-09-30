package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.entity.model.credential.CredentialData;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/public")
@CrossOrigin(originPatterns = "*")
public class StaticIssuancePublicController {

    private final StaticFlowService staticFlowService;
    private final StaticQrCodeFlowDataService staticQrCodeFlowDataService;

    public StaticIssuancePublicController(
            StaticFlowService staticFlowService,
            StaticQrCodeFlowDataService staticQrCodeFlowDataService) {
        this.staticFlowService = staticFlowService;
        this.staticQrCodeFlowDataService = staticQrCodeFlowDataService;
    }

    @Operation(summary = "Retrieve attributes for a static flow")
    @GetMapping("/v1/static-flow/attribute/{uuid}")
    public ResponseEntity<CredentialData> getAttributes(@PathVariable UUID uuid) {
        return ResponseEntity.ok(staticFlowService.getAttributesByUuid(uuid));
    }

    @Operation(summary = "Resolve a static flow to a fresh issuer credential offer")
    @GetMapping("/v1/static-flow/credential-offer/{uuid}")
    public ResponseEntity<Void> getCredentialOffer(@PathVariable UUID uuid) {
        URI issuerOffer = staticFlowService.getCredentialOfferUri(uuid);
        return ResponseEntity.status(HttpStatus.FOUND)
                .cacheControl(CacheControl.noStore())
                .location(issuerOffer)
                .build();
    }

    @Operation(summary = "Retrieve ActionPayload for a static QR code flow")
    @GetMapping("/v1/static-qr-code-flow/{uuid}")
    public ResponseEntity<StaticQrCodeFlowElementOverview> getStaticQrCodeFlowPayload(
            @PathVariable UUID uuid) {
        var entity = staticQrCodeFlowDataService.findByUuid(uuid);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(StaticQrCodeFlowElementOverview.from(entity));
    }
}
