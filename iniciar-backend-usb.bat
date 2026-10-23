@echo off
setlocal
pushd "%~dp0"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo Nao encontrei o adb em: %ADB%
    popd
    exit /b 1
)

"%ADB%" reverse tcp:8081 tcp:8081
if errorlevel 1 (
    echo Falha no encaminhamento USB. Verifique se o telefone autorizou a depuracao USB.
    popd
    exit /b 1
)

call gradlew.bat -p server run
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%
