package org.heidiverse.heidi.platformapi.extensions.authflow;

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
import org.heidiverse.heidi.coordinator.service.ProcessActionExtension;
import org.heidiverse.heidi.coordinator.service.TokenSignatureService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/** Restores the former auth-issuance process action in the TG4U build. */
@Component
public class Tg4uAuthorizationProcessExtension implements ProcessActionExtension {
    public static final String ACTION = "auth_issuance_azure_oidc";
    private static final String DATA_FIELD = "authIssuanceData";
    private static final String OPENID_CREDENTIAL_OFFER_SCHEME = "openid-credential-offer://";

    private final AuthenticationService authenticationService;
    private final TokenSignatureService tokenSignatureService;
    private final Tg4uIssuerAuthFeignClient issuer;
    private final ProtocolProcessDataService processDataService;
    private final ObjectMapper objectMapper;

    public Tg4uAuthorizationProcessExtension(
            AuthenticationService authenticationService,
            TokenSignatureService tokenSignatureService,
            Tg4uIssuerAuthFeignClient issuer,
            ProtocolProcessDataService processDataService,
            ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.tokenSignatureService = tokenSignatureService;
        this.issuer = issuer;
        this.processDataService = processDataService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String action) {
        return ACTION.equals(action);
    }

    @Override
    public SignatureTokenWithTxCode initialize(
            InitializeProcessRequest request,
            String authorizationHeader,
            ZonedDateTime issuedAt,
            ZonedDateTime expiresAt) {
        Tg4uAuthIssuanceData data = data(request);
        AuthFlowEntry flow = issuer.get(UUID.fromString(data.authFlowId()));
        authenticationService.performAuthenticationForInitializeProcess(
                authorizationHeader,
                request,
                required(flow.credentialIdentifier(), "credentialIdentifier"),
                IntegrationScope.ISSUE);
        SignedData signablePayload = new SignedData(
                issuedAt,
                expiresAt,
                new ActionPayload(request.action(), data, null));
        return tokenSignatureService.generateToken(
                signablePayload, flow.credentialIdentifier(), false);
    }

    @Override
    public StartProcessResponse start(
            SignatureToken processToken,
            ActionPayload processTokenPayload) {
        Tg4uAuthIssuanceData data = objectMapper.convertValue(
                processTokenPayload.getData(), Tg4uAuthIssuanceData.class);
        Map<String, Object> offer = issuer.credentialOffer(
                data.issuerSlug(), "c", UUID.fromString(data.authFlowId()));
        String connectionId = required(offer.get("connection_id"), "connection_id");
        String offerJson;
        try {
            offerJson = objectMapper.writeValueAsString(offer);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode TG4U authorization credential offer", exception);
        }
        processDataService.createProcess(
                connectionId,
                objectMapper.valueToTree(data),
                ProtocolType.OID4VCI.toString());
        String encodedOffer = "?credential_offer="
                + URLEncoder.encode(offerJson, StandardCharsets.UTF_8);
        return new StartProcessResponse(
                new StartProcessResponse.ProcessData(
                        encodedOffer, OPENID_CREDENTIAL_OFFER_SCHEME, "VCI:" + connectionId),
                null);
    }

    private Tg4uAuthIssuanceData data(InitializeProcessRequest request) {
        JsonNode payload = request.extensionData().get(DATA_FIELD);
        if (payload == null) {
            throw new IllegalArgumentException("`" + DATA_FIELD + "` must be set for " + ACTION + " action.");
        }
        Tg4uAuthIssuanceData data = objectMapper.convertValue(payload, Tg4uAuthIssuanceData.class);
        required(data.issuerSlug(), "issuerSlug");
        try {
            UUID.fromString(required(data.authFlowId(), "authFlowId"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("authFlowId must be a UUID", exception);
        }
        return data;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String required(Object value, String name) {
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.toString();
    }

    private record Tg4uAuthIssuanceData(String issuerSlug, String authFlowId) {}
}
