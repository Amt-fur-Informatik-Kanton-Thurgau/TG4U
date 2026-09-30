#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "usage: $0 /absolute-or-relative/workspace /path/to/oss.lock [repository]" >&2
  exit 64
fi

tg4u_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
workspace_input=$1
lock_file=$2
repository_override=${3:-}

if [[ "$workspace_input" == /* ]]; then
  workspace=$workspace_input
else
  workspace="$tg4u_root/$workspace_input"
fi

if [[ "$lock_file" != /* ]]; then
  lock_file="$tg4u_root/$lock_file"
fi

if [[ ! -f "$lock_file" ]]; then
  echo "Heidi Platform lock file does not exist: $lock_file" >&2
  exit 1
fi

repository=${repository_override:-$(awk -F= '$1 == "repository" {print $2}' "$lock_file")}
commit=$(awk -F= '$1 == "commit" {print $2}' "$lock_file")

if [[ -z "$repository" || -z "$commit" ]]; then
  echo "Heidi Platform lock file must define repository and commit" >&2
  exit 1
fi
if [[ ! "$commit" =~ ^[0-9a-f]{40}$ ]]; then
  echo "OSS commit must be a 40-character SHA: $commit" >&2
  exit 1
fi

if [[ "$repository" != /* && "$repository" != *://* ]]; then
  repository="$tg4u_root/$repository"
fi

mkdir -p "$(dirname -- "$workspace")"

if [[ ! -d "$workspace/.git" ]]; then
  if [[ -e "$workspace" ]]; then
    echo "Refusing to use a non-generated OSS workspace: $workspace" >&2
    exit 1
  fi
  git clone --no-checkout "$repository" "$workspace"
fi

if [[ "$workspace" == "$tg4u_root" || "$workspace" == "/" ]]; then
  echo "Refusing an unsafe OSS workspace path: $workspace" >&2
  exit 1
fi

git -C "$workspace" remote set-url origin "$repository"
git -C "$workspace" fetch --depth=1 origin "$commit"

# The generated route tree is the only tracked file changed by the combined
# frontend build. Restore that exact generated file before changing the pin.
git -C "$workspace" restore --source=HEAD -- heidi-web/src/routeTree.gen.ts 2>/dev/null || true
rm -f "$workspace/heidi-web/src/routeTree.gen.ts"
# This checkout is generated and never holds source-of-truth changes. Discard
# tracked build output before moving between pinned OSS revisions so a failed
# previous assembly cannot mask files from the new revision.
git -C "$workspace" switch --discard-changes --detach "$commit"

# Maven target directories are ignored by Git and can survive a revision
# switch.  They may contain classes compiled against a different OSS
# contract, so clear generated build output before assembling TG4U sources.
find "$workspace" -type d -name target -prune -exec rm -rf -- {} +

git_dir=$(git -C "$workspace" rev-parse --git-dir)
git_marker="$git_dir/tg4u-platform-workspace"
if ! printf 'repository=%s\ncommit=%s\n' "$repository" "$commit" > "$git_marker" 2>/dev/null; then
  # Some local checkouts expose .git as read-only while still allowing
  # generated source and build artifacts to be refreshed.
  printf 'repository=%s\ncommit=%s\n' "$repository" "$commit" > "$workspace/.tg4u-workspace"
fi
echo "Prepared Heidi Platform workspace at $commit: $workspace"
