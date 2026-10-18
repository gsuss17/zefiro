@echo off
cls

echo Limpiando compilaciones anteriores...
del /Q Lexer.java parser.java sym.java *.class 2>nul

echo [1/4] Generando Parser y Tabla de Simbolos con CUP...
java -jar lib/java-cup-11b.jar -parser parser -symbols sym Parser.cup
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al generar el parser con CUP.
    pause
    exit /b %errorlevel%
)

echo [2/4] Generando Lexer con JFlex...
java -jar lib/jflex-full-1.9.1.jar Lexer.flex
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al generar el lexer con JFlex.
    pause
    exit /b %errorlevel%
)

echo [3/4] Compilando clases Java...
javac -encoding UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" *.java
if %errorlevel% neq 0 (
    echo [ERROR] Fallo en la compilacion de Java.
    pause
    exit /b %errorlevel%
)

echo [4/4] Ejecutando analisis lexico y sintactico...
echo.
java -cp ".;lib/java-cup-11b-runtime.jar" Main codigo.zefiro
echo.
pause