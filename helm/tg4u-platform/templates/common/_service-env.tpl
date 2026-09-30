{{/*
Chart-computed configuration defaults, one define per service.

These are the values the chart can work out on its own — almost all of them
service URLs, which follow from the ingress hosts and the Kubernetes Service
names and would only go stale if written out by hand. Everything else belongs in
values.yaml under <service>.config.

Internal vs public is the distinction to get right: a URL another pod calls is
the cluster-local one, a URL that ends up in a credential offer, a QR code, a
federation entity statement or a browser redirect must be the public one.

Secrets are NOT here — they arrive through the generated dev Secret or the
external Secrets listed in <service>.externalSecrets.
*/}}

{{/* ------------------------------------------------------------------------ */}}
{{/* Every Spring service                                                      */}}
{{/* ------------------------------------------------------------------------ */}}
{{- define "heidi.service.commonConfig" -}}
{{/* `no-security` unless global.springProfiles says otherwise. Not optional for
     the issuer, whose application.properties pins spring.profiles.active=local:
     only this variable keeps that profile's localhost datasource and signing
     defaults out of the cluster. */}}
{{- if hasKey .Values "springProfiles" }}
SPRING_PROFILES_ACTIVE: {{ .Values.springProfiles | quote }}
{{- else }}
SPRING_PROFILES_ACTIVE: {{ .Values.global.springProfiles | quote }}
{{- end }}

{{/* spring-boot-docker-compose is an optional dependency of the platform API
     and verifier, and it is enabled by default when present. In a container
     there is no Compose file beside the app, so startup fails unless the chart
     disables it. The upstream local profile does this; deployments must set it
     here. Harmless for the issuer, which does not ship the dependency. */}}
SPRING_DOCKER_COMPOSE_ENABLED: "false"
{{- end -}}

{{/* ------------------------------------------------------------------------ */}}
{{/* heidi-platform-api — the combined coordinator + entity API                */}}
{{/* ------------------------------------------------------------------------ */}}
{{- define "heidi.platformApi.defaultConfig" -}}
{{- $u := .Values.urls -}}
SERVER_PORT: "8080"

{{/* Platform-to-issuer and platform-to-verifier calls use in-cluster URLs.
     The platform's wallet-facing identity is set separately below. */}}
HEIDI_PLATFORM_ISSUER_INTERNAL_BASE_URL: {{ $u.issuerInternal | quote }}
HEIDI_PLATFORM_VERIFIER_INTERNAL_BASE_URL: {{ $u.verifierInternal | quote }}
HEIDI_PLATFORM_WEB_BASE_URL: {{ $u.webPublic | quote }}
HEIDI_PLATFORM_PUBLIC_BASE_URL: {{ $u.platformApiPublic | quote }}

{{/* The verifier is reached in-cluster, but the URL handed to wallets is the
     public one — the app takes both. */}}
HEIDI_VERIFIER_PUBLIC_BASE_URL: {{ $u.verifierPublic | quote }}
{{/* The issuer twice over, like the verifier above: the API calls it in-cluster,
     but the credential issuer identifier it has to reason about — the one the
     development CA issues a leaf for, and the one that ends up in a credential
     offer — is the address a wallet resolves. */}}
HEIDI_ISSUER_PUBLIC_BASE_URL: {{ $u.issuerPublic | quote }}

{{/* Subject alternative name stamped into the issuance leaf certificates —
     a bare hostname, never host:port. */}}
{{/* Actuator listens on its own port now (management.server.port), so the
     probes and any scrape target this one, not the service port. */}}
HEIDI_PLATFORM_MANAGEMENT_PORT: {{ .Values.managementPort | quote }}

{{- if (.Values.devCa | default dict).enabled }}
{{/* Seeds the local issuer with a development CA at start-up, so issue-then-
     verify closes in an environment with no external CA on a Trusted List. Test
     deployments only: it is a CA private key, and nothing outside this cluster
     trusts it.

     The keystore itself arrives as HEIDI_PLATFORM_DEV_CA_KEYSTORE_BASE64 from the
     generated Secret, alongside its password — the same way every other key
     here travels — which is what makes it survive a pod being replaced. See
     templates/dev-secrets-generator.yaml. */}}
{{- if not .Values.global.devSecrets.generate.enabled }}
{{- fail "platformApi.devCa.enabled requires global.devSecrets.generate.enabled: the CA keystore is generated into that Secret" }}
{{- end }}
HEIDI_PLATFORM_DEV_GENERATE_CA: "true"
{{- end }}

{{- if $u.signingEnabled }}
{{/* The platform stores this provider using its own registered client seed. */}}
HEIDI_PLATFORM_GLOBAL_SIGNING_PROVIDER_ENDPOINT: {{ $u.signingInternal | quote }}
HEIDI_PLATFORM_GLOBAL_SIGNING_PROVIDER_AUTHENTICATION_MODE: "registered"
{{- end }}
{{- end -}}

