@echo off
setlocal

rem -----------------------------------------------------------------------------
rem Gradle start up script for Windows
rem -----------------------------------------------------------------------------

if not "%DEBUG%" == "" echo Gradle debug is on

rem Resolve symbolic links - set APP_HOME
set APP_HOME=%~dp0
if "%APP_HOME:~-1%"=="\" set APP_HOME=%APP_HOME:~0,-1%

rem Setup default JVM if not provided
if "%JAVA_HOME%"=="" (
  set "JAVA_EXE=java"
) else (
  set "JAVA_EXE=%JAVA_HOME%\bin\java"
)

rem Default values for wrapper
set GRADLE_WRAPPER_JAR=gradle\wrapper\gradle-wrapper.jar
set GRADLE_WRAPPER_PROPERTIES=gradle\wrapper\gradle-wrapper.properties

rem Assemble the classpath for the wrapper
set CLASSPATH=%APP_HOME%\%GRADLE_WRAPPER_JAR%

rem Build the command line
set CMD_LINE_ARGS=
set APP_CLASSPATH=%CLASSPATH%
set QUOTED_APP_HOME="%APP_HOME%"

rem Forward all arguments
:parseArgs
if "%~1"=="" goto argsParsed
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto parseArgs
:argsParsed

rem Echo for debug
if not "%DEBUG%"=="" echo APP_HOME=%APP_HOME%
if not "%DEBUG%"=="" echo JAVA_EXE=%JAVA_EXE%
if not "%DEBUG%"=="" echo CLASSPATH=%CLASSPATH%

rem Run the wrapper
"%JAVA_EXE%" -cp "%APP_CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
set EXIT_CODE=%ERRORLEVEL%

endlocal & exit /b %EXIT_CODE%
