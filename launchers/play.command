#!/bin/sh
set -eu
app_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$app_dir"
if ! command -v java >/dev/null 2>&1; then
    echo "Java 17+ is required. Use the bundled macOS app instead."
    read -r answer
    exit 1
fi
java -jar "$app_dir/AbyssExpedition.jar" "$@" || {
    echo "Unable to start. Install Java 17+ or use the bundled app. Press Enter."
    read -r answer
}
