{{/*
Deployment for a Spring Boot service in the TG4U chart.

Configuration arrives as environment variables only — the platform services read
every setting from one — so there is no config volume or SPRING_CONFIG_*
indirection. Sources, later winning over earlier:

  1. <service>-config ConfigMap   chart-computed URLs + values.yaml config
  2. the generated dev Secret     shared keys, see global.devSecrets.generate
  3. <service>-secrets            values pinned in <service>.devSecrets.data
  4. <service>-database           datasource URL/username/password, unless
                                  global.externalDatabase.existingSecret names a
                                  Secret to read them from instead
  5. .Values.env                  container env, which always wins
*/}}
{{/*
Datasource environment for a service whose credentials live in a Secret that
already exists in the namespace (global.externalDatabase.existingSecret, or a
per-service one).

Every field is read with a secretKeyRef, so nothing sensitive is written into
the Deployment; SPRING_DATASOURCE_URL is then assembled around them with
Kubernetes' own $(VAR) expansion. That expansion only sees variables defined
EARLIER in this same env list — never ones arriving through envFrom — which is
why the pieces are env entries here rather than another Secret, and why the URL
comes last.
*/}}
{{- define "heidi.service.databaseEnv" -}}
{{- $secret := include "heidi.databaseExistingSecret" . -}}
{{- if and (include "heidi.usesDatabase" .) $secret (include "heidi.databaseSecretIsMapped" .) -}}
{{- $ext := .Values.global.externalDatabase | default dict -}}
{{- $keys := include "heidi.databaseSecretKeys" . | fromYaml -}}
{{- $host := $ext.host -}}
{{- $port := $ext.port | default 5432 -}}
{{- $name := include "heidi.databaseName" . -}}
{{- if $keys.host }}
- name: HEIDI_DATASOURCE_HOST
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: {{ $keys.host }}
{{- $host = "$(HEIDI_DATASOURCE_HOST)" -}}
{{- else -}}
{{- $host = required (printf "%s: global.externalDatabase.host is not set and secretKeys.host names no key to read it from" .Values.appName) $host -}}
{{- end }}
{{- if $keys.port }}
- name: HEIDI_DATASOURCE_PORT
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: {{ $keys.port }}
{{- $port = "$(HEIDI_DATASOURCE_PORT)" -}}
{{- end }}
{{- if $keys.database }}
- name: HEIDI_DATASOURCE_NAME
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: {{ $keys.database }}
{{- $name = "$(HEIDI_DATASOURCE_NAME)" -}}
{{- else -}}
{{- $name = required (printf "%s: no database name — set devDatabase.database or secretKeys.database" .Values.appName) $name -}}
{{- end }}
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: {{ required (printf "%s: global.externalDatabase.secretKeys.username must name the key holding the username" .Values.appName) $keys.username }}
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: {{ required (printf "%s: global.externalDatabase.secretKeys.password must name the key holding the password" .Values.appName) $keys.password }}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ $host }}:{{ $port }}/{{ $name }}{{ $ext.jdbcParams }}"
{{- end -}}
{{- end -}}

{{/* Signing seeds are split from the general generated Secret: that Secret is
   envFrom'd into every workload, while each registered client seed must stay in
   its owning backend. The signing service needs only the platform public key. */}}
{{- define "heidi.service.signingClientEnv" -}}
{{- if and .Values.urls.signingEnabled .Values.global.devSecrets.generate.enabled -}}
{{- $secret := .Values.global.devSecrets.generate.signingClientSecretName | default (printf "%s-signing-clients" .Values.global.devSecrets.generate.secretName) -}}
{{- if eq .Values.appName "platform-api" }}
- name: HEIDI_PLATFORM_SIGNING_PROVIDER_CLIENT_SEED
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: HEIDI_PLATFORM_SIGNING_PROVIDER_CLIENT_SEED
{{- else if eq .Values.appName "issuer" }}
- name: HEIDI_ISSUER_SIGNING_PROVIDER_CLIENT_SEED
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: HEIDI_ISSUER_SIGNING_PROVIDER_CLIENT_SEED
{{- else if eq .Values.appName "verifier" }}
- name: HEIDI_VERIFIER_SIGNING_PROVIDER_CLIENT_SEED
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: HEIDI_VERIFIER_SIGNING_PROVIDER_CLIENT_SEED
{{- else if eq .Values.appName "signing" }}
- name: HEIDI_SIGNING_AUTH_CLIENTS_PLATFORM
  valueFrom:
    secretKeyRef:
      name: {{ $secret }}
      key: HEIDI_SIGNING_AUTH_CLIENTS_PLATFORM
{{- end }}
{{- end -}}
{{- end -}}

