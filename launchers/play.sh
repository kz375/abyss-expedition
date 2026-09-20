#!/bin/sh
set -eu
app_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if ! command -v java >/dev/null 2>&1; then
    echo "Java 17+ is required for the JAR edition. Use a bundled desktop image instead."
    exit 1
fi
cd "$app_dir"
exec java -jar "$app_dir/AbyssExpedition.jar" "$@"
