package org.heidiverse.heidi.issuer.extensions.authflow;

import org.heidiverse.heidi.issuer.model.FlowVariant;
import org.heidiverse.heidi.issuer.model.api.AuthorizationServerMetadata;
import org.heidiverse.heidi.issuer.model.api.CredentialOfferResponse;
import org.heidiverse.heidi.issuer.model.api.IssuerMetadata;
import org.heidiverse.heidi.issuer.model.api.PushedAuthorizationResponse;
import org.heidiverse.heidi.issuer.model.api.TokenResponse;
import org.heidiverse.heidi.issuer.service.IssuanceService;
import org.heidiverse.heidi.issuer.service.IssuerAuthorizationExtension;
import org.heidiverse.heidi.issuer.service.IssuerProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** TG4U authorization-code bridge. Its final wallet session is still an OSS pre-auth session. */
@Service
public class Tg4uAuthorizationService implements IssuerAuthorizationExtension {
    private static final int PAR_LIFETIME_SECONDS = 300;
    private static final int AUTHORIZATION_LIFETIME_SECONDS = 600;
    private static final String AUTHORIZATION_CODE_GRANT = "authorization_code";
    private static final String PRE_AUTHORIZED_GRANT = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

    private final AuthFlowRepository authFlows;
    private final AuthFlowService authFlowService;
    private final AuthorizationSessionRepository sessions;
    private final PushedAuthorizationRequestRepository pushedRequests;
    private final IssuanceService issuanceService;
    private final IssuerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Tg4uAuthorizationService(
            AuthFlowRepository authFlows,
            AuthFlowService authFlowService,
            AuthorizationSessionRepository sessions,
            PushedAuthorizationRequestRepository pushedRequests,
            IssuanceService issuanceService,
            IssuerProperties properties,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.authFlows = authFlows;
        this.authFlowService = authFlowService;
        this.sessions = sessions;
        this.pushedRequests = pushedRequests;
        this.issuanceService = issuanceService;
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String issuerSlug, FlowVariant variant, String credentialIdentifier, String credentialVersion) {
        return authFlows.existsByCredentialIdentifierAndCredentialVersion(credentialIdentifier, credentialVersion);
    }

    @Override
    public AuthorizationServerMetadata authorizationMetadata(
            String issuerSlug, FlowVariant variant, String credentialIdentifier, String credentialVersion,
            AuthorizationServerMetadata base) {
        String issuer = base.issuer();
        List<String> grants = new ArrayList<>(base.grantTypesSupported());
        if (!grants.contains(AUTHORIZATION_CODE_GRANT)) grants.add(AUTHORIZATION_CODE_GRANT);
        return new AuthorizationServerMetadata(
                issuer, issuer + "/authorize", issuer + "/par", base.tokenEndpoint(), List.of("code"),
                grants, List.of("S256"), base.tokenEndpointAuthMethodsSupported(),
                base.authorizationDetailsTypesSupported(), base.dpopSigningAlgValuesSupported());
    }

    @Override
    @Transactional
    public PushedAuthorizationResponse pushAuthorization(MultiValueMap<String, String> params) {
        String responseType = params.getFirst("response_type");
        if (responseType != null && !"code".equals(responseType)) throw new IllegalArgumentException("Only response_type=code is supported");
        PushedAuthorizationRequestEntity request = new PushedAuthorizationRequestEntity();
        String requestUri = "urn:ietf:params:oauth:request_uri:" + randomToken();
        request.setRequestUri(requestUri);
        try {
            request.setParameters(objectMapper.writeValueAsString(params.toSingleValueMap()));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not persist pushed authorization request", exception);
        }
        request.setExpiresAt(Instant.now().plusSeconds(PAR_LIFETIME_SECONDS));
        pushedRequests.save(request);
        return new PushedAuthorizationResponse(requestUri, PAR_LIFETIME_SECONDS);
    }

