@echo off
cls
echo [1/3] Generando Lexer con JFlex...
java -jar lib/jflex-full-1.9.1.jar Lexer.flex
if %errorlevel% neq 0 exit /b %errorlevel%

echo [2/3] Compilando clases Java...
javac -encoding UTF-8 *.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo [3/3] Ejecutando analisis lexico...
echo.
java Main codigo.zefiro
pause