@echo off
REM Uso: servidor.bat [ip-do-servidor] [porta] [linhas] [colunas]
REM  ex.: servidor.bat 192.168.0.10 1099 4 4
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "%~dp0dist\servidor.jar" %*
pause
