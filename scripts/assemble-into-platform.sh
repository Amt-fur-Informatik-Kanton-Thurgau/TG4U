#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 /absolute/path/to/heidi-platform" >&2
  exit 64
fi

tg4u_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
platform_root=$1
web_source_root="$tg4u_root/heidi-web-extensions"
web_root="$platform_root/heidi-web"
extension_package="@tg4u/tg4u-web-extension"
extension_store="$web_root/.tg4u"
extension_archive="$extension_store/tg4u-web-extension.tgz"
extension_marker="$web_root/.tg4u-extension-package"
extension_selection="$web_root/.heidi-extension-package"
extension_public_manifest="$web_root/.tg4u-extension-public-files"
pnpm_cmd="${PNPM:-pnpm}"

if [[ ! -d "$platform_root/heidi-web" ]]; then
  echo "OSS checkout does not contain heidi-web: $platform_root" >&2
  exit 1
fi
if [[ "$platform_root" == "/" || "$platform_root" == "$tg4u_root" ]]; then
  echo "Refusing an unsafe OSS checkout path: $platform_root" >&2
  exit 1
fi
if [[ -e "$extension_marker" ]]; then
  echo "A TG4U web extension is already installed; reset the platform workspace first: $platform_root" >&2
  exit 1
fi
if [[ -e "$extension_selection" ]]; then
  echo "A web extension package is already selected; reset the platform workspace first: $platform_root" >&2
  exit 1
fi

package_artifacts=$(mktemp -d)
trap 'rm -rf "$package_artifacts"' EXIT

echo "Packing TG4U web extension"
(
  cd "$web_source_root"
  "$pnpm_cmd" pack --pack-destination "$package_artifacts"
)

package_archive=$(find "$package_artifacts" -maxdepth 1 -type f -name '*.tgz' -print -quit)
if [[ -z "$package_archive" ]]; then
  echo "pnpm pack did not produce a web extension archive" >&2
  exit 1
fi

mkdir -p "$extension_store"
cp "$package_archive" "$extension_archive"
(
  cd "$web_root"
  "$pnpm_cmd" add \
    --force \
    --save-dev \
    --ignore-workspace-root-check \
    --allow-build=@biomejs/biome \
    --allow-build=@tailwindcss/oxide \
    --allow-build=esbuild \
    --allow-build=sharp \
    "./.tg4u/tg4u-web-extension.tgz"
)
printf '%s\n' "$extension_package" > "$extension_selection"
printf '%s\n' "$extension_package" > "$extension_marker"

package_destination="$web_root/node_modules/$extension_package"
if [[ -d "$package_destination/public" ]]; then
  mkdir -p "$web_root/public"
  : > "$extension_public_manifest"
  while IFS= read -r -d '' source; do
    relative_path="${source#"$package_destination/public/"}"
    destination="$web_root/public/$relative_path"
    if [[ ! -e "$destination" ]]; then
      mkdir -p "$(dirname -- "$destination")"
      cp "$source" "$destination"
      printf '%s\n' "$relative_path" >> "$extension_public_manifest"
    fi
  done < <(find "$package_destination/public" -type f -print0)
fi

echo "Assembled TG4U web extension package into $platform_root"
echo "Build backend extensions with:"
echo "  scripts/build-tg4u-backend.sh $platform_root"
