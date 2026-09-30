{{/*
One host for the whole stack, services told apart by path prefix.

  https://example.com/                         web (the cockpit)
  https://example.com/api/...                  platform-api
  https://example.com/issuer/...               issuer
  https://example.com/verifier/...             verifier
  https://example.com/.well-known/...          issuer (see below)

Each rule strips its own prefix, so the services keep serving at the root and
need no context-path of their own. What makes that safe is that none of them
derives a URL from the incoming request — every public URL is built from the
configured base URL, which global.pathRouting gives the matching prefix.

Only what a wallet or a browser has to reach is routed. Everything else keeps
to the cluster: the coordinator calls the issuer and the verifier through their
Services, and those paths are deliberately absent from the table below.

Rendered only when global.pathRouting.enabled; otherwise each service keeps its
own host and its own ingress (templates/<service>/ingress.yaml).
*/}}
{{- define "heidi.sharedIngress" -}}
{{- $g := .Values.global -}}
{{- $routing := $g.pathRouting -}}
{{- $u := include "heidi.urls" . | fromYaml -}}
{{- $host := $routing.host | default $g.baseDomain -}}
{{- $host = required "global.pathRouting.enabled is true but no host could be resolved — set global.pathRouting.host or global.baseDomain" $host -}}
{{- $prefixes := $routing.prefixes | default dict -}}
{{- /* Ingress objects, their TLS Secret and their ExternalName aliases can end
       up in a namespace shared with every other deployment in the cluster, so
       their names carry which deployment they belong to rather than only the
       project — see heidi.instance. */}}
{{- $instance := include "heidi.instance" . -}}
{{- $tlsSecret := $routing.tlsSecretName | default (printf "tls-secret-%s-%s" $instance $g.environment) -}}
{{- /* Where the Ingress objects go. A cluster with a shared ingress controller
       keeps them next to it, which is why the workload is reached through an
       ExternalName alias; minikube's addon watches every namespace, so there
       the Ingress sits with the pods and needs no alias. */}}
{{- $ingressNs := $routing.ingressNamespace | default $g.namespace -}}
{{- $aliases := ne $ingressNs $g.namespace -}}
{{- /* What a rule points at: the alias when the Ingress sits in another
       namespace, the workload Service itself when it sits with the pods. */}}
{{- $backend := dict -}}
{{- range $k := list "web" "platformApi" "issuer" "verifier" -}}
{{- $_ := set $backend $k (ternary (index $u (printf "%sAlias" $k)) (index $u (printf "%sName" $k)) $aliases) -}}
{{- end -}}
{{- $tls := true -}}
{{- if hasKey $routing "tls" -}}{{- $tls = $routing.tls -}}{{- end -}}
{{- $class := $routing.ingressClassName | default "" -}}

{{/* The cockpit takes the root, so its rule is a plain prefix and nothing is
     rewritten — the SPA fallback needs the path it was asked for. This is also
     the one rule carrying TLS: the host has a single certificate, and a second
     Ingress claiming the same Secret would give cert-manager two Certificates
     to fight over. NGINX serves every rule below under this certificate. */}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $instance }}-web-{{ $g.environment }}-ingress-rule
  namespace: {{ $ingressNs }}
  annotations:
    kubernetes.io/ingress.class: nginx
{{- if $tls }}
    cert-manager.io/cluster-issuer: {{ $g.clusterIssuer }}
{{- end }}
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 256k
    nginx.ingress.kubernetes.io/proxy-buffering: "on"
    nginx.ingress.kubernetes.io/proxy-buffers-number: "4"
    nginx.ingress.kubernetes.io/proxy-max-temp-file-size: 1024m
spec:
{{- if $class }}
  ingressClassName: {{ $class }}
{{- end }}
{{- if $tls }}
  tls:
    - hosts:
        - {{ $host }}
      secretName: {{ $tlsSecret }}
{{- end }}
  rules:
    - host: {{ $host }}
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: {{ $backend.web }}
                port:
                  number: 80
{{ include "heidi.sharedIngress.alias" (dict "name" $u.webAlias "target" $u.webName "namespace" $g.namespace "port" 80 "ingressNamespace" $ingressNs "render" $aliases) }}

{{- if and .Values.platformApi .Values.platformApi.enabled }}
{{/* The whole API. The cockpit is now same-origin with it, and the endpoints
     that are public without a token are the ones PlatformApiSecurityConfig
     lists — that decision stays in the application, not here. */}}
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $instance }}-platform-api-{{ $g.environment }}-ingress-rule
  namespace: {{ $ingressNs }}
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 128k
spec:
{{- if $class }}
  ingressClassName: {{ $class }}
{{- end }}
  rules:
    - host: {{ $host }}
      http:
        paths:
          - path: '{{ index $prefixes "platformApi" }}(/|$)(.*)'
            pathType: ImplementationSpecific
            backend:
              service:
                name: {{ $backend.platformApi }}
                port:
                  number: {{ $u.platformApiPort }}
{{ include "heidi.sharedIngress.alias" (dict "name" $u.platformApiAlias "target" $u.platformApiName "namespace" $g.namespace "port" $u.platformApiPort "ingressNamespace" $ingressNs "render" $aliases) }}
{{- end }}

