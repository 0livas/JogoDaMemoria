#!/usr/bin/env sh
# Uso: ./cliente.sh [ip-do-servidor] [nome] [porta]
exec java -jar "$(dirname "$0")/dist/cliente.jar" "$@"
