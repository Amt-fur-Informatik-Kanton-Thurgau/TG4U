#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 /absolute/path/to/heidi-platform" >&2
  exit 64
fi

tg4u_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
platform_root=$1

if [[ "$platform_root" == "/" || "$platform_root" == "$tg4u_root" ]]; then
  echo "Refusing an unsafe OSS checkout path: $platform_root" >&2
  exit 1
fi
if [[ ! -f "$platform_root/heidi-platform-api/pom.xml" ]]; then
  echo "OSS checkout does not contain the platform Maven reactor: $platform_root" >&2
  exit 1
fi
if [[ ! -f "$platform_root/heidi-issuer-backend/pom.xml" ]]; then
  echo "OSS checkout does not contain the issuer Maven reactor: $platform_root" >&2
  exit 1
fi

maven_args=()
if [[ -n "${MAVEN_ARGS:-}" ]]; then
  read -r -a maven_args <<< "$MAVEN_ARGS"
fi

if [[ ${#maven_args[@]} -gt 0 ]]; then
  mvn_cmd=(mvn "${maven_args[@]}" -B -ntp)
else
  mvn_cmd=(mvn -B -ntp)
fi
extension_group="${TG4U_EXTENSION_GROUP_ID:-ch.tg4u}"
platform_extension_artifact="${TG4U_PLATFORM_EXTENSION_ARTIFACT:-tg4u-platform-api-extension}"
issuer_extension_artifact="${TG4U_ISSUER_EXTENSION_ARTIFACT:-tg4u-issuer-extension}"
extension_version="${TG4U_EXTENSION_VERSION:-1.0.0-SNAPSHOT}"

echo "Installing the OSS platform reactor"
"${mvn_cmd[@]}" -f "$platform_root/heidi-platform-api/pom.xml" -DskipTests install

echo "Building the TG4U platform extension JAR"
"${mvn_cmd[@]}" \
  -f "$tg4u_root/heidi-platform-api-extensions/tg4u-platform-api-extension/pom.xml" \
  install

echo "Rebuilding the Heidi Platform API with the TG4U extension dependency"
"${mvn_cmd[@]}" \
  -f "$platform_root/heidi-platform-api/pom.xml" \
  -pl heidi-platform-api-ws -am \
  -Dheidi.extension.groupId="$extension_group" \
  -Dheidi.extension.artifactId="$platform_extension_artifact" \
  -Dheidi.extension.version="$extension_version" \
  -DskipTests install

echo "Installing the OSS issuer reactor"
"${mvn_cmd[@]}" -f "$platform_root/heidi-issuer-backend/pom.xml" -DskipTests install

echo "Building the TG4U issuer extension JAR"
"${mvn_cmd[@]}" \
  -f "$tg4u_root/heidi-issuer-backend-extensions/tg4u-issuer-extension/pom.xml" \
  -DskipTests install

echo "Rebuilding the Heidi issuer application with the TG4U extension dependency"
"${mvn_cmd[@]}" \
  -f "$platform_root/heidi-issuer-backend/pom.xml" \
  -pl heidi-issuer-ws -am \
  -Dheidi.extension.groupId="$extension_group" \
  -Dheidi.extension.artifactId="$issuer_extension_artifact" \
  -Dheidi.extension.version="$extension_version" \
  -DskipTests install

echo "Built TG4U platform and issuer extension artifacts"
