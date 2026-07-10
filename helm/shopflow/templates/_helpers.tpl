{{/* Common labels applied to every object */}}
{{- define "shopflow.labels" -}}
app.kubernetes.io/part-of: shopflow
app.kubernetes.io/managed-by: helm
{{- end -}}

{{/* Build a full image reference for a service repo */}}
{{- define "shopflow.image" -}}
{{- printf "%s/%s:%s" .registry .repo .tag -}}
{{- end -}}
