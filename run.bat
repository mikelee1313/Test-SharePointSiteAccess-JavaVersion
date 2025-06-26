@echo off
echo Starting SharePoint Access Test...
echo.

REM Set Java and Maven environment variables
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot"
set "PATH=%PATH%;C:\Tools\maven-mvnd\bin"

REM Check if config.properties exists
if not exist "src\main\resources\config.properties" (
    echo ERROR: Configuration file not found.
    echo.
    echo Please ensure config.properties exists in src\main\resources\
    echo Copy from config.properties.sample and update with your settings.
    echo.
    pause
    exit /b 1
)

echo Building and running SharePoint Access Test...
echo.

REM Build and run the application using Maven
mvnd clean compile exec:java "-Dexec.mainClass=com.microsoft.sharepoint.SharePointAccessTest"

echo.
echo Application finished. Press any key to close...
pause
