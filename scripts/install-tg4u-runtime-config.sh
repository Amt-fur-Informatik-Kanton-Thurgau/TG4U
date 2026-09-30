#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 /path/to/generated/oss/workspace platform-url" >&2
  exit 64
fi

tg4u_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
workspace=$1
platform_base_url=$2
template="$tg4u_root/local-runtime-config.json"
destination="$workspace/heidi-web/public/config.json"

if [[ ! -f "$template" || ! -d "$workspace/heidi-web/public" ]]; then
  echo "Cannot install local runtime config into: $workspace" >&2
  exit 1
fi

sed \
  -e "s|__PLATFORM_BASE_URL__|$platform_base_url|g" \
  "$template" > "$destination"

echo "Installed local TG4U runtime config: $destination"
