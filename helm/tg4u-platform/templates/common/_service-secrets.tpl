{{/*
The explicitly-configured half of a service's dev secrets, as a YAML map with
empty values dropped.

Empty values must not be emitted: the generated Secret is listed FIRST in the
deployment's envFrom, so a key present here with a blank value would shadow the
generated one (Kubernetes takes the last source that defines a key).

Shared by the Secret itself and by the deployment's checksum annotation, so the
two can never disagree about what the Secret contains.
*/}}
{{- define "heidi.service.devSecretsData" -}}
{{- $data := dict }}
{{- range $key, $val := ((.Values.devSecrets | default dict).data | default dict) }}
{{- if $val }}{{ $_ := set $data $key ($val | toString) }}{{ end }}
{{- end }}
{{- toYaml $data }}
{{- end -}}

{{/*
Secrets: database credentials + dev secrets bundle
*/}}
{{- define "heidi.service.secrets" -}}
{{- if include "heidi.rendersDatabaseSecret" . }}
{{- $ext := .Values.global.externalDatabase | default dict }}
{{- $external := eq ($ext.enabled | toString) "true" }}
{{- $svcDb := include "heidi.serviceDb" . | fromYaml }}
apiVersion: v1
kind: Secret
metadata:
  name: {{ include "heidi.fullname" . }}-database
  namespace: {{ .Values.global.namespace }}
type: Opaque
stringData:
  {{- /* jdbcParams is appended verbatim, so it carries its own leading "?" —
         an external server usually needs at least ?sslmode=require. */}}
  SPRING_DATASOURCE_URL: "jdbc:postgresql://{{ include "heidi.databaseHostPort" . }}/{{ include "heidi.databaseName" . }}{{ if $external }}{{ $ext.jdbcParams }}{{ end }}"
{{- if $external }}
  SPRING_DATASOURCE_USERNAME: {{ $svcDb.username | default (required "global.externalDatabase.username is required" $ext.username) | quote }}
  SPRING_DATASOURCE_PASSWORD: {{ $svcDb.password | default (required "global.externalDatabase.password is required (or name an existingSecret)" $ext.password) | quote }}
{{- else }}
  SPRING_DATASOURCE_USERNAME: {{ $svcDb.username | default .Values.global.devDatabase.postgres.username | quote }}
  SPRING_DATASOURCE_PASSWORD: {{ $svcDb.password | default .Values.global.devDatabase.postgres.password | quote }}
{{- end }}
{{- end }}
{{- if (.Values.devSecrets | default dict).enabled }}
---
apiVersion: v1
kind: Secret
metadata:
  name: {{ include "heidi.fullname" . }}-secrets
  namespace: {{ .Values.global.namespace }}
type: Opaque
{{- /* Renders `{}` when nothing is set explicitly, which is the norm —
       everything self-contained comes from global.devSecrets.generate. */}}
stringData:
  {{- include "heidi.service.devSecretsData" . | nindent 2 }}
{{- end }}
{{- end -}}
