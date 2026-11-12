@echo off
setlocal
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  call "%MAVEN_HOME%\bin\mvn.cmd" %*
  exit /b %ERRORLEVEL%
)
if exist "%ProgramFiles%\Apache\maven\bin\mvn.cmd" (
  call "%ProgramFiles%\Apache\maven\bin\mvn.cmd" %*
  exit /b %ERRORLEVEL%
)
echo Apache Maven was not found. Install Maven or set MAVEN_HOME to its installation directory.
exit /b 1