{{/* ------------------------------------------------------------------------ */}}
{{/* heidi-issuer — OID4VCI issuance                                           */}}
{{/* ------------------------------------------------------------------------ */}}
{{- define "heidi.issuer.defaultConfig" -}}
{{- $u := .Values.urls -}}
SERVER_PORT: "8082"

HEIDI_ISSUER_MANAGEMENT_PORT: {{ .Values.managementPort | quote }}

{{/* Wallet-facing — this is the credential issuer identifier. */}}
HEIDI_ISSUER_PUBLIC_BASE_URL: {{ $u.issuerPublic | quote }}
{{/* Server-to-server, so cluster-local. */}}
HEIDI_ISSUER_PLATFORM_INTERNAL_BASE_URL: {{ $u.platformApiInternal | quote }}
{{- if $u.signingEnabled }}
{{/* The issuer has its own registered client key; the platform accepts it from Cockpit. */}}
HEIDI_ISSUER_SIGNING_PROVIDER_BASE_URL: {{ $u.signingInternal | quote }}
HEIDI_ISSUER_SIGNING_PROVIDER_AUTHENTICATION_MODE: "registered"
{{- end }}


{{- end -}}

{{/* ------------------------------------------------------------------------ */}}
{{/* heidi-verifier — OID4VP presentation                                      */}}
{{/* ------------------------------------------------------------------------ */}}
{{- define "heidi.verifier.defaultConfig" -}}
{{- $u := .Values.urls -}}
SERVER_PORT: "8083"

HEIDI_VERIFIER_MANAGEMENT_PORT: {{ .Values.managementPort | quote }}

{{/* Wallets post their responses here, so it has to be resolvable from a phone. */}}
HEIDI_VERIFIER_PUBLIC_BASE_URL: {{ $u.verifierPublic | quote }}
{{/* The platform API twice over: the verifier calls it in-cluster, but the two
     identifiers it hands wallets — the federation authority hint and the vct —
     have to be resolvable from a phone. */}}
HEIDI_VERIFIER_PLATFORM_INTERNAL_BASE_URL: {{ $u.platformApiInternal | quote }}
HEIDI_VERIFIER_WEB_BASE_URL: {{ $u.webPublic | quote }}

{{/* Digital Credentials API origins are hosts, not URLs — the browser compares
     them against the calling origin. Cockpit and API both invoke the flow, and
     under path routing they are the same origin, hence the one entry. */}}
HEIDI_VERIFIER_OID4VP_DC_API_EXPECTED_ORIGINS: {{ (compact (uniq (list $u.webHost $u.platformApiHost))) | join "," | quote }}
{{- if $u.signingEnabled }}
{{/* The verifier has its own registered client key; the platform accepts it from Cockpit. */}}
HEIDI_VERIFIER_SIGNING_PROVIDER_BASE_URL: {{ $u.signingInternal | quote }}
HEIDI_VERIFIER_SIGNING_PROVIDER_AUTHENTICATION_MODE: "registered"
{{- end }}

{{- end -}}

{{/* ------------------------------------------------------------------------ */}}
{{/* heidi-signing — the software signing provider                             */}}
{{/* ------------------------------------------------------------------------ */}}
{{- define "heidi.signing.defaultConfig" -}}
HEIDI_SIGNING_PORT: "8086"

{{- if (.Values.persistence | default dict).enabled }}
{{/* Keys in Postgres instead of in memory. The service excludes Spring's
     datasource auto-configuration and builds its own only in this mode, and its
     Flyway is switched on by this same variable, so nothing migrates and no
     table exists until it is set. The datasource itself arrives as
     SPRING_DATASOURCE_* from the service's own Secret, and the master key that
     encrypts the stored keys is generated with the other dev secrets. */}}
{{- if not (include "heidi.usesDatabase" .) }}
{{- fail "signing.persistence.enabled needs a database: set signing.database, and one of global.devDatabase or global.externalDatabase" }}
{{- end }}
HEIDI_SIGNING_SOFTWARE_DATABASE_ENABLED: "true"
{{- end }}
{{/* No management.server.port in this service, so actuator shares the main
     port — which is why it declares no managementPort in values.yaml. */}}

{{/* The software provider advertises the `software` scheme. The platform key is
     pre-accepted; its authenticated Cockpit action can then add issuer/verifier. */}}
HEIDI_SIGNING_AUTH_MODE: "registered"
HEIDI_SIGNING_AUTH_CLIENT_ACCEPTANCE: "platform-assisted"
HEIDI_SIGNING_SCHEME: "software"
{{- end -}}
