@echo off
REM Run from the project folder:  tests\run_tests.bat
REM Needs a JDK (javac) on PATH. No database or internet needed.
if exist test-out rmdir /s /q test-out
mkdir test-out
javac -d test-out -cp lib\postgresql-42.7.14.jar src\*.java tests\*.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp test-out FilterExportTests
