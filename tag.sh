#!/bin/bash
set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Uso: ./tag.sh <version>"
  echo "Ejemplo: ./tag.sh 1.0.0"
  exit 1
fi

TAG="$1"

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Error: el tag $TAG ya existe"
  exit 1
fi

git tag -a "$TAG" -m "Release $TAG"
git push origin "$TAG"

echo "Tag $TAG creado y pusheado"
