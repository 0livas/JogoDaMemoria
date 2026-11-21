@echo off
REM Uso: cliente.bat [ip-do-servidor] [nome] [porta]
REM  Sem argumentos abre uma janela pedindo os dados.
start "" javaw -jar "%~dp0dist\cliente.jar" %*
