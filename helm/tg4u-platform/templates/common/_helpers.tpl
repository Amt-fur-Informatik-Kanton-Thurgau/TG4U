{{/*
Generate the full resource name: <project>-<appName>-<environment>
Example: tg4u-platform-api-dev
*/}}
{{- define "heidi.fullname" -}}
{{- printf "%s-%s-%s" .Values.global.project .Values.appName .Values.global.environment -}}
{{- end -}}

{{/*
Which deployment of this chart a resource belongs to.

Everything in global.namespace is namespaced away from every other deployment
and needs no such marker. The Ingress objects do not have that luxury: a cluster
with a shared ingress controller keeps them all in one namespace
(global.pathRouting.ingressNamespace), where two deployments of this chart —
say the same stack in two separate workload namespaces — would
otherwise both want to be called tg4u-web-dev-ingress-rule, and the second
install fails on the name the first already owns. The same goes for their TLS
Secrets and for the ExternalName aliases that reach back into the workload
namespace.

So the names of those resources are prefixed with this instead of with
global.project: it says which deployment a rule in that shared namespace belongs
to. It defaults to global.namespace, which is already unique per deployment —
two deployments sharing a namespace would collide on everything else long before
they got here. Set global.instance to prefix them with something else.
*/}}
{{- define "heidi.instance" -}}
{{- .Values.global.instance | default .Values.global.namespace -}}
{{- end -}}

{{/*
Name of a service's Ingress, and of the ExternalName alias it points at, both of
which live in the shared ingress namespace: <instance>-<appName>-<environment>,
with -ingress-rule appended for the Ingress itself. Every Ingress in the cluster
ends in <environment>-ingress-rule, whoever deployed it, so the suffix stays
last and the deployment identity goes in front. See heidi.instance.
*/}}
{{- define "heidi.ingressName" -}}
{{- printf "%s-ingress-rule" (include "heidi.ingressAlias" .) -}}
{{- end -}}

{{- define "heidi.ingressAlias" -}}
{{- printf "%s-%s-%s" (include "heidi.instance" .) .Values.appName .Values.global.environment -}}
{{- end -}}

