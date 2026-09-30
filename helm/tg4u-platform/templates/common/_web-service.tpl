{{/*
Service for the cockpit frontend
*/}}
{{- define "heidi.web.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "heidi.fullname" . }}
  namespace: {{ .Values.global.namespace }}
spec:
  type: ClusterIP
  ports:
    - name: http
      port: {{ .Values.port }}
      targetPort: {{ .Values.port }}
  selector:
    app: {{ include "heidi.fullname" . }}
{{- end -}}
