@echo off
cls

echo Limpiando compilaciones anteriores...
del /Q Lexer.java parser.java sym.java *.class arbol_ast.dot arbol_ast.png arbol_ast.svg 2>nul

echo [1/5] Generando Parser y Tabla de Simbolos con CUP...
java -jar lib/java-cup-11b.jar -parser parser -symbols sym Parser.cup
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al generar el parser con CUP.
    pause
    exit /b %errorlevel%
)

echo [2/5] Generando Lexer con JFlex...
java -jar lib/jflex-full-1.9.1.jar Lexer.flex
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al generar el lexer con JFlex.
    pause
    exit /b %errorlevel%
)

echo [3/5] Compilando clases Java...
javac -encoding UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" *.java
if %errorlevel% neq 0 (
    echo [ERROR] Fallo en la compilacion de Java.
    pause
    exit /b %errorlevel%
)

echo [4/5] Ejecutando analisis lexico y sintactico...
echo.
java -cp ".;lib/java-cup-11b-runtime.jar" Main becas.zefiro
echo.

if exist arbol_ast.dot (
    echo [5/5] Generando imagen del Arbol Sintactico con Graphviz...
    where dot >nul 2>nul
    if %errorlevel% equ 0 (
        dot -Tpng arbol_ast.dot -o arbol_ast.png
        dot -Tsvg arbol_ast.dot -o arbol_ast.svg
        echo [EXITO] Imagenes generadas: arbol_ast.png y arbol_ast.svg
        start "" arbol_ast.png
    ) else (
        if exist "C:\Program Files\Graphviz\bin\dot.exe" (
            "C:\Program Files\Graphviz\bin\dot.exe" -Tpng arbol_ast.dot -o arbol_ast.png
            "C:\Program Files\Graphviz\bin\dot.exe" -Tsvg arbol_ast.dot -o arbol_ast.svg
            echo [EXITO] Imagenes generadas: arbol_ast.png y arbol_ast.svg
            start "" arbol_ast.png
        ) else (
            echo [AVISO] Graphviz no esta instalado o no esta en el PATH.
            echo El archivo arbol_ast.dot fue creado; puedes visualizarlo en https://dreampuf.github.io/GraphvizOnline/
        )
    )
)

pause