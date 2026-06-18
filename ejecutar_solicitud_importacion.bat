@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "JAVAC=C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot\bin\javac.exe"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot\bin\java.exe"

if not exist "%JAVAC%" (
    echo No se encontro el JDK. Instale Eclipse Temurin JDK 8 o ajuste la ruta en este archivo.
    pause
    exit /b 1
)

if not exist "out" mkdir out

echo Compilando...
"%JAVAC%" -encoding UTF-8 -cp "lib/*" -d out src\*.java
if errorlevel 1 (
    echo Error al compilar.
    pause
    exit /b 1
)

set "CODIGO=IMP-S1011"
if not "%~1"=="" set "CODIGO=%~1"

set "FORMATO=clasico"
if not "%~2"=="" set "FORMATO=%~2"

echo.
echo Generando PDF para: %CODIGO%
echo Formato: %FORMATO%
echo.
"%JAVA%" -cp "out;lib/*" PruebaSolicitudImportacion %CODIGO% %FORMATO%

echo.
pause
