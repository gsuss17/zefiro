# Guía de Sintaxis y Plantillas de Zefiro

Referencia rápida con plantillas listas para copiar y usar cada característica del lenguaje **Zefiro**.

---

## 1. Estructura mínima de un programa

Todo programa comienza con `INICIO <nombre>;` y termina con `FIN;`. El nombre del programa debe iniciar con letra minúscula.

```zefiro
INICIO miPrograma;

    // Tus funciones y sentencias van aquí

FIN;
```

---

## 2. Comentarios

```zefiro
// Comentario de una sola línea

/*
   Comentario de varias líneas
   (comentario de bloque)
*/
```

---

## 3. Variables y tipos de datos

Los identificadores **deben iniciar con letra minúscula** (`[a-z][a-zA-Z0-9_]*`).  
Las palabras reservadas siempre van en **MAYÚSCULAS**.

```zefiro
// Declaración con inicialización
ENTERO edad = 20;
DECIMAL promedio = 9.5;
CADENA nombre = "Ana";
BOOLEANO activo = VERDADERO;   // VERDADERO o FALSO

// Declaración múltiple (con o sin valor inicial)
ENTERO x, y = 10, z = 20;

// Asignación posterior
x = y + z;
```

> **Regla estilo Java:** Si declaras una variable sin valor inicial (como `ENTERO x;`), debes asignarle un valor o leerla con `LEER(x);` antes de usarla en una expresión o en `IMPRIMIR(x);`.

---

## 4. Operadores

```zefiro
// Aritméticos (+, -, *, /, %, menos unario)
ENTERO suma = 10 + 5;
ENTERO resta = 10 - 5;
ENTERO mult = 10 * 5;
ENTERO divEntera = 10 / 3;       // Resultado: 3
DECIMAL divDecimal = 10 / 3.0;   // Resultado: 3.3333...
ENTERO modEntero = 10 % 3;       // Resultado: 1
DECIMAL modDecimal = 10.5 % 2;   // Resultado: 0.5
ENTERO negativo = -suma;

// Concatenación de cadenas con +
CADENA saludo = "Hola " + nombre + ", edad: " + edad;

// Relacionales (==, !=, <, <=, >, >=) -> devuelven BOOLEANO
BOOLEANO esMayor = edad >= 18;
BOOLEANO esDistinto = edad != 0;

// Lógicos (Y, O, NO) con evaluación en cortocircuito
BOOLEANO cumple = (edad >= 18) Y (promedio >= 8.0);
BOOLEANO alerta = NO activo O (edad < 18);
```

---

## 5. Entrada y salida (`LEER` e `IMPRIMIR`)

```zefiro
ENTERO numero = 0;

IMPRIMIR("Ingresa un número:");
LEER(numero);
IMPRIMIR("Ingresaste: " + numero);
```

---

## 6. Condicionales (`SI` / `SINO` / `FINSI;`)

La condición dentro de `SI (...)` debe ser de tipo `BOOLEANO`.

```zefiro
// Condicional simple
SI (edad >= 18)
    IMPRIMIR("Es mayor de edad");
FINSI;

// Condicional con SINO
SI (promedio >= 9.0)
    IMPRIMIR("Excelente");
SINO
    IMPRIMIR("Puede mejorar");
FINSI;
```

---

## 7. Ciclos (`MIENTRAS`, `PARA`, `ROMPER`, `CONTINUAR`)

### Ciclo `MIENTRAS`

```zefiro
ENTERO i = 0;
MIENTRAS (i < 5)
    IMPRIMIR(i);
    i = i + 1;
FINMIENTRAS;
```

### Ciclo `PARA`

```zefiro
PARA (ENTERO j = 0; j < 5; j = j + 1)
    IMPRIMIR("Iteración: " + j);
FINPARA;
```

### Control de flujo (`ROMPER;` y `CONTINUAR;`)

```zefiro
PARA (ENTERO k = 1; k <= 10; k = k + 1)
    SI (k % 2 != 0)
        CONTINUAR;   // Salta a la siguiente iteración
    FINSI;
    SI (k > 6)
        ROMPER;      // Sale del ciclo inmediatamente
    FINSI;
    IMPRIMIR(k);
FINPARA;
```

---

## 8. Funciones (`FUNCION` / `RETORNAR` / `FINFUNCION;`)

Las funciones se declaran al inicio del programa (justo después de `INICIO nombre;`) y pueden llamarse entre sí o de forma recursiva.

### Función con valor de retorno

```zefiro
FUNCION ENTERO sumar(ENTERO a, ENTERO b)
    RETORNAR a + b;
FINFUNCION;

// Uso en el programa principal:
ENTERO total = sumar(4, 6);
IMPRIMIR("Total: " + total);
```

### Función sin valor de retorno (`VACIO`)

```zefiro
FUNCION VACIO saludar(CADENA persona)
    IMPRIMIR("Hola, " + persona + "!");
FINFUNCION;

// Llamada como sentencia:
saludar("Zefiro");
```

---

## 9. Reglas importantes (estilo Java)

1. **Sin sombreado en bloques:** No se puede volver a declarar una variable dentro de un `SI`, `MIENTRAS` o `PARA` si ya existe con el mismo nombre en un ámbito contenedor.
2. **Retorno obligatorio:** Toda función cuyo tipo no sea `VACIO` debe garantizar un `RETORNAR` en todos sus caminos (por ejemplo, tanto en `SI` como en `SINO`).
3. **Promoción numérica:** Un `ENTERO` puede asignarse o pasarse donde se espera un `DECIMAL`, pero no al revés.
