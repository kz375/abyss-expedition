#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
java tools/Web.java
