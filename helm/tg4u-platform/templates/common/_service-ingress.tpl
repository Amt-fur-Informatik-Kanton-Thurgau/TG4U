{{/*
Ingress + ExternalName Service for a service.

The host is resolved by heidi.host (ingress.hosts, then
<subdomain>.<baseDomain>, then <baseDomain>); ingress.hosts may list more than
one, and all of them are routed. Nothing renders unless ingress.enabled is true.

The Ingress and its ExternalName Service live in the cluster's shared
ingress-basic namespace, which is why the Service is duplicated there — the
workload itself stays in global.namespace. That namespace is shared with every
other deployment in the cluster, so both are named after the deployment rather
than only the project; see heidi.instance.
*/}}
{{- define "heidi.service.ingress" -}}
{{- $ing := .Values.ingress | default dict }}
{{- if $ing.enabled }}
{{- $hosts := $ing.hosts }}
{{- if not $hosts }}
{{- $host := include "heidi.host" (dict "ingress" $ing "baseDomain" .Values.global.baseDomain) }}
{{- $hosts = required (printf "%s: ingress.enabled is true but no host could be resolved — set ingress.subdomain, ingress.hosts, or global.baseDomain" .Values.appName) (ternary (list $host) nil (ne $host "")) }}
{{- end }}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "heidi.ingressName" . }}
  namespace: ingress-basic
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: {{ .Values.global.clusterIssuer }}
    nginx.ingress.kubernetes.io/proxy-body-size: 30m
    nginx.ingress.kubernetes.io/proxy-buffer-size: 128k
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
                name: {{ include "heidi.ingressAlias" $ }}
                port:
                  number: {{ $.Values.port }}
    {{- end }}

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
{{- end -}}
