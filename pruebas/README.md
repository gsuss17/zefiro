# Suite de Pruebas de Zefiro

Esta carpeta contiene programas de prueba para verificar el blindaje de cada fase del compilador e intérprete.

## Archivos de Prueba

| Archivo | Fase que prueba | Resultado esperado |
| :--- | :--- | :--- |
| `01_errores_lexicos.zefiro` | **Léxica** | Reporta identificadores en mayúscula, decimales incompletos, caracteres inválidos, cadena sin cerrar y comentario de bloque sin cerrar. |
| `02_errores_sintacticos.zefiro` | **Sintáctica** | Reporta falta de `;`, declaraciones/asignaciones incompletas, paréntesis desbalanceados y `PARA` mal formado, recuperándose hasta el final. |
| `03_errores_semanticos.zefiro` | **Semántica** | Compila léxica y sintácticamente, y reporta errores de tipos, redeclaración, variables no declaradas, condiciones no booleanas, `ROMPER`/`CONTINUAR`/`RETORNAR` fuera de contexto y llamadas inválidas a funciones. |
| `04_errores_ejecucion.zefiro` | **Intérprete** | Compila sin errores, pero detiene la ejecución limpiamente al detectar división entre cero. |
| `05_exito_funciones_y_alcance.zefiro` | **Todas (Éxito)** | Ejecuta recursión (`fibonacci`), múltiples tipos de parámetros, retorno temprano en `VACIO` y ámbitos locales independientes. |
| `06_exito_completo.zefiro` | **Todas (Éxito)** | Ejecuta todos los tipos, operadores, secuencias de escape, bloques vacíos, `SI`/`SINO`, `PARA`, `MIENTRAS`, `ROMPER` y `CONTINUAR`. |

## Cómo ejecutar una prueba

Desde la raíz del proyecto:

```bat
ejecutar.bat pruebas\01_errores_lexicos.zefiro
```

O directamente con Java (si ya compilaste):

```bat
java -Dfile.encoding=UTF-8 -cp ".;lib/java-cup-11b-runtime.jar" Main pruebas\06_exito_completo.zefiro
```
