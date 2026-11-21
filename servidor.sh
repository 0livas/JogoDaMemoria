#!/usr/bin/env sh
# Uso: ./servidor.sh [ip-do-servidor] [porta] [linhas] [colunas]
exec java -jar "$(dirname "$0")/dist/servidor.jar" "$@"
