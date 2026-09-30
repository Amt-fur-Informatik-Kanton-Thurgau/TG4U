{{/*
Deployment for the cockpit frontend (nginx serving static files)
*/}}
{{- define "heidi.web.deployment" -}}
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
        # subPath mounts do not track ConfigMap updates, so the pods have to be
        # rolled when config.json changes.
        checksum/config: {{ include "heidi.web.configData" . | sha256sum }}
    spec:
      nodeSelector:
        "kubernetes.io/os": linux
        # "kubernetes.io/arch": amd64
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            # try not to schedule a pod in a HA zone if there is already a pod with label <key>=<value>
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - {{ include "heidi.fullname" . }}
                topologyKey: topology.kubernetes.io/zone
            # try not to schedule a pod on a node if there is already a pod with label <key>=<value>
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values:
                        - {{ include "heidi.fullname" . }}
                topologyKey: kubernetes.io/hostname
      containers:
        - name: {{ include "heidi.fullname" . }}
          image: {{ include "heidi.image" . | quote }}
          imagePullPolicy: {{ (.Values.image | default dict).pullPolicy | default .Values.global.imagePullPolicy }}
          ports:
            - containerPort: {{ .Values.port }}
          resources:
            {{- toYaml (.Values.resources | default (dict "requests" (dict "cpu" "100m" "memory" "64Mi") "limits" (dict "memory" "64Mi"))) | nindent 12 }}
          volumeMounts:
            - name: web-config
              mountPath: /var/www/web/config.json
              subPath: config.json
              readOnly: true
      volumes:
        - name: web-config
          configMap:
            name: {{ include "heidi.fullname" . }}-config
      {{- with .Values.global.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
{{- end -}}
