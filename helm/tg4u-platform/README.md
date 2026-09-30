# tg4u-platform

Helm chart for deploying TG4U, built on the
[Heidi Platform](https://github.com/heidiverse/heidi-platform). Configure the
chart for your cluster and set `global.imageTag` to a published TG4U release.

Images are published to the GitHub Container Registry namespace for
[Amt für Informatik Kanton Thurgau](https://github.com/Amt-fur-Informatik-Kanton-Thurgau):
`ghcr.io/amt-fur-informatik-kanton-thurgau`. The chart is published there as
`tg4u-platform`, with a chart version matching the release tag without its
leading `v`. Set the five image packages and the chart package to public after
their first publication so deployments can pull them without credentials.

## What it deploys

The TG4U release workflow publishes five images for each release tag, and this
chart deploys them:

| Values key    | Image                                      | Port | Default host                    |
|---------------|--------------------------------------------|------|---------------------------------|
| `platformApi` | `<registry>/tg4u-platform-api`             | 8080 | `api.<baseDomain>`              |
| `issuer`      | `<registry>/tg4u-issuer`                   | 8082 | `issuer.<baseDomain>`           |
| `verifier`    | `<registry>/tg4u-verifier`                 | 8083 | `verifier.<baseDomain>`         |
| `web`         | `<registry>/tg4u-web`                      | 80   | `platform.<baseDomain>`         |
| `signing`     | `<registry>/tg4u-signing-service`          | 8086 | none — cluster-internal         |

`<registry>` is `global.imageRegistry`, by default
`ghcr.io/amt-fur-informatik-kanton-thurgau`. If the published images end up
with different names, change `<service>.image.name`.

Deploying `signing` switches the issuer, verifier and platform API from the
in-process signer to that service and points them at it on the cluster network;
`signing.enabled: false` puts them back. It has no ingress — it signs
credentials and has no business being public — and it is the one backend with no
database. The chart configures registered-key authentication with separate,
persistent client seeds. The platform provider is added to the global provider
list on API startup; the issuer and verifier public keys are accepted from the
Cockpit.

### Signing provider in the Cockpit

With the chart-managed signing service enabled, open **Issuer definitions** (or
**Keys → Providers**) in the Cockpit. The global **Heidi Software Signing
Service** provider is created automatically by the platform API. Open its
connection details and accept the **issuer** and **verifier** clients when they
appear. This registers their public keys with the signing service; no API key or
shared bearer token needs to be copied into the Cockpit.

To add a different signing service manually, use **Add provider**, enter its
cluster-reachable endpoint, choose the authentication mode that service
advertises, run **Check connection**, then **Create provider**. For registered
authentication, the platform must have its own client seed configured. If the
service advertises platform-assisted client acceptance, use the **Accept issuer**
and **Accept verifier** buttons after creating it.


`heidi-platform-api` is the combined coordinator + entity API — the two used to
be separate services and separate repositories, and are now one deployment.

## Authentication

The chart ships without a login proxy. By default every Spring service runs
the `no-security` profile (`global.springProfiles`), which grants every role to
every caller — only defensible on a cluster nobody else can reach, or behind an
authenticating proxy you deploy yourself.

To authenticate against an OIDC provider instead, clear
`global.springProfiles` (or `platformApi.springProfiles`) and set
`platformApi.config.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` to the
provider's issuer. The platform API then validates bearer tokens itself and
enforces the roles and tenant claims below.

### Platform roles and tenant claims

For a user JWT accepted by the platform API, the relevant claims are:

| Claim | Required shape and meaning |
|---|---|
| `sub` | Stable user subject. Required when platform services build a user profile. |
| `permissions` | Array of strings matching the exact role names below. The platform maps them directly to Spring authorities, with no `ROLE_` prefix. |
| `companyId` | The user's tenant ID. This is the external claim name; the platform maps it to its internal `tenantId`. |
| `username`, `email`, `display_name` | Optional profile data. The API uses `username` as the profile email, falling back to `email`; the cockpit falls back through the display name, email, then `sub`. |

`SUPER_ADMIN` can list tenants and perform cross-tenant operations; operations
that create or update tenant-scoped data require a `tenantId` to be selected in
the request. Other users are scoped to the tenant in `companyId`; if they pass a
tenant ID in a request it must match. A non-super-admin token therefore needs a
valid `companyId` for the tenant it belongs to.

The platform recognizes these exact values in `permissions`:

| Role | Typical platform access |
|---|---|
| `SUPER_ADMIN` | Cross-tenant administration and operations reserved for the platform owner. |
| `ADMIN` | Tenant administration; accepted by selected management and operational endpoints. |
| `MANAGER` | Tenant management such as tenant settings and user administration; accepted by selected management endpoints. |
| `EDITOR` | Content/configuration operations such as credential schemas, proof schemas, and status lists. |
| `OPERATOR` | Operational credential/process actions; the coordinator accepts this role for credential JWT operations. |
| `DEVELOPER` | Integration management endpoints, alongside the relevant admin roles. |

These are individual authorities, not an inherited hierarchy. Each endpoint
declares its own accepted roles, so the tables describe common use rather than
granting every operation in that category. For example, tenant listing and
deletion are `SUPER_ADMIN` only, while updates allow `ADMIN`, `MANAGER`, or
`SUPER_ADMIN`. For broad cockpit administration, assign `ADMIN` plus the
specialized `EDITOR`, `OPERATOR`, or `DEVELOPER` authorities the user's tasks
need; `ADMIN` does not imply those roles.

All five are built from a single tag, so pinning a release is one edit:
`global.imageTag` in [values.yaml](values.yaml). A service can override
`image.registry` and `image.tag` when it uses a different registry or build;
`image.imageRegistry` and `image.imageTag` are accepted aliases for overlays
that mirror the global names. A full `image.repository` overrides the registry
and image name together.

## Prerequisites

- `kubectl` connected to a running cluster
- Helm 3

If you deploy from a private mirror, create a `docker-registry` Secret in the
release namespace and name it in `global.imagePullSecrets`.

## Deploy

Each TG4U release publishes an OCI chart to GHCR. Install a published release
with its matching image tag like this:

```bash
helm upgrade --install tg4u \
  oci://ghcr.io/amt-fur-informatik-kanton-thurgau/tg4u-platform \
  --version <release> \
  --namespace <namespace> --create-namespace \
  -f <your-values.yaml> \
  --set global.namespace=<namespace> \
  --set global.environment=dev \
  --set global.imageTag=<release>
```

The chart and app versions both match the release tag without its leading `v`.
For example, release `v1.2.3` is installed with `--version 1.2.3` and
`global.imageTag=1.2.3`.

You can also render or install a chart checkout locally:

```bash
# Render without touching the cluster
helm lint . --set global.imageTag=<tag>
helm template tg4u . --namespace <namespace> --set global.imageTag=<tag>

# Deploy (or upgrade)
helm upgrade --install tg4u . \
  --namespace <namespace> --create-namespace \
  -f <your-values.yaml> \
  --set global.namespace=<namespace> \
  --set global.environment=dev \
  --set global.imageTag=<tag>

# Remove the release. The Postgres PVC and the generated dev Secrets
# (heidi-dev-generated, heidi-signing-clients) are kept; delete them by hand
# to start from scratch.
helm uninstall tg4u --namespace <namespace>
```

`global.environment` suffixes every resource name, and `global.namespace` must
match the release namespace.

## Service Configuration

The platform services read every setting from an environment variable, so a
service's configuration is a flat map of environment variables rather than a
mounted properties file. Each service gets one ConfigMap, projected with
`envFrom`. Three layers, later winning over earlier:

1. **chart-computed defaults** — [templates/common/_service-env.tpl](templates/common/_service-env.tpl)
2. **`<service>.config`** — declared in [values.yaml](values.yaml)
3. **`<service>.env`** — real container env, overrides everything

The chart computes what follows from the deployment itself, which is almost
entirely service URLs. Those come in two flavours and are not interchangeable:

- **internal** — the cluster-local Service URL. Service-to-service calls take
  this path: the issuer reaching the platform API, the verifier reaching the
  entity API.
- **public** — `https://<ingress host>`. Anything a wallet, a browser or another
  party has to resolve has to be this one: credential offers, QR codes,
  federation entity statements, wallet response endpoints.

Because both are derived from `global.baseDomain` and the ingress hosts,
changing a hostname moves every URL that depends on it in one edit.

The cockpit frontend is static, so its URLs cannot be baked into the image. It
fetches `/config.json` at start-up; the chart renders that file into a ConfigMap
and mounts it over the one in the image, filling in every service URL from the
same source. Override individual keys under `web.config`.

### Configuration the `local` profile used to supply

The upstream Heidi Platform `local` profile does more than pick localhost URLs,
and a deployment does not run it. Anything it switched off has to be switched off
here instead — `templates/common/_service-env.tpl` holds these under
`heidi.service.commonConfig`:

- `SPRING_DOCKER_COMPOSE_ENABLED=false`. `spring-boot-docker-compose` is an
  optional dependency of `heidi-platform-api` and `heidi-verifier-backend`, and
  it is **on** whenever it is on the classpath. It then looks for a compose file
  beside the working directory and aborts start-up with `No Docker Compose file
  found in directory` — which in a container it never finds.

### Probes

`<service>.probes.enabled` fronts readiness and liveness with a **startupProbe**.
Kubernetes runs neither until the startup probe has succeeded once, so the probe
budget can accommodate a slow start without guessing an `initialDelaySeconds`.
The budget is `startupPeriodSeconds x startupFailureThreshold`, five minutes by
default; tune it using startup measurements from the target cluster.

### Spring profiles

`global.springProfiles` defaults to `no-security`, the profile that lets the
platform run without an identity provider — **it grants every role to every
caller**, so it is only defensible behind an authenticating proxy or on a
cluster nobody else can reach. To use an OIDC provider instead, clear it and set
`spring.security.oauth2.resourceserver.jwt.issuer-uri` through
`<service>.config`.

This value is not optional for the issuer: its image pins
`spring.profiles.active=local` in `application.properties`, and only the
environment variable keeps that profile's localhost datasource defaults out of
the cluster.

## Databases

Two settings pick the **server**, and they are mutually exclusive:

| | what it is |
|---|---|
| `global.devDatabase` | a Postgres container this chart deploys in `global.namespace`, with a PVC — for development |
| `global.externalDatabase` | a server that already exists (Azure, RDS, …); the chart deploys nothing and only connects |

Which database each service uses **inside** that server is a separate question,
answered per service under `<service>.database` — that block describes the
service's own datasource and never decides which server is in play:

```yaml
issuer:
  database:
    name: heidi_issuer          # the database inside the server
    existingSecret: ""          # a Secret holding the credentials
    enabled: true               # false for a service with no datasource (signing)
```

It used to be called `<service>.devDatabase`, with `database` where `name` now
is; that spelling is still read, so older values files keep working.

### The dev Postgres

`global.devDatabase.enabled` deploys one PostgreSQL instance with a PVC, and a
`post-install`/`post-upgrade` Job creates one database per enabled service —
`heidi_platform_api`, `heidi_issuer`, `heidi_verifier`. They do not share a
schema; the local development setup keeps the service databases separate for the
same reason. Each service then gets a `<service>-database` Secret carrying
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.

Override a service's database name under `<service>.database.name`; the init Job
reads the same values, so a renamed database is created automatically.

Turning `global.devDatabase.enabled` off drops the Postgres deployment, the Job
and the datasource Secrets — the datasource then has to come from either
`global.externalDatabase` below or an external Secret listed in
`<service>.externalSecrets`.

### An existing server

`global.externalDatabase.enabled` points every service with a datasource at a
server this chart does not deploy, instead of the one above. Turn
`global.devDatabase.enabled` off alongside it, or the chart deploys a Postgres
nothing connects to.

Credentials come from Secrets that already exist in the namespace — see below.
To let the chart
render them from this file instead, set `host`, `port`, `username`, `password`
and, for a managed instance, `jdbcParams` (appended to the JDBC URL verbatim, so
it carries its own leading `?`).

The database names still come from `<service>.database.name`, one per
service for the same schema reason as above, and **nothing creates them** — the
init Job only ever talks to the in-cluster Postgres, so they have to exist on the
external server before the first deploy.

#### Credentials from an existing Secret

`existingSecret` names a Secret already in `global.namespace`, so no credential
is written into the values file. Set it per service — the usual shape, since the
URL carries the database name and each service needs its own:

```yaml
global:
  externalDatabase:
    enabled: true

platformApi:
  database:
    existingSecret: heidi-platform-api-db-dev
issuer:
  database:
    existingSecret: heidi-issuer-db-dev
verifier:
  database:
    existingSecret: heidi-verifier-db-dev
```

Each Secret is handed to the container whole, as a `secretRef`, and must carry
these three keys under exactly these names:

```
SPRING_DATASOURCE_URL       jdbc:postgresql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

The URL arrives in one piece, so `host`, `port`, `jdbcParams` and
`<service>.database.name` are unused on this path. It is listed last among
the `envFrom` sources, so it wins over anything else defining those keys.
Changing a Secret afterwards needs `kubectl rollout restart deploy/<service>` —
the chart checksums only the Secrets it renders itself.

Create each Secret before the first deploy, for example:

```bash
kubectl -n <namespace> create secret generic heidi-verifier-db-dev \
  --from-literal=SPRING_DATASOURCE_URL='jdbc:postgresql://<host>:5432/heidi_verifier?sslmode=require' \
  --from-literal=SPRING_DATASOURCE_USERNAME='<user>' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<password>'
```

##### Fields stored separately

If the Secret holds the parts rather than a URL, name the keys instead:

```yaml
global:
  externalDatabase:
    enabled: true
    host: postgres.example.internal
    port: 5432
    secretKeys:
      database: POSTGRES_DB
      username: POSTGRES_USER
      password: POSTGRES_PASSWORD
```

Naming any key switches the chart to reading them one by one with a
`secretKeyRef` and assembling `SPRING_DATASOURCE_URL` itself from those values
plus `host`, `port` and `jdbcParams`; a key left empty falls back to the values
file, so a Secret holding only credentials works too. The assembly happens in the
container's `env` list rather than in a Secret because Kubernetes' `$(VAR)`
expansion only reaches variables declared earlier in that same list — never ones
arriving through a `secretRef`. Nothing sensitive lands in the Deployment either
way.

## Dev Secrets

Dev secrets are **not committed**. A `pre-install`/`pre-upgrade` hook Job
([templates/dev-secrets-generator.yaml](templates/dev-secrets-generator.yaml))
generates them on first install into a single Secret, `heidi-dev-generated`,
shared by every service — master keys, EC keypairs, a self-signed PKCS#12
issuance root with its password, and the basic-auth credential the services use
between themselves. What it generates is declared under
`global.devSecrets.generate.recipes` in [values.yaml](values.yaml).

One Secret for all services is deliberate: several keys must be byte-identical
across them, which a per-service generator could not guarantee.

| Key                                                 | Shared by                          |
|-----------------------------------------------------|------------------------------------|
| `HEIDI_TRUST_STATEMENTS_*`                          | platform-api, verifier             |
| `OID4VP_VERIFIER_FEDERATION_*`                      | platform-api, verifier             |
| `HEIDI_VERIFIER_SESSION_ENCRYPTION_MASTER_KEY`      | verifier                           |
| `HEIDI_ISSUER_API_KEY`                              | platform-api, issuer               |
| `HEIDI_ENCRYPTION_MASTER_KEY` / `HEIDI_ISSUER_TRANSACTION_CODE_MASTER_KEY` | platform-api, issuer |
| `HEIDI_ISSUER_SIGNING_CONFIGURATION_ENCRYPTION_KEY` | platform-api, issuer, verifier     |
| `HEIDI_SERVER_API_BASIC_AUTH` / `HEIDI_PLATFORM_BASIC_AUTH` | platform-api, issuer, verifier |

The last two rows are each one generated value published under two names, which
is what a recipe's `aliases` field does. The platform API validates the
basic-auth header the issuer and the verifier send; and it encrypts transaction
codes with the master key the issuer decrypts them with. Both pairs would be
silently broken by two independently generated values, and the issuer's
transaction-code key is easy to miss because it has no line in that service's
`application.properties` — it binds straight from the environment.

The verifier's four JWKs deserve a note. `JwkUtil.parseOrGenerate` falls back to
an ephemeral P-256 key when one is blank, so the service still starts without
them — but the key then changes on every restart and differs between replicas,
which breaks request-object signatures and wallet response decryption under
both. Its own javadoc says a deployment must supply them; generating them once
here is what makes them stable.

`HEIDI_ISSUER_DPOP_NONCE_KEY` is the same shape of problem: optional for a single
replica, mandatory the moment there are two, because a nonce minted by one pod is
rejected by the others.

**Existing keys are never rotated.** These master keys encrypt data at rest, so
an upgrade only ever adds keys that are missing — adding a recipe tops the
Secret up on the next deploy. To deliberately re-roll everything:

```bash
# destructive: data encrypted with the old keys is lost
kubectl -n <namespace> delete secret heidi-dev-generated heidi-signing-clients
helm upgrade --install tg4u . --namespace <namespace> ...   # regenerates them
kubectl -n <namespace> rollout restart deployment
```

To pin a value instead of generating it, set it under
`<service>.devSecrets.data`. That wins: the generated Secret is listed first in
the pod's `envFrom`, and Kubernetes lets the last source define a duplicate key.

Secrets that authenticate against an **external** party cannot be generated — a
random value would not authenticate — so those stay in `values.yaml`
(`HEIDI_RP_REGISTRAR_CLIENT_*`). The chart defaults the registrar URLs to the
reserved `registrar.example.invalid` domain, so registrar calls cannot reach an
external service until the endpoints and matching credentials are configured.

Setting `<service>.devSecrets.enabled: false` switches that service to the
`externalSecrets` list instead, which is the production path; the generator is
turned off entirely with `global.devSecrets.generate.enabled: false`.

The signing-client seeds are generated into a separate Secret and projected
only to the platform API, issuer, and verifier that own them. The signing
service receives only the platform public key. If disabling the generator,
provide `HEIDI_PLATFORM_SIGNING_PROVIDER_CLIENT_SEED` to `platformApi`, the
issuer and verifier `HEIDI_*_SIGNING_PROVIDER_CLIENT_SEED` values to their
respective services, and the matching `HEIDI_SIGNING_AUTH_CLIENTS_PLATFORM`
public key to `signing`.

## Issuer signing configuration

The issuer reads its signing provider endpoint and registered-auth mode from the
chart. Issuer identities and signing keys are managed through the platform API
and Cockpit; they are not configured with a static issuer-signing map in the
chart.

## Ingress naming

Everything the chart deploys lives in `global.namespace` and is named
`<global.project>-<appName>-<global.environment>` — `heidi-issuer-dev`. Two
deployments in two namespaces never meet, so that name only has to be unique
within one of them.

The Ingress objects are the exception. A cluster whose ingress controller lives
in its own namespace keeps them all together there
(`global.pathRouting.ingressNamespace`, `ingress-basic` by default), with an
ExternalName Service per backend so the rules can reach the workload. That
namespace is shared with every other deployment in the cluster, so deploying
this chart twice used to have both installs claim
`heidi-web-dev-ingress-rule` — and the second one fails on the name the first
already owns.

So those names carry which deployment they belong to instead of only the
project:

| | |
|---|---|
| Ingress | `<instance>-<appName>-<environment>-ingress-rule` |
| ExternalName alias | `<instance>-<appName>-<environment>` |
| TLS Secret (path routing) | `tls-secret-<instance>-<environment>` |
| TLS Secret (per service) | `tls-secret-<instance>-<appName>-<environment>` |

`<instance>` is `global.instance`, and defaults to `global.namespace`. For example,
these values yield distinct ingress names for two deployments:

```
kanton-prod-issuer-prod-ingress-rule
kanton-dev-issuer-dev-ingress-rule
```

Every Ingress in the cluster ends in `<environment>-ingress-rule`, whichever
chart put it there, so that suffix stays last and the deployment identity goes
in front of it.

Set `global.instance` to prefix them with something else. Where the Ingress sits
with the pods instead — minikube, where the addon's controller watches every
namespace and `ingressNamespace` is empty — no alias is written and the rules
point at the workload Service directly.
