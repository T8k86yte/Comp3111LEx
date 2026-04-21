@echo off
setlocal

set "INTELLIJ_MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"

if not exist "%INTELLIJ_MVN%" (
  echo IntelliJ bundled Maven not found:
  echo   %INTELLIJ_MVN%
  echo.
  echo Update mvn-intellij.cmd with your IntelliJ install path.
  exit /b 1
)

"%INTELLIJ_MVN%" %*

