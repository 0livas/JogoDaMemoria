@echo off
REM Compila o projeto e gera dist\servidor.jar e dist\cliente.jar (requer JDK 17+)
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out\classes
mkdir dist

echo Compilando...
javac --release 17 -encoding UTF-8 -d out\classes src\comum\*.java src\servidor\*.java src\cliente\*.java
if errorlevel 1 goto erro

xcopy /e /i /q /y recursos\imagens out\classes\imagens >nul

echo Empacotando...
jar --create --file dist\servidor.jar --main-class servidor.ServidorMain -C out\classes comum -C out\classes servidor
if errorlevel 1 goto erro
jar --create --file dist\cliente.jar --main-class cliente.ClienteMain -C out\classes comum -C out\classes cliente -C out\classes imagens
if errorlevel 1 goto erro

echo.
echo OK: dist\servidor.jar e dist\cliente.jar gerados.
exit /b 0

:erro
echo.
echo ERRO na compilacao/empacotamento.
exit /b 1