{{- define "heidi.service.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "heidi.fullname" . }}
  namespace: {{ .Values.global.namespace }}
spec:
  replicas: {{ .Values.replicaCount | default 1 }}
  selector:
    matchLabels:
      app: {{ include "heidi.fullname" . }}
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  minReadySeconds: 60
  template:
    metadata:
      labels:
        app: {{ include "heidi.fullname" . }}
      annotations:
        # Rolls the pods whenever the rendered configuration changes
        checksum/config: {{ include "heidi.service.configData" . | sha256sum }}
        {{- if (.Values.devSecrets | default dict).enabled }}
        checksum/dev-secrets: {{ include "heidi.service.devSecretsData" . | sha256sum }}
        {{- end }}
        {{- if and .Values.urls.signingEnabled .Values.global.devSecrets.generate.enabled }}
        # The signing-client Secret is populated by a pre-install/pre-upgrade
        # hook. This marker makes the first upgrade after introducing the
        # Secret-backed client wiring roll the pods so they read the seed.
        checksum/signing-client-wiring: {{ include "heidi.service.signingClientEnv" . | sha256sum }}
        {{- end }}
    spec:
      nodeSelector:
        "kubernetes.io/os": linux
      containers:
        - name: {{ include "heidi.fullname" . }}
          image: {{ include "heidi.image" . | quote }}
          imagePullPolicy: {{ (.Values.image | default dict).pullPolicy | default .Values.global.imagePullPolicy }}
          ports:
            - containerPort: {{ .Values.port }}
              name: http
            {{- if .Values.managementPort }}
            - containerPort: {{ .Values.managementPort }}
              name: management
            {{- end }}
          resources:
            {{- toYaml (.Values.resources | default (dict "requests" (dict "memory" "800Mi") "limits" (dict "memory" "800Mi"))) | nindent 12 }}
          {{- $probes := .Values.probes | default dict }}
          {{- if $probes.enabled }}
          {{- /* The startupProbe is what makes the other two safe: Kubernetes
                 runs neither liveness nor readiness until it has succeeded once,
                 and they target managementPort, where actuator now listens —
                 so there is no initialDelaySeconds to guess wrong. These images
                 answer in 37-51s cold, and a loaded cluster is slower — a fixed
                 delay either fires too early ("connection refused" on every
                 rollout) or wastes time on every restart. The budget below is
                 startupPeriodSeconds x startupFailureThreshold. */}}
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {{ .Values.managementPort | default .Values.port }}
            periodSeconds: {{ $probes.startupPeriodSeconds | default 5 }}
            failureThreshold: {{ $probes.startupFailureThreshold | default 60 }}
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: {{ .Values.managementPort | default .Values.port }}
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {{ .Values.managementPort | default .Values.port }}
            periodSeconds: 20
          {{- end }}
          envFrom:
            - configMapRef:
                name: {{ include "heidi.fullname" . }}-config
            {{- if (.Values.devSecrets | default dict).enabled }}
            {{- /* Order matters: Kubernetes lets the LAST source win for a
                   duplicate key, so the generated fallback goes first and any
                   value set in <service>.devSecrets.data overrides it. */}}
            {{- if .Values.global.devSecrets.generate.enabled }}
            - secretRef:
                name: {{ .Values.global.devSecrets.generate.secretName }}
            {{- end }}
            - secretRef:
                name: {{ include "heidi.fullname" . }}-secrets
            {{- else }}
            {{- range .Values.externalSecrets }}
            - secretRef:
                name: {{ . }}
            {{- end }}
            {{- end }}
            {{- if include "heidi.rendersDatabaseSecret" . }}
            - secretRef:
                name: {{ include "heidi.fullname" . }}-database
            {{- else if and (include "heidi.usesDatabase" .) (include "heidi.databaseExistingSecret" .) }}
            {{- /* An existing Secret with no key mapping is taken as already
                   carrying SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD, so it can
                   be handed over whole. A mapped one is read key by key in
                   `env` below, where $(VAR) expansion can reach it. */}}
            {{- if not (include "heidi.databaseSecretIsMapped" .) }}
            - secretRef:
                name: {{ include "heidi.databaseExistingSecret" . }}
            {{- end }}
          {{- end }}
          {{- $dbEnv := include "heidi.service.databaseEnv" . }}
          {{- $signingClientEnv := include "heidi.service.signingClientEnv" . }}
          {{- if or $dbEnv $signingClientEnv .Values.env }}
          env:
            {{- with $signingClientEnv }}
            {{- . | trim | nindent 12 }}
            {{- end }}
            {{- with $dbEnv }}
            {{- . | trim | nindent 12 }}
            {{- end }}
            {{- range $key, $val := (.Values.env | default dict) }}
            - name: {{ $key }}
              value: {{ $val | quote }}
            {{- end }}
          {{- end }}
      {{- with .Values.global.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
{{- end -}}
