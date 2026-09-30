{{/*
Runtime configuration for the cockpit frontend.

heidi-web is built as static files, so its service URLs cannot be baked in at
build time — the app fetches /config.json at startup instead. The nginx image
serves from /var/www/web, so this ConfigMap is mounted over that one file and
the same image serves any environment.

Every URL here is resolved by the browser, so all of them are public. They carry
a trailing slash because the frontend concatenates paths onto them.
*/}}
{{- define "heidi.web.configData" -}}
{{- $u := .Values.urls -}}
{{- $api := printf "%s/" $u.platformApiPublic -}}
{{- $defaults := dict
      "heidiApiBaseUrl" $api
      "heidiEntityWsBaseUrl" $api
      "heidiEntityWsNoProxyBaseUrl" $api
      "heidiCoordinatorBaseUrl" $api
      "heidiCoordinatorNoProxyBaseUrl" $api
      "heidiIntegrationBaseUrl" $api
      "heidiIssuerBaseUrl" (printf "%s/" $u.issuerPublic)
      "funkeUrl" (printf "%s/" $u.webPublic)
      "heidiDefaultNamespace" "org.iso.18013.5.1"
      "heidiDefaultDoctype" "org.iso.18013.5.1.mDL"
      "appVersion" (.Values.image.tag | default .Values.global.imageTag | toString) -}}
{{- merge (dict) (.Values.config | default dict) $defaults | toJson -}}
{{- end -}}

{{- define "heidi.web.configmap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "heidi.fullname" . }}-config
  namespace: {{ .Values.global.namespace }}
data:
  config.json: |
    {{ include "heidi.web.configData" . }}
{{- end -}}
