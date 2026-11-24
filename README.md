# Zefiro — Compilador e Intérprete

**Zefiro** es un lenguaje de programación estructurado con palabras reservadas en español e inspirado en las reglas de tipos y ámbitos de **Java**, desarrollado para la materia de **Lenguajes y Autómatas**.

---

## Requisitos

- **Java JDK** (8 o superior).
- **Graphviz** *(opcional)*: si está instalado, `ejecutar.bat` genera automáticamente las imágenes `arbol_ast.png` y `arbol_ast.svg` del árbol sintáctico.

---

## Cómo ejecutar un programa

### Opción 1: Usando el script automático (Windows)

```bat
ejecutar.bat
```

Por defecto compila el proyecto y ejecuta `becas.zefiro`. También puedes pasar cualquier otro archivo `.zefiro`:

```bat
ejecutar.bat ejemplo_completo.zefiro
ejecutar.bat pruebas\06_exito_completo.zefiro
```

### Opción 2: Comandos manuales

```bat
java -jar lib/java-cup-11b.jar -parser parser -symbols sym Parser.cup
java -jar lib/jflex-full-1.9.1.jar Lexer.flex
javac -encoding UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" *.java
java -Dfile.encoding=UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" Main becas.zefiro
```

> Si solo deseas validar el código sin ejecutarlo, agrega la bandera `--solo-compilar` al final del comando `java`.

---

## Fases del compilador

Al analizar un archivo `.zefiro`, `Main` ejecuta 5 fases en orden:

1. **Análisis Léxico (`Lexer.flex`):** Convierte el texto fuente en tokens y detecta caracteres inválidos, identificadores mal escritos, decimales incompletos y cadenas o comentarios sin cerrar. Si hay errores léxicos, se detiene aquí.
2. **Análisis Sintáctico (`Parser.cup`):** Verifica la estructura gramatical (LALR) y construye el Árbol de Sintaxis Abstracta (AST).
3. **Análisis Semántico (`AnalizadorSemantico.java`):** Construye la Tabla de Símbolos y valida ámbitos (estilo Java, sin sombreado en bloques), inicialización de variables, tipos en expresiones, condiciones booleanas, `ROMPER`/`CONTINUAR` dentro de ciclos y `RETORNAR` en funciones.
4. **Exportación del AST (`Nodo.java`):** Imprime el árbol en consola y genera `arbol_ast.dot` para visualizarlo con Graphviz.
5. **Ejecución (`Interprete.java`):** Si no hubo errores en las fases anteriores, ejecuta el programa en consola.

---

## Guía rápida del lenguaje

> Para ver plantillas de código listas para copiar de cada estructura, consulta **[SINTAXIS.md](SINTAXIS.md)**.

- **Estructura de un programa:** Todo programa inicia con `INICIO nombre;` y termina con `FIN;`.
- **Identificadores:** Deben iniciar con letra minúscula (`[a-z][a-zA-Z0-9_]*`). Las palabras reservadas van en **MAYÚSCULAS**.
- **Tipos de datos:** `ENTERO`, `DECIMAL`, `CADENA`, `BOOLEANO` (`VERDADERO` / `FALSO`) y `VACIO` (solo para retorno de funciones).
- **Operadores:**
  - Aritméticos: `+`, `-`, `*`, `/`, `%`, menos unario (`-x`). El operador `+` también concatena texto con cualquier valor.
  - Relacionales: `==`, `!=`, `<`, `<=`, `>`, `>=`.
  - Lógicos (con cortocircuito): `Y`, `O`, `NO`.
- **Control de flujo:** `SI (...) ... SINO ... FINSI;`, `MIENTRAS (...) ... FINMIENTRAS;`, `PARA (init; cond; paso) ... FINPARA;`, `ROMPER;` y `CONTINUAR;`.
- **Funciones:** `FUNCION <tipo|VACIO> nombre(params) ... RETORNAR ...; FINFUNCION;`.
- **Entrada y salida:** `LEER(variable);` e `IMPRIMIR(expresion);`.
- **Comentarios:** De una línea (`// ...`) y de bloque (`/* ... */`).

---

## Estructura del proyecto

```text
zefiro/
├── lib/                        # JFlex y Java CUP
├── pruebas/                    # Suite de pruebas para cada fase (ver pruebas/README.md)
├── Lexer.flex                  # Especificación léxica (JFlex)
├── Parser.cup                  # Gramática LALR y construcción del AST (CUP)
├── Nodo.java                   # Nodo del AST y exportación a Graphviz (.dot)
├── Simbolo.java                # Registro de la Tabla de Símbolos
├── TablaSimbolos.java          # Manejo de ámbitos anidados
├── AnalizadorSemantico.java    # Validación semántica y de tipos
├── Interprete.java             # Intérprete del lenguaje
├── Main.java                   # Orquestador de las 5 fases
├── gramatica-bnf.txt           # Gramática formal en notación BNF
├── tokens.txt                  # Lista oficial de todos los tokens del lenguaje
├── SINTAXIS.md                 # Guía de sintaxis y plantillas de código
├── becas.zefiro                # Programa interactivo de ejemplo
├── ejemplo_completo.zefiro     # Ejemplo con todas las características del lenguaje
├── ejecutar.bat                # Script de compilación y ejecución en Windows
└── README.md
```