{{- if and .Values.issuer .Values.issuer.enabled }}
{{/* The whole issuer except what the coordinator alone calls. Listing the
     endpoints to expose would mean editing this file for every new route, and
     the failure mode is a wallet getting the cockpit's index.html back, so the
     rule is the other way round: everything passes but the two named below.

       /<slug>/<variant>/credential-offer     mints a credential offer
       /<variant>/<connectionId>/connectionStatus

     Both are the coordinator's, reached in-cluster, and the issuer runs no
     authentication of its own — an open offer endpoint is the one thing here
     that must not be on the internet. Matching on the last segment rather than
     the whole shape keeps the rule readable and independent of how many
     identifier segments precede it.

     So: a new wallet-facing route needs nothing here. A new coordinator-only
     one has to be added, or it is public. Moving those under one stable prefix
     in the issuer — /internal/... — would end that too. */}}
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $instance }}-issuer-{{ $g.environment }}-ingress-rule
  namespace: {{ $ingressNs }}
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 128k
spec:
{{- if $class }}
  ingressClassName: {{ $class }}
{{- end }}
  rules:
    - host: {{ $host }}
      http:
        paths:
          - path: '{{ index $prefixes "issuer" }}(/|$)((?!.*/credential-offer$)(?!.*/connectionStatus$).*)'
            pathType: ImplementationSpecific
            backend:
              service:
                name: {{ $backend.issuer }}
                port:
                  number: {{ $u.issuerPort }}
{{/* OID4VCI puts the well-known between host and path, so a credential issuer
     at https://host/issuer/acme/c/id/1 publishes its metadata at
     https://host/.well-known/openid-credential-issuer/issuer/acme/c/id/1 — at
     the root of the host, with the prefix as the first path segment. Dropping
     that segment leaves exactly what the issuer serves.

     Any well-known kind, not a list of them: the segment after it is the
     issuer's own prefix, so nothing but the issuer can own these URLs anyway,
     and a kind the issuer learns to serve later needs no edit here. Today that
     is openid-credential-issuer (with and without a trust framework), the
     authorization server metadata under both its RFC 8414 name and the OpenID
     Connect one, and jwt-vc-issuer from SD-JWT VC. The issuer serves the AS
     metadata with the well-known appended too, which the rule above covers —
     wallets differ on which they ask for, so both have to work.

     Its own rule because it rewrites to a different target, and only needed
     while the issuer has a prefix to strip — at the root it already serves
     these paths itself. */}}
{{- if ne (index $prefixes "issuer") "" }}
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $instance }}-issuer-wellknown-{{ $g.environment }}-ingress-rule
  namespace: {{ $ingressNs }}
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /.well-known/$1/$2
spec:
{{- if $class }}
  ingressClassName: {{ $class }}
{{- end }}
  rules:
    - host: {{ $host }}
      http:
        paths:
          - path: '/\.well-known/([^/]+){{ index $prefixes "issuer" }}/(.*)'
            pathType: ImplementationSpecific
            backend:
              service:
                name: {{ $backend.issuer }}
                port:
                  number: {{ $u.issuerPort }}
{{- end }}
{{ include "heidi.sharedIngress.alias" (dict "name" $u.issuerAlias "target" $u.issuerName "namespace" $g.namespace "port" $u.issuerPort "ingressNamespace" $ingressNs "render" $aliases) }}
{{- end }}

{{- if and .Values.verifier .Values.verifier.enabled }}
{{/* The whole verifier except /v1/verifier, which is the coordinator's own API
     — par, authorization, state, internal/vp-token, all reached in-cluster
     through VerifierFeignClient. It is excluded rather than merely unused:
     /v1/verifier/state and /v1/verifier/internal/vp-token return a
     presentation's contents to anyone holding a transaction id.

     What that leaves public is /v1/wallet (the request_uri the coordinator
     hands out and the response_uri the wallet posts to), the federation entity
     configuration and its fetch endpoint, and the health endpoint the service
     deliberately calls public. Wallet-facing routes added later are covered on
     their own; another coordinator-only namespace would have to be named here,
     the way /v1/verifier is. */}}
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ $instance }}-verifier-{{ $g.environment }}-ingress-rule
  namespace: {{ $ingressNs }}
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/use-regex: "true"
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 128k
spec:
{{- if $class }}
  ingressClassName: {{ $class }}
{{- end }}
  rules:
    - host: {{ $host }}
      http:
        paths:
          - path: '{{ index $prefixes "verifier" }}(/|$)((?!v1/verifier(?:/|$)).*)'
            pathType: ImplementationSpecific
            backend:
              service:
                name: {{ $backend.verifier }}
                port:
                  number: {{ $u.verifierPort }}
{{ include "heidi.sharedIngress.alias" (dict "name" $u.verifierAlias "target" $u.verifierName "namespace" $g.namespace "port" $u.verifierPort "ingressNamespace" $ingressNs "render" $aliases) }}
{{- end }}
{{- end -}}

{{/*
The ExternalName alias an Ingress needs to reach a workload in another
namespace: the Ingress lives in the cluster's shared ingress-basic namespace,
the pods do not.

Nothing is rendered when the Ingress already lives in the workload namespace,
where it reaches the Service directly.

Argument: dict "name" <alias> "target" <workload service> "namespace" <workload ns>
         "port" <port> "ingressNamespace" <ingress ns> "render" <bool>
*/}}
{{- define "heidi.sharedIngress.alias" -}}
{{- if .render }}
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
  namespace: {{ .ingressNamespace }}
spec:
  type: ExternalName
  externalName: {{ .target }}.{{ .namespace }}.svc.cluster.local
  ports:
    - port: {{ .port }}
      targetPort: {{ .port }}
      protocol: TCP
      name: http
{{- end }}
{{- end -}}
