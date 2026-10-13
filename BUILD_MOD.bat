@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title Build Mob Spawning Real - NeoForge 26.2

echo ============================================================
echo  Mob Spawning Real - NeoForge 26.2 Builder
echo ============================================================
echo.
echo This builds the universal mob-control mod automatically.
echo The first build can take several minutes while Gradle and NeoForge
echo dependencies are downloaded.
echo.

set "JDK_DIR=%CD%\.build-jdk25"
set "JDK_ZIP=%TEMP%\mobspawningreal-jdk25.zip"

if exist "%JDK_DIR%\bin\java.exe" goto HAVE_JAVA

echo [1/3] Downloading a private Java 25 build for this project...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '%JDK_ZIP%'"
if errorlevel 1 goto DOWNLOAD_FAIL

if exist "%JDK_DIR%" rmdir /s /q "%JDK_DIR%"
mkdir "%JDK_DIR%"
echo Extracting Java 25...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%JDK_ZIP%' -DestinationPath '%JDK_DIR%\extract' -Force; $d = Get-ChildItem '%JDK_DIR%\extract' -Directory | Select-Object -First 1; Move-Item -Path ($d.FullName + '\*') -Destination '%JDK_DIR%' -Force; Remove-Item '%JDK_DIR%\extract' -Recurse -Force"
if errorlevel 1 goto DOWNLOAD_FAIL
del /q "%JDK_ZIP%" >nul 2>nul

:HAVE_JAVA
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [2/3] Running tests and building the mod...
call gradlew.bat clean test build
if errorlevel 1 goto BUILD_FAIL

echo [3/3] Finding finished jar...
set "BUILT_JAR="
for %%F in (build\libs\*.jar) do (
    echo %%~nxF | findstr /i /v "sources dev shadow" >nul
    if not errorlevel 1 set "BUILT_JAR=%%F"
)
if not defined BUILT_JAR goto JAR_FAIL

copy /y "!BUILT_JAR!" "%CD%\MobSpawningReal-neoforge-26.2.jar" >nul

echo.
echo ============================================================
echo  SUCCESS!
echo ============================================================
echo.
echo Finished jar:
echo %CD%\MobSpawningReal-neoforge-26.2.jar
echo.
echo Put that jar in the dedicated server's mods folder.
echo For multiplayer, keep the same jar on clients too unless testing

echo confirms your setup only needs it server-side.
echo.
echo Rules are created after first launch under:
echo config\mobspawningreal\rules
echo.
pause
exit /b 0

:DOWNLOAD_FAIL
echo.
echo ERROR: Could not download/extract Java 25.
echo Check your internet connection and try again.
pause
exit /b 1

:BUILD_FAIL
echo.
echo ERROR: Gradle could not compile/test the mod.
echo Leave this window open and send the error text to ChatGPT.
pause
exit /b 1

:JAR_FAIL
echo.
echo ERROR: Build completed but the finished jar could not be found.
pause
exit /b 1