{{/*
Resolve the external host of a service, in order of precedence:
  1. ingress.hosts        — explicit list, first entry wins
  2. <subdomain>.<baseDomain>
  3. <baseDomain>         — apex, when no subdomain is set
Renders the empty string when nothing can be resolved.

Argument: dict "ingress" <ingress map> "baseDomain" <string>
*/}}
{{- define "heidi.host" -}}
{{- $ing := .ingress | default dict -}}
{{- if $ing.hosts -}}
{{- first $ing.hosts -}}
{{- else if .baseDomain -}}
{{- if $ing.subdomain -}}
{{- printf "%s.%s" $ing.subdomain .baseDomain -}}
{{- else -}}
{{- .baseDomain -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Cross-service URLs, computed once from the ROOT context and handed to every
per-service template through .Values.urls.

Two flavours per service, and they are not interchangeable:

  <svc>Internal  cluster-local Service URL. Service-to-service calls take this
                 path — it never leaves the cluster and needs no ingress.
  <svc>Public    https://<ingress host>. Anything a wallet, a browser or another
                 party has to resolve must be this one: credential offers, QR
                 codes, federation entity statements, redirect URLs.

With global.pathRouting on, the whole stack answers on one host and the public
URLs differ only by their path prefix — https://example.com/issuer and so
on. The internal ones do not change: a pod still calls a Service, never the
ingress. Host and Hostname stay bare, since an origin and a certificate SAN
have no path.

Consumed via `include "heidi.urls" . | fromYaml`.
*/}}
{{- define "heidi.urls" -}}
{{- $g := .Values.global -}}
{{- $routing := $g.pathRouting | default dict -}}
{{- $prefixes := $routing.prefixes | default dict -}}
{{- $out := dict -}}
{{- $instance := include "heidi.instance" . -}}
{{- $services := dict
      "platformApi" (dict "svc" .Values.platformApi "port" 8080)
      "issuer"      (dict "svc" .Values.issuer      "port" 8082)
      "verifier"    (dict "svc" .Values.verifier    "port" 8083)
      "web"         (dict "svc" .Values.web         "port" 80)
      "signing"     (dict "svc" .Values.signing     "port" 8086) -}}
{{- range $key, $spec := $services -}}
{{- $svc := $spec.svc | default dict -}}
{{- $name := printf "%s-%s-%s" $g.project ($svc.appName | default $key) $g.environment -}}
{{- $host := include "heidi.host" (dict "ingress" $svc.ingress "baseDomain" $g.baseDomain) -}}
{{- $public := "" -}}
{{- $prefix := "" -}}
{{- /* One host for everything, services told apart by path. A service the
       routing table does not name keeps its own host — the signing service has
       no public address at all and stays as it is. */}}
{{- $routedBase := "" -}}
{{- if and $routing.enabled (hasKey $prefixes $key) -}}
{{- $host = $routing.host | default $g.baseDomain -}}
{{- $prefix = trimSuffix "/" (index $prefixes $key) -}}
{{- /* pathRouting.publicUrl is to the whole stack what <service>.publicUrl is
       to one service: the address to hand out when it is not https://<host>.
       A minikube cluster reached through a port-forward of the ingress
       controller is http://<machine>:8080, and every prefix hangs off that. */}}
{{- if $routing.publicUrl -}}
{{- $routedBase = trimSuffix "/" $routing.publicUrl -}}
{{- $host = regexReplaceAll "^https?://" $routedBase "" -}}
{{- else -}}
{{- $routedBase = printf "https://%s" $host -}}
{{- end -}}
{{- end -}}
{{- /* <service>.publicUrl wins outright. It exists because the public address
       is not always "https://<ingress host>": a minikube cluster reached over a
       port-forward, or a Tailscale tailnet where MagicDNS gives one name and
       the services are told apart by port, both need scheme and port spelled
       out. The host is then read back off it, since OID4VP_DC_API_EXPECTED_ORIGINS
       and ISSUANCE_LEAF_SAN want a bare host. */}}
{{- if $svc.publicUrl -}}
{{- $public = trimSuffix "/" $svc.publicUrl -}}
{{- $host = regexReplaceAll "^https?://" $public "" -}}
{{- else if ne $routedBase "" -}}
{{- $public = printf "%s%s" $routedBase $prefix -}}
{{- else if ne $host "" -}}
{{- $public = printf "https://%s" $host -}}
{{- end -}}
{{- $_ := set $out (printf "%sName" $key) $name -}}
{{- /* The Service the shared ingress namespace knows this service by — the
       ExternalName alias pointing back at $name. See heidi.instance. */}}
{{- $_ := set $out (printf "%sAlias" $key) (printf "%s-%s-%s" $instance ($svc.appName | default $key) $g.environment) -}}
{{- $_ := set $out (printf "%sPort" $key) (int $spec.port) -}}
{{- $_ := set $out (printf "%sInternal" $key) (printf "http://%s.%s.svc.cluster.local:%d" $name $g.namespace (int $spec.port)) -}}
{{- $_ := set $out (printf "%sHost" $key) $host -}}
{{- /* Host keeps the port, because a browser origin is host:port. Hostname
       drops it, because a certificate SAN is a name and a port in one is not
       a valid SAN. They differ only when publicUrl carries a port. */}}
{{- $_ := set $out (printf "%sHostname" $key) (regexReplaceAll ":[0-9]+$" $host "") -}}
{{- $_ := set $out (printf "%sPublic" $key) $public -}}
{{- end -}}
{{/* The per-service contexts are a merge of global + this service's values, so
     they cannot see sibling services. Whether the signing service is deployed
     decides how the other three sign, so it travels with the URLs. */}}
{{- $_ := set $out "signingEnabled" (ternary "yes" "" (((.Values.signing | default dict).enabled) | default false)) -}}
{{- toYaml $out -}}
{{- end -}}

{{/*
Full image reference for a service.

The TG4U release workflow publishes all five images with the same tag, so it
lives in global.imageTag and a service only overrides it to pin itself to a
different build. The registry defaults to global.imageRegistry. A service may
override the registry and tag under image.registry/image.tag; image.imageRegistry and
image.imageTag are accepted aliases for overlays that mirror the global names.
image.repository remains a full repository override and takes precedence over
the registry/name combination.
*/}}
{{- define "heidi.image" -}}
{{- $img := .Values.image | default dict -}}
{{- $registry := $img.registry | default ($img.imageRegistry | default .Values.global.imageRegistry) -}}
{{- $repo := $img.repository | default (printf "%s/%s" $registry (required (printf "%s: image.name or image.repository is required" .Values.appName) $img.name)) -}}
{{- $tag := $img.tag | default ($img.imageTag | default .Values.global.imageTag) -}}
{{- $tag = required (printf "%s: no image tag — set global.imageTag (or image.tag for this service)" .Values.appName) $tag -}}
{{- printf "%s:%s" $repo ($tag | toString) -}}
{{- end -}}

