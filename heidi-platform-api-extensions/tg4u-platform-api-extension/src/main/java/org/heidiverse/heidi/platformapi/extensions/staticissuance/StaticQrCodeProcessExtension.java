package org.heidiverse.heidi.platformapi.extensions.staticissuance;

import org.heidiverse.heidi.coordinator.data.service.ProtocolProcessDataService;
import org.heidiverse.heidi.coordinator.model.ProtocolType;
import org.heidiverse.heidi.coordinator.model.api.InitializeProcessRequest;
import org.heidiverse.heidi.coordinator.model.api.StartProcessResponse;
import org.heidiverse.heidi.coordinator.model.integration.IntegrationScope;
import org.heidiverse.heidi.coordinator.model.issuance.SignatureToken;
import org.heidiverse.heidi.coordinator.model.issuance.SignatureTokenWithTxCode;
import org.heidiverse.heidi.coordinator.model.oid4vci.ActionPayload;
import org.heidiverse.heidi.coordinator.model.oid4vci.SignedData;
import org.heidiverse.heidi.coordinator.service.AuthenticationService;
import org.heidiverse.heidi.coordinator.service.CoordinatorEntityGateway;
import org.heidiverse.heidi.coordinator.service.ProcessActionExtension;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

@Component
public class StaticQrCodeProcessExtension implements ProcessActionExtension {

    public static final String ACTION = "static_qr_code";
    static final String DATA_FIELD = "staticQrCodeData";
    private static final String OPENID_CREDENTIAL_OFFER_SCHEME = "openid-credential-offer://";

    private final AuthenticationService authenticationService;
    private final CoordinatorEntityGateway entityGateway;
    private final TokenSignatureService tokenSignatureService;
    private final StaticIssuerFeignClient issuerFeignClient;
    private final ProtocolProcessDataService processDataService;
    private final ObjectMapper objectMapper;

    public StaticQrCodeProcessExtension(
            AuthenticationService authenticationService,
            CoordinatorEntityGateway entityGateway,
            TokenSignatureService tokenSignatureService,
            StaticIssuerFeignClient issuerFeignClient,
            ProtocolProcessDataService processDataService,
            ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.entityGateway = entityGateway;
        this.tokenSignatureService = tokenSignatureService;
        this.issuerFeignClient = issuerFeignClient;
        this.processDataService = processDataService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String action) {
        return ACTION.equals(action);
    }

    public String resolveProcessTenantId(InitializeProcessRequest request) {
        JsonNode payload = request.extensionData().get(DATA_FIELD);
        if (payload == null) {
            throw new IllegalArgumentException(
                    "`staticQrCodeData` must be set for static QR code action.");
        }
        Tg4uStaticQrCodeData staticQrCodeData =
                objectMapper.convertValue(payload, Tg4uStaticQrCodeData.class);
        return entityGateway
                .getCredentialSchemeTenant(
                        staticQrCodeData.schemaIdentifier().credentialIdentifier())
                .tenantId();
    }

    @Override
    public SignatureTokenWithTxCode initialize(
            InitializeProcessRequest request,
            String authorizationHeader,
            ZonedDateTime issuedAt,
            ZonedDateTime expiresAt) {
        Tg4uStaticQrCodeData staticQrCodeData = staticQrCodeData(request);
        authenticationService.performAuthenticationForInitializeProcess(
                authorizationHeader,
                request,
                staticQrCodeData.schemaIdentifier().credentialIdentifier(),
                IntegrationScope.ISSUE);

        SignedData signablePayload =
                new SignedData(
                        issuedAt,
                        expiresAt,
                        new ActionPayload(request.action(), staticQrCodeData, null));
        return tokenSignatureService.generateToken(
                signablePayload,
                staticQrCodeData.schemaIdentifier().credentialIdentifier(),
                false);
    }

    /**
     * Creates the issuer's static credential-offer URL without creating a coordinator process.
     * Static offers are resolved by the issuer when the wallet scans them, so they do not have a
     * coordinator connection to start or poll here.
     */
    public Tg4uStaticCredentialOfferResponse createStaticCredentialOffer(
            InitializeProcessRequest request,
            String authorizationHeader,
            ZonedDateTime issuedAt,
            ZonedDateTime expiresAt) {
        if (!ACTION.equals(request.action())) {
            throw new IllegalArgumentException("Only the static_qr_code action is supported.");
        }
        resolveProcessTenantId(request);
        Tg4uStaticQrCodeData staticQrCodeData = staticQrCodeData(request);
        SignatureTokenWithTxCode initialized =
                initialize(request, authorizationHeader, issuedAt, expiresAt);
        return createStaticCredentialOffer(initialized.token(), staticQrCodeData);
    }

    public Tg4uStaticCredentialOfferResponse createStaticCredentialOffer(
            String processToken, Tg4uStaticQrCodeData staticQrCodeData) {
        return issuerFeignClient.getStaticCredentialOfferURL(
                staticQrCodeData.issuerSlug(),
                "c",
                new Tg4uStaticCredentialOfferRequest(processToken));
    }

    @Override
    public StartProcessResponse start(
            SignatureToken processToken,
            ActionPayload processTokenPayload) {
        Tg4uStaticQrCodeData staticQrCodeData =
                objectMapper.convertValue(processTokenPayload.getData(), Tg4uStaticQrCodeData.class);
        Tg4uStaticCredentialOfferResponse qrCodeResponse =
                createStaticCredentialOffer(processToken.token(), staticQrCodeData);

        String credentialOfferUri = qrCodeResponse.credentialOfferURl();
        String credentialOfferEncoded =
                "?credential_offer_uri="
                        + URLEncoder.encode(credentialOfferUri, StandardCharsets.UTF_8);
        String connectionId =
                credentialOfferUri.substring(credentialOfferUri.lastIndexOf("=") + 1);
        processDataService.createProcess(
                connectionId,
                objectMapper.valueToTree(staticQrCodeData),
                ProtocolType.OID4VCI.toString());

        return new StartProcessResponse(
                new StartProcessResponse.ProcessData(
                        credentialOfferEncoded, OPENID_CREDENTIAL_OFFER_SCHEME, connectionId),
                null);
    }

    private Tg4uStaticQrCodeData staticQrCodeData(InitializeProcessRequest request) {
        JsonNode payload = request.extensionData().get(DATA_FIELD);
        if (payload == null) {
            throw new IllegalArgumentException(
                    "`staticQrCodeData` must be set for static QR code action.");
        }
        return objectMapper.convertValue(payload, Tg4uStaticQrCodeData.class);
    }
}
