# Suite de Pruebas de Zefiro

Esta carpeta reúne programas de ejemplo diseñados para comprobar que cada fase del compilador e intérprete detecte los errores esperados y ejecute correctamente los programas válidos.

> Si buscas plantillas de código para escribir tus propios programas, consulta **[../SINTAXIS.md](../SINTAXIS.md)**.

---

## Archivos incluidos

| Archivo | Qué prueba | Qué debes observar al ejecutarlo |
| :--- | :--- | :--- |
| `01_errores_lexicos.zefiro` | **Fase Léxica** | Reporta 8 errores léxicos (identificadores en mayúscula, decimales como `3.` y `.5`, símbolos `@` y `$`, cadena sin cerrar y comentario `/*` abierto) y omite la fase sintáctica. |
| `02_errores_sintacticos.zefiro` | **Fase Sintáctica** | Pasa la fase léxica y reporta 5 errores sintácticos (falta de `;`, declaración/asignación incompleta, paréntesis sin cerrar y `PARA` mal formado), recuperándose hasta el final. |
| `03_errores_semanticos.zefiro` | **Fase Semántica** | Pasa léxico y sintaxis, y reporta errores de tipos, redeclaración/sombreado estilo Java, variable sin inicializar, `ROMPER`/`CONTINUAR`/`RETORNAR` fuera de lugar y llamadas inválidas a funciones. |
| `04_errores_ejecucion.zefiro` | **Intérprete** | Compila sin errores, inicia la ejecución y se detiene de forma controlada al intentar dividir entre cero. |
| `05_exito_funciones_y_alcance.zefiro` | **Éxito (Funciones)** | Ejecuta recursión (`fibonacci`), promoción `ENTERO` a `DECIMAL`, retorno temprano en función `VACIO` y ámbitos independientes entre bloques hermanos. |
| `06_exito_completo.zefiro` | **Éxito (General)** | Ejecuta todos los tipos, operadores, secuencias de escape en cadenas, bloques vacíos, `SI`/`SINO`, `PARA`, `MIENTRAS`, `ROMPER` y `CONTINUAR`. |

---

## Cómo ejecutar las pruebas

Desde la carpeta raíz del proyecto (`zefiro/`), ejecuta cualquiera de los archivos con `ejecutar.bat`:

```bat
ejecutar.bat pruebas\01_errores_lexicos.zefiro
ejecutar.bat pruebas\06_exito_completo.zefiro
```

Si ya compilaste las clases Java y solo quieres correr una prueba rápidamente sin regenerar el lexer y el parser:

```bat
java -Dfile.encoding=UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" Main pruebas\03_errores_semanticos.zefiro
```
