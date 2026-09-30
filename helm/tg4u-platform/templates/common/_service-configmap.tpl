{{/*
Per-service environment ConfigMap.

The platform services are configured through environment variables —
every property in their application.properties reads one — so a service's
configuration is a flat map of env vars, not a mounted properties file. The
ConfigMap is projected with `envFrom`, so adding a key needs no template change.

Four layers, later wins:
  1. heidi.service.commonConfig — what every Spring service needs
  2. heidi.<service>.defaultConfig — computed by the chart, mostly URLs
  3. <service>.config           — declared in values.yaml
  4. <service>.env              — real container env, overrides everything

Layers 1 and 2 live in common/_service-env.tpl, one define per service, so each
service's wiring can be read in one place.
*/}}

{{/*
The merged config map for a service, as YAML. Shared by the ConfigMap itself and
by the deployment's checksum annotation, so the two can never disagree.
*/}}
{{- define "heidi.service.configData" -}}
{{- $defaults := dict -}}
{{- $tpl := printf "heidi.%s.defaultConfig" .Values.configKey -}}
{{- if .Values.configKey -}}
{{- $defaults = include $tpl . | fromYaml -}}
{{- end -}}
{{- $common := include "heidi.service.commonConfig" . | fromYaml -}}
{{- $merged := merge (dict) (.Values.config | default dict) $defaults $common -}}
{{- $out := dict -}}
{{- range $k, $v := $merged }}
{{- if not (kindIs "invalid" $v) }}{{ $_ := set $out $k ($v | toString) }}{{ end }}
{{- end }}
{{- toYaml $out -}}
{{- end -}}

{{- define "heidi.service.configmap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "heidi.fullname" . }}-config
  namespace: {{ .Values.global.namespace }}
data:
  {{- include "heidi.service.configData" . | nindent 2 }}
{{- end -}}
