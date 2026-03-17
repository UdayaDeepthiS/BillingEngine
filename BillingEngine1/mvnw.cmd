@REM Maven Wrapper script for Windows
@echo off
setlocal

set SCRIPT_DIR=%~dp0
set WRAPPER_JAR=%SCRIPT_DIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%SCRIPT_DIR%.mvn\wrapper\maven-wrapper.properties

if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_CMD=java.exe
)

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    for /f "tokens=2 delims==" %%a in ('findstr /i wrapperUrl "%WRAPPER_PROPERTIES%"') do set WRAPPER_URL=%%a
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

"%JAVA_CMD%" ^
    -classpath "%WRAPPER_JAR%" ^
    "-Dmaven.multiModuleProjectDirectory=%SCRIPT_DIR%" ^
    org.apache.maven.wrapper.MavenWrapperMain ^
    %*
endlocal
