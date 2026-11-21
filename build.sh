#!/usr/bin/env sh
# Compila o projeto e gera dist/servidor.jar e dist/cliente.jar (requer JDK 17+)
set -e
cd "$(dirname "$0")"
rm -rf out dist
mkdir -p out/classes dist

echo "Compilando..."
javac --release 17 -encoding UTF-8 -d out/classes src/comum/*.java src/servidor/*.java src/cliente/*.java
cp -r recursos/imagens out/classes/imagens

echo "Empacotando..."
jar --create --file dist/servidor.jar --main-class servidor.ServidorMain -C out/classes comum -C out/classes servidor
jar --create --file dist/cliente.jar --main-class cliente.ClienteMain -C out/classes comum -C out/classes cliente -C out/classes imagens

echo "OK: dist/servidor.jar e dist/cliente.jar gerados."
