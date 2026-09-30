package org.heidiverse.heidi.issuer.extensions.authflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tg4u_pushed_authorization_request")
public class PushedAuthorizationRequestEntity {
    @Id @Column(name = "request_uri") private String requestUri;
    @Column(nullable = false, columnDefinition = "text") private String parameters;
    @Column(nullable = false) private Instant expiresAt;

    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String value) { requestUri = value; }
    public String getParameters() { return parameters; }
    public void setParameters(String value) { parameters = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
}