{{/*
A service's own datasource settings, normalised.

`<service>.database` is the block that describes where THIS service's data
lives, whichever server is in use:

  enabled         false for a service with no datasource at all (signing)
  name            the database inside the server
  existingSecret  a Secret in the namespace holding the credentials
  username        \ only when the chart renders the credentials itself,
  password        / i.e. no existingSecret
  secretKeys      which keys of that Secret hold what

`<service>.devDatabase` is the former name of the same block, with `database`
where `name` now is; it is still read, so existing values files keep working.
Neither has anything to do with which SERVER is used — that is
global.devDatabase (a Postgres container this chart deploys) versus
global.externalDatabase (one that already exists).
*/}}
{{- define "heidi.serviceDb" -}}
{{- $l := .Values.devDatabase | default dict -}}
{{- $n := .Values.database | default dict -}}
{{- $out := dict -}}
{{- if hasKey $n "enabled" -}}{{- $_ := set $out "enabled" $n.enabled -}}
{{- else if hasKey $l "enabled" -}}{{- $_ := set $out "enabled" $l.enabled -}}{{- end -}}
{{- $_ := set $out "name" ($n.name | default $l.database | default "") -}}
{{- $_ := set $out "existingSecret" ($n.existingSecret | default $l.existingSecret | default "") -}}
{{- $_ := set $out "username" ($n.username | default $l.username | default "") -}}
{{- $_ := set $out "password" ($n.password | default $l.password | default "") -}}
{{- $_ := set $out "secretKeys" (merge (deepCopy ($n.secretKeys | default dict)) ($l.secretKeys | default dict)) -}}
{{- toYaml $out -}}
{{- end -}}

{{/*
The database a service connects to: its own name, else the shared default of
whichever server is in use. It is per service either way — the three Spring
services do not share a schema, so they never share a database.
*/}}
{{- define "heidi.databaseName" -}}
{{- $ext := .Values.global.externalDatabase | default dict -}}
{{- $default := ternary ($ext.database | default "") .Values.global.devDatabase.postgres.database (eq ($ext.enabled | toString) "true") -}}
{{- (include "heidi.serviceDb" . | fromYaml).name | default $default -}}
{{- end -}}

{{/*
host:port of the database server: the external one when it is enabled, otherwise
the Postgres this chart deploys.
*/}}
{{- define "heidi.databaseHostPort" -}}
{{- $ext := .Values.global.externalDatabase | default dict -}}
{{- if eq ($ext.enabled | toString) "true" -}}
{{- printf "%s:%v" (required "global.externalDatabase.enabled is true but global.externalDatabase.host is not set" $ext.host) ($ext.port | default 5432) -}}
{{- else -}}
heidi-dev-postgres:5432
{{- end -}}
{{- end -}}

{{/*
Whether a service gets datasource environment at all: one of the two servers is
configured, unless the service opts out. The signing service has no datasource,
so handing it SPRING_DATASOURCE_* would be noise at best.
Renders a non-empty string for true, empty for false — `if` treats "" as false.
*/}}
{{- define "heidi.usesDatabase" -}}
{{- $ext := .Values.global.externalDatabase | default dict -}}
{{- if or .Values.global.devDatabase.enabled (eq ($ext.enabled | toString) "true") -}}
{{- if ne ((include "heidi.serviceDb" . | fromYaml).enabled | toString) "false" -}}yes{{- end -}}
{{- end -}}
{{- end -}}

{{/*
The Secret that already holds this service's database credentials, if any:
<service>.database.existingSecret, else global.externalDatabase.existingSecret.
Only consulted for an external server — the dev Postgres brings its own.
*/}}
{{- define "heidi.databaseExistingSecret" -}}
{{- $ext := .Values.global.externalDatabase | default dict -}}
{{- if eq ($ext.enabled | toString) "true" -}}
{{- (include "heidi.serviceDb" . | fromYaml).existingSecret | default $ext.existingSecret | default "" -}}
{{- end -}}
{{- end -}}

{{/*
Which key of that Secret holds what: the global mapping, with the service's own
entries winning. A key left empty means the Secret does not carry that field and
the value comes from this file instead.
*/}}
{{- define "heidi.databaseSecretKeys" -}}
{{- $g := (((.Values.global.externalDatabase | default dict).secretKeys) | default dict) -}}
{{- $s := ((include "heidi.serviceDb" . | fromYaml).secretKeys | default dict) -}}
{{- toYaml (merge (deepCopy $s) $g) -}}
{{- end -}}

{{/*
Whether that Secret is read key by key. Mapping no keys at all means the Secret
already carries SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD under those exact
names, and is handed to the container whole.
*/}}
{{- define "heidi.databaseSecretIsMapped" -}}
{{- $keys := include "heidi.databaseSecretKeys" . | fromYaml -}}
{{- range $field, $key := $keys -}}
{{- if $key -}}yes{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Whether the chart renders the <service>-database Secret itself — it does not
when an existing one was named.
*/}}
{{- define "heidi.rendersDatabaseSecret" -}}
{{- if include "heidi.usesDatabase" . -}}
{{- if not (include "heidi.databaseExistingSecret" .) -}}yes{{- end -}}
{{- end -}}
{{- end -}}
