set dotenv-load := true
set dotenv-filename := ".env.local"
set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

tg4u_root := justfile_directory()
platform_root := env_var_or_default("TG4U_PLATFORM_ROOT", tg4u_root + "/.tg4u/platform")
upstream_repository := env_var_or_default("TG4U_UPSTREAM_REPOSITORY", "")
pnpm := env_var_or_default("PNPM", "pnpm")
platform_base_url := env_var_or_default("HEIDI_PLATFORM_PUBLIC_BASE_URL", "http://127.0.0.1:8080/")
tg4u_platform_extension_maven := "mvn -Dheidi.extension.groupId=ch.tg4u -Dheidi.extension.artifactId=tg4u-platform-api-extension -Dheidi.extension.version=1.0.0-SNAPSHOT"
platform_extension_maven := tg4u_platform_extension_maven
tg4u_issuer_extension_maven := "mvn -Dheidi.extension.groupId=ch.tg4u -Dheidi.extension.artifactId=tg4u-issuer-extension -Dheidi.extension.version=1.0.0-SNAPSHOT"
verifier_maven := "mvn"

# The TG4U platform API extension calls the issuer with this key, and the OSS
# local setup does not define it. Match the issuer's `local` profile default so
# both sides agree; set HEIDI_ISSUER_API_KEY (e.g. in .env.local) to override.
export HEIDI_ISSUER_API_KEY := env_var_or_default("HEIDI_ISSUER_API_KEY", "local-development-api-key")

# Spring Boot removes dashes when mapping property names to environment names.
# These defaults brand the fresh local tenant, issuer identity, and request user.
export HEIDI_PLATFORM_LOCAL_TENANTDISPLAYNAME := env_var_or_default("HEIDI_PLATFORM_LOCAL_TENANTDISPLAYNAME", "Kanton Thurgau")
export HEIDI_PLATFORM_LOCAL_ISSUERDISPLAYNAME := env_var_or_default("HEIDI_PLATFORM_LOCAL_ISSUERDISPLAYNAME", "Kanton Thurgau")
export HEIDI_PLATFORM_LOCAL_USERNAME := env_var_or_default("HEIDI_PLATFORM_LOCAL_USERNAME", "admin@tg4u.example")
export HEIDI_PLATFORM_LOCAL_DISPLAYNAME := env_var_or_default("HEIDI_PLATFORM_LOCAL_DISPLAYNAME", "Kanton Admin")

# Replace Heidi Platform's generic local PID fixtures with TG4U's example
# credential. Set HEIDI_SCHEMA_SEED_MANIFEST in the shell or .env.local to use
# another manifest.
export HEIDI_SCHEMA_SEED_MANIFEST := env_var_or_default("HEIDI_SCHEMA_SEED_MANIFEST", tg4u_root + "/local-dev/schema-seed.json")

default:
    @just --list

# Prepare a generated Heidi Platform workspace and install the TG4U web extension.
tg4u-sync:
    scripts/prepare-platform-workspace.sh "{{platform_root}}" "{{tg4u_root}}/oss.lock" "{{upstream_repository}}"
    scripts/reset-platform-workspace.sh "{{platform_root}}"
    scripts/assemble-into-platform.sh "{{platform_root}}"

# Install the combined frontend dependencies and local TG4U runtime config.
tg4u-setup: tg4u-sync tg4u-runtime-config
    cd "{{platform_root}}/heidi-web" && {{pnpm}} install

tg4u-runtime-config:
    scripts/install-tg4u-runtime-config.sh "{{platform_root}}" "{{platform_base_url}}"

# Build and install the TG4U backend JARs, then rebuild the OSS service
# applications with those JARs as normal Maven dependencies.
tg4u-backend-build: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" build-signing-adapters
    scripts/build-tg4u-backend.sh "{{platform_root}}"

# Start the complete assembled local stack using the OSS justfile.
tg4u-dev *args: tg4u-setup tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" MAVEN_VERIFIER="{{verifier_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev {{args}}

# Public-repository-compatible entry points. These install the TG4U package
# first, then delegate to the exact OSS recipe so both repositories have the
# same local-development vocabulary.
setup: tg4u-setup

clean: tg4u-sync
    MAVEN_PLATFORM="{{platform_extension_maven}}" MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" MAVEN_VERIFIER="{{verifier_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" clean

dev *names: tg4u-setup tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" MAVEN_VERIFIER="{{verifier_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev {{names}}

dev-all *names: tg4u-setup tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" MAVEN_VERIFIER="{{verifier_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-all {{names}}

# Start only the assembled platform API.
tg4u-api: tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-api

dev-db: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-db

stop-db: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" stop-db

reset-db: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" reset-db

dev-api: tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-api

dev-issuer: tg4u-backend-build
    MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-issuer

dev-verifier: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-verifier

# Start only the assembled cockpit.
tg4u-web: tg4u-setup
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-web

dev-web skip="": tg4u-setup
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" dev-web {{skip}}

build-api: tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" build-api

build-issuer: tg4u-backend-build
    MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" build-issuer

build-verifier: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" build-verifier

build-web: tg4u-setup
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" build-web

test-api: tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" test-api

test-issuer: tg4u-backend-build
    MAVEN_ISSUER="{{tg4u_issuer_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" test-issuer

test-verifier: tg4u-sync
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" test-verifier

lint-web: tg4u-setup
    just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" lint-web

# Run the combined backend and frontend checks.
tg4u-test: tg4u-setup tg4u-backend-build
    MAVEN_PLATFORM="{{platform_extension_maven}}" just --justfile "{{platform_root}}/justfile" --working-directory "{{platform_root}}" test-api
    cd "{{platform_root}}/heidi-web" && {{pnpm}} run lint
    cd "{{platform_root}}/heidi-web" && {{pnpm}} exec vite build && {{pnpm}} exec tsc -b

# Remove only generated TG4U package artifacts from the generated OSS workspace.
tg4u-clean:
    scripts/reset-platform-workspace.sh "{{platform_root}}"
