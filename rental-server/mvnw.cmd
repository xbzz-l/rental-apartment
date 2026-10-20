@echo off
rem ============================================
rem  Bundled Maven launcher (no system Maven needed)
rem  Dependencies download via Aliyun mirror (.mvn/settings.xml)
rem ============================================
set "MAVEN_HOME=%~dp0.mvn\apache-maven-3.9.6"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [ERROR] Bundled Maven not found: %MAVEN_HOME%
    echo         Please make sure rental-server\.mvn\apache-maven-3.9.6 exists.
    pause
    exit /b 1
)

"%MAVEN_HOME%\bin\mvn.cmd" -s "%~dp0.mvn\settings.xml" %*
