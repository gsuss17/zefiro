# Zefiro - Compilador e Intérprete

Proyecto de compilador e intérprete para el lenguaje de programación **Zefiro**, desarrollado para la materia de **Lenguajes y Autómatas**. Zefiro es un lenguaje estructurado con palabras reservadas en español inspirado en Java, diseñado para simplificar la lógica algorítmica mediante estructuras explícitas y legibles.

---

## Características del Lenguaje

* **Palabras reservadas en mayúsculas:** `INICIO`, `FIN`, `ENTERO`, `DECIMAL`, `CADENA`, `BOOLEANO`, `VACIO`, `SI`, `SINO`, `FINSI`, `MIENTRAS`, `FINMIENTRAS`, `PARA`, `FINPARA`, `ROMPER`, `CONTINUAR`, `FUNCION`, `FINFUNCION`, `RETORNAR`, `LEER`, `IMPRIMIR`.
* **Identificadores estrictos:** Deben iniciar con letra minúscula obligatoria (`[a-z][a-zA-Z0-9_]*`).
* **Delimitación de sentencias y bloques:** Cada sentencia concluye con punto y coma (`;`). Los bloques se delimitan explícitamente mediante palabras de cierre (`FINSI;`, `FINMIENTRAS;`, `FINPARA;`, `FINFUNCION;`, `FIN;`).
* **Manejo de tipos básicos:** `ENTERO`, `DECIMAL`, `CADENA`, `BOOLEANO` (`VERDADERO` / `FALSO`) y `VACIO` (para funciones sin retorno).
* **Operadores:** Aritméticos (`+`, `-`, `*`, `/`, `%`), concatenación de cadenas (`+`), relacionales (`==`, `!=`, `<`, `<=`, `>`, `>=`), lógicos (`Y`, `O`, `NO`) y menos unario (`-expresion`).
* **Comentarios:** De línea (`// ...`) y de bloque (`/* ... */`).
* **Fases completas:** Análisis Léxico (JFlex), Análisis Sintáctico y AST (CUP + Graphviz), Análisis Semántico con Tabla de Símbolos y ámbitos, e Intérprete de ejecución.

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
├── Nodo.java                     # Estructura del Árbol Sintáctico (AST) y exportación DOT
├── Simbolo.java                  # Entrada de la Tabla de Símbolos
├── TablaSimbolos.java            # Gestión de ámbitos y tabla de símbolos
├── AnalizadorSemantico.java      # Validación semántica y de tipos
├── Interprete.java               # Intérprete para ejecutar programas Zefiro
├── Main.java                     # Orquestador de fases y reporte
├── gramatica-bnf.txt             # Definición formal de la gramática en BNF
├── becas.zefiro                  # Programa de prueba interactivo
├── ejemplo_completo.zefiro       # Programa de prueba con todas las características
├── ejecutar.bat                  # Script de compilación y ejecución
└── README.md
```