    @Override
    @Transactional
    public String authorize(Map<String, String> params) {
        Map<String, String> merged = new HashMap<>();
        String requestUri = params.get("request_uri");
        if (requestUri != null) {
            PushedAuthorizationRequestEntity pushed = pushedRequests.findByRequestUri(requestUri)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown or expired request_uri"));
            if (!pushed.getExpiresAt().isAfter(Instant.now())) {
                pushedRequests.delete(pushed);
                throw new IllegalArgumentException("Authorization request has expired");
            }
            try {
                merged.putAll(objectMapper.readValue(pushed.getParameters(), new TypeReference<Map<String, String>>() {}));
            } catch (JacksonException exception) {
                throw new IllegalArgumentException("Invalid pushed authorization request", exception);
            }
            pushedRequests.delete(pushed);
        }
        merged.putAll(params);
        if (!"code".equals(merged.getOrDefault("response_type", "code"))) throw new IllegalArgumentException("Only response_type=code is supported");
        AuthorizationSessionEntity session = sessions.findByIssuerState(required(merged, "issuer_state"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown issuer_state"));
        ensureActive(session);
        session.setRedirectUri(required(merged, "redirect_uri"));
        session.setClientState(merged.get("state"));
        session.setCodeChallenge(required(merged, "code_challenge"));
        if (!"S256".equals(merged.get("code_challenge_method"))) throw new IllegalArgumentException("Only the S256 code_challenge_method is supported");
        String requestedJkt = merged.get("dpop_jkt");
        if (requestedJkt != null && !requestedJkt.matches("^[A-Za-z0-9_-]{43}$")) throw new IllegalArgumentException("dpop_jkt must be a base64url SHA-256 JWK thumbprint");
        session.setRequestedDpopJkt(requestedJkt);
        session.setStatus(AuthorizationSessionStatus.AUTHORIZATION_REQUESTED);
        sessions.save(session);

        AuthFlowEntity flow = authFlowService.get(session.getAuthFlowId());
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(flow.getAuthorizeEndpoint())
                .queryParam("client_id", flow.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", callbackUri())
                .queryParam("state", session.getExternalState());
        if (flow.getScope() != null && !flow.getScope().isBlank()) builder.queryParam("scope", flow.getScope());
        return builder.build().encode().toUriString();
    }

    @Override
    @Transactional
    public TokenResponse token(Map<String, String> params, String dpopProof, String httpMethod) {
        if (!AUTHORIZATION_CODE_GRANT.equals(params.get("grant_type"))) throw new IllegalArgumentException("Unsupported grant_type");
        AuthorizationSessionEntity session = sessions.findByAuthorizationCode(required(params, "code"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid authorization code"));
        ensureActive(session);
        if (session.getStatus() != AuthorizationSessionStatus.AUTHORIZED) throw new IllegalArgumentException("Authorization code is not available");
        if (params.containsKey("redirect_uri") && !session.getRedirectUri().equals(params.get("redirect_uri"))) throw new IllegalArgumentException("redirect_uri does not match the authorization request");
        String verifier = required(params, "code_verifier");
        String expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        if (!MessageDigest.isEqual(expectedChallenge.getBytes(StandardCharsets.US_ASCII), session.getCodeChallenge().getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException("Invalid code_verifier");
        String preAuthorizedCode = session.getPreAuthorizedCode();
        if (preAuthorizedCode == null) throw new IllegalArgumentException("Authorization has not completed");
        TokenResponse response = issuanceService.token(
                Map.of("grant_type", PRE_AUTHORIZED_GRANT, "pre-authorized_code", preAuthorizedCode),
                dpopProof, httpMethod, session.getRequestedDpopJkt());
        session.setAuthorizationCode(null);
        session.setStatus(AuthorizationSessionStatus.REDEEMED);
        sessions.save(session);
        return response;
    }

    @Transactional
    public CredentialOfferResponse createOffer(String issuerSlug, FlowVariant variant, UUID authFlowId) {
        AuthFlowEntity flow = authFlowService.get(authFlowId);
        IssuerMetadata metadata = issuanceService.credentialMetadata(
                issuerSlug,
                variant,
                flow.getCredentialIdentifier(),
                flow.getCredentialVersion(),
                issuanceService.issuanceProfileFor(
                        issuerSlug, flow.getCredentialIdentifier(), flow.getCredentialVersion()));
        AuthorizationSessionEntity session = new AuthorizationSessionEntity();
        session.setAuthFlowId(authFlowId);
        session.setIssuerSlug(issuerSlug);
        session.setVariant(variant);
        session.setCredentialIdentifier(flow.getCredentialIdentifier());
        session.setCredentialVersion(flow.getCredentialVersion());
        session.setIssuerState(randomToken());
        session.setExternalState(randomToken());
        session.setConnectionId(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plusSeconds(AUTHORIZATION_LIFETIME_SECONDS));
        session.setStatus(AuthorizationSessionStatus.OFFER_CREATED);
        sessions.save(session);
        return new CredentialOfferResponse(
                issuerUrl(issuerSlug, variant, flow.getCredentialIdentifier(), flow.getCredentialVersion()),
                new ArrayList<>(metadata.credentialConfigurationsSupported().keySet()),
                new CredentialOfferResponse.Grants(null, new CredentialOfferResponse.AuthorizationCodeGrant(session.getIssuerState())),
                session.getConnectionId());
    }

    @Transactional(readOnly = true)
    public AuthorizationCallbackContext context(String externalState) {
        AuthorizationSessionEntity session = sessions.findByExternalState(externalState)
                .orElseThrow(() -> new IllegalArgumentException("Unknown callback state"));
        ensureActive(session);
        AuthFlowEntity flow = authFlowService.get(session.getAuthFlowId());
        return new AuthorizationCallbackContext(
                session.getAuthFlowId(), session.getConnectionId(), session.getIssuerSlug(),
                session.getCredentialIdentifier(), session.getCredentialVersion(), flow.getClientId(), callbackUri());
    }

    @Transactional
    public Map<String, Object> exchange(AuthorizationCodeExchangeRequest request) {
        AuthorizationSessionEntity session = sessions.findByExternalState(request.state())
                .orElseThrow(() -> new IllegalArgumentException("Unknown callback state"));
        ensureActive(session);
        AuthFlowEntity flow = authFlowService.get(session.getAuthFlowId());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", request.code());
        form.add("redirect_uri", callbackUri());
        form.add("client_id", flow.getClientId());
        form.add("client_secret", authFlowService.clientSecret(flow));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(flow.getTokenEndpoint(), new HttpEntity<>(form, headers), Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new IllegalArgumentException("External authorization code exchange failed");
        Object accessToken = response.getBody().get("access_token");
        if (!(accessToken instanceof String token)) throw new IllegalArgumentException("External token response has no access_token");
        return decodeAndVerifyClaims(token, flow);
    }

    @Transactional
    public OidcAuthSuccessResponse complete(String connectionId, String processToken) {
        AuthorizationSessionEntity session = sessions.findByConnectionId(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown authorization session"));
        ensureActive(session);
        if (session.getStatus() != AuthorizationSessionStatus.AUTHORIZATION_REQUESTED) throw new IllegalArgumentException("Authorization session is not awaiting callback");
        CredentialOfferResponse offer = issuanceService.createOffer(
                session.getIssuerSlug(), session.getVariant(),
                new org.heidiverse.heidi.issuer.model.api.CredentialOfferRequest(processToken, null));
        if (offer.grants() == null || offer.grants().preAuthorizedCode() == null) throw new IllegalArgumentException("Issuer did not create a pre-authorized session");
        session.setPreAuthorizedCode(offer.grants().preAuthorizedCode().preAuthorizedCode());
        session.setIssuerConnectionId(offer.connectionId());
        session.setAuthorizationCode(randomToken());
        session.setStatus(AuthorizationSessionStatus.AUTHORIZED);
        sessions.save(session);
        String separator = session.getRedirectUri().contains("?") ? "&" : "?";
        String state = session.getClientState() == null ? "" : "&state=" + urlEncode(session.getClientState());
        return new OidcAuthSuccessResponse(session.getRedirectUri() + separator + "code=" + urlEncode(session.getAuthorizationCode()) + state);
    }

    private Map<String, Object> decodeAndVerifyClaims(String token, AuthFlowEntity flow) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            String algorithm = jwt.getHeader().getAlgorithm().getName();
            String jwks = restTemplate.getForObject(flow.getJwksEndpoint(), String.class);
            JWKSet keySet = JWKSet.parse(jwks);
            JWK key = jwt.getHeader().getKeyID() == null
                    ? null
                    : keySet.getKeyByKeyId(jwt.getHeader().getKeyID());
            if (key == null) {
                key = keySet.getKeys().stream()
                        .filter(candidate -> candidate.getAlgorithm() == null
                                || algorithm.equals(candidate.getAlgorithm().getName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Provider JWK for external token was not found"));
            }
            if (!jwt.verify(verifier(key))) throw new IllegalArgumentException("External access-token signature is invalid");
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) {
                throw new IllegalArgumentException("External access token has expired");
            }
            if (flow.getProviderIssuer() != null && !flow.getProviderIssuer().equals(claims.getIssuer())) {
                throw new IllegalArgumentException("External access-token issuer is invalid");
            }
            if (flow.getAudience() != null && !flow.getAudience().isBlank()
                    && (claims.getAudience() == null || !claims.getAudience().contains(flow.getAudience()))) {
                throw new IllegalArgumentException("External access-token audience is invalid");
            }
            return claims.getClaims();
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException illegal) throw illegal;
            throw new IllegalArgumentException("Could not verify external access token", exception);
        }
    }

    private static JWSVerifier verifier(JWK key) throws Exception {
        if (key instanceof RSAKey rsa) return new RSASSAVerifier(rsa.toRSAPublicKey());
        if (key instanceof ECKey ec) return new ECDSAVerifier(ec.toECPublicKey());
        throw new IllegalArgumentException("Unsupported provider JWK type");
    }

    private void ensureActive(AuthorizationSessionEntity session) {
        if (!session.getExpiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Authorization session has expired");
    }

    private String callbackUri() { return properties.getPlatformPublicBaseUrl().replaceAll("/$", "") + "/v1/oidc/callback"; }
    private String issuerUrl(String slug, FlowVariant variant, String identifier, String version) { return properties.getPublicUrl().replaceAll("/$", "") + "/" + slug + "/" + variant.path() + "/" + identifier + "/" + version; }
    private static String required(Map<String, String> values, String key) { String value = values.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required"); return value; }
    private static byte[] sha256(byte[] value) { try { return MessageDigest.getInstance("SHA-256").digest(value); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private static String randomToken() { return UUID.randomUUID().toString().replace("-", ""); }
    private static String urlEncode(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
