@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17+ is required. Use the bundled Windows desktop image instead.
  pause
  exit /b 1
)
java -jar "%~dp0AbyssExpedition.jar" %*
if errorlevel 1 pause
