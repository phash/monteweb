@REM Maven Wrapper script for Windows
@REM Downloads and runs Maven if not already available

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

for /f "tokens=1,* delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set "MAVEN_DIST_URL=%%b"

set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_DIST_URL%' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%TEMP%\maven.zip" 2>nul
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
