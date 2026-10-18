# Zefiro - Analizador Léxico y Sintáctico

Proyecto de compilador para el lenguaje de programación **Zefiro**, desarrollado para la materia de **Lenguajes y Autómatas**. Zefiro es un lenguaje estructurado con palabras reservadas en español inspirado en Java, diseñado para simplificar la lógica algorítmica mediante estructuras explícitas y legibles.

---

## Características del Lenguaje

* **Palabras reservadas en mayúsculas:** `INICIO`, `FIN`, `ENTERO`, `DECIMAL`, `CADENA`, `BOOLEANO`, `SI`, `SINO`, `FINSI`, `MIENTRAS`, `FINMIENTRAS`, `LEER`, `IMPRIMIR`.
* **Identificadores estrictos:** Deben iniciar con letra minúscula obligatoria (`[a-z][a-zA-Z0-9_]*`).
* **Delimitación de sentencias y bloques:** Cada sentencia concluye con punto y coma (`;`). Los bloques no utilizan llaves ni indentación obligatoria; se delimitan explícitamente mediante palabras de cierre (`FINSI;`, `FINMIENTRAS;`, `FIN;`).
* **Manejo de tipos básicos:** `ENTERO`, `DECIMAL`, `CADENA` y `BOOLEANO` (`VERDADERO` / `FALSO`).
* **Soporte de operadores:** Aritméticos (`+`, `-`, `*`, `/`), relacionales (`==`, `!=`, `<`, `<=`, `>`, `>=`) y lógicos (`Y`, `O`, `NO`), además del menos unario (`-expresion`).

---

## Estructura del Proyecto

```text
zefiro/
├── lib/
│   ├── java-cup-11b.jar          # Generador de analizadores sintácticos CUP
│   ├── java-cup-11b-runtime.jar  # Biblioteca en tiempo de ejecución para CUP
│   └── jflex-full-1.9.1.jar      # Generador de analizadores léxicos JFlex
├── Lexer.flex                    # Especificación léxica para JFlex
├── Parser.cup                    # Especificación gramatical (LALR) y precedencias para CUP
├── gramatica-bnf.txt             # Definición formal de la gramática en BNF
├── Main.java                     # Punto de entrada (orquestador de fases y reporte)
├── becas.zefiro                  # Programa de prueba escrito en Zefiro
├── ejecutar.bat                  # Script de automatización de compilación y pruebas
└── README.md