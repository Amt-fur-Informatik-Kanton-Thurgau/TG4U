{{/*
Ingress + ExternalName Service for the cockpit frontend.

The host is resolved by heidi.host, same as for the backend services; the
frontend normally takes the apex, so it sets no subdomain.

The chart now ships the frontend itself, so the ingress routes to its own
Service by default. ingress.backend still overrides that with a Service this
chart does not create — an app proxy in front of the cockpit, for instance —
in which case no ExternalName alias is written, because that Service has to
already exist in ingress-basic.

ingress-basic is shared with every other deployment in the cluster, so the
Ingress and the alias are named after the deployment rather than only the
project; see heidi.instance.
*/}}
{{- define "heidi.web.ingress" -}}
{{- $ing := .Values.ingress | default dict }}
{{- if $ing.enabled }}
{{- $hosts := $ing.hosts }}
{{- if not $hosts }}
{{- $host := include "heidi.host" (dict "ingress" $ing "baseDomain" .Values.global.baseDomain) }}
{{- $hosts = required (printf "%s: ingress.enabled is true but no host could be resolved — set ingress.subdomain, ingress.hosts, or global.baseDomain" .Values.appName) (ternary (list $host) nil (ne $host "")) }}
{{- end }}
{{- $backend := $ing.backend | default (dict "name" (include "heidi.ingressAlias" .) "port" .Values.port) }}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "heidi.ingressName" . }}
  namespace: ingress-basic
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: {{ .Values.global.clusterIssuer }}
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 256k
    nginx.ingress.kubernetes.io/proxy-buffering: "on"
    nginx.ingress.kubernetes.io/proxy-buffers-number: "4"
    nginx.ingress.kubernetes.io/proxy-max-temp-file-size: 1024m
spec:
  tls:
    - hosts:
      {{- range $hosts }}
        - {{ . }}
      {{- end }}
      secretName: {{ $ing.tlsSecretName | default (printf "tls-secret-%s" (include "heidi.ingressAlias" .)) }}
  rules:
    {{- range $hosts }}
    - host: {{ . }}
      http:
        paths:
          - path: {{ $.Values.global.path | default "/" }}
            pathType: Prefix
            backend:
              service:
                name: {{ $backend.name }}
                port:
                  number: {{ $backend.port }}
    {{- end }}
{{- if not $ing.backend }}

---
apiVersion: v1
kind: Service
metadata:
  name: {{ include "heidi.ingressAlias" . }}
  namespace: ingress-basic
spec:
  type: ExternalName
  externalName: {{ include "heidi.fullname" . }}.{{ .Values.global.namespace }}.svc.cluster.local
  ports:
    - port: {{ .Values.port }}
      targetPort: {{ .Values.port }}
      protocol: TCP
      name: http
{{- end }}
{{- end }}
{{- end -}}
