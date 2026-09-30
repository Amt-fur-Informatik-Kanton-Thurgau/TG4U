#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 /path/to/generated/oss/workspace" >&2
  exit 64
fi

tg4u_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
workspace_input=$1
if [[ "$workspace_input" == /* ]]; then
  workspace=$workspace_input
else
  workspace="$tg4u_root/$workspace_input"
fi

if [[ "$workspace" == "$tg4u_root" || "$workspace" == "/" || ! -d "$workspace/.git" ]]; then
  echo "Refusing an unsafe or non-git OSS workspace: $workspace" >&2
  exit 1
fi

git_dir=$(git -C "$workspace" rev-parse --git-dir)
if [[ ! -f "$workspace/.tg4u-workspace" && ! -f "$git_dir/tg4u-platform-workspace" ]]; then
  echo "Refusing to clean a workspace not created by prepare-platform-workspace.sh: $workspace" >&2
  exit 1
fi

# The package install and generated route tree are build artifacts in the
# generated checkout. Restore only those tracked files before the next build.
git -C "$workspace" restore --source=HEAD -- \
  heidi-web/src/routeTree.gen.ts \
  heidi-web/package.json \
  heidi-web/pnpm-lock.yaml \
  heidi-web/pnpm-workspace.yaml 2>/dev/null || true

public_manifest="$workspace/heidi-web/.tg4u-extension-public-files"
if [[ -f "$public_manifest" ]]; then
  while IFS= read -r relative_path; do
    [[ -n "$relative_path" ]] || continue
    rm -f "$workspace/heidi-web/public/$relative_path"
  done < "$public_manifest"
fi

rm -f "$workspace/heidi-web/.tg4u-extension-package"
rm -f "$workspace/heidi-web/.heidi-extension-package"
rm -f "$workspace/heidi-web/.tg4u-extension-public-files"
rm -f "$workspace/heidi-web/.tg4u/tg4u-web-extension.tgz"
rm -rf "$workspace/heidi-web/.tg4u/extensions"
if [[ -d "$workspace/heidi-web/.tg4u" ]] && [[ -z "$(find "$workspace/heidi-web/.tg4u" -mindepth 1 -print -quit)" ]]; then
  rmdir "$workspace/heidi-web/.tg4u"
fi
