import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * Intérprete de recorrido sobre el AST para el lenguaje Zefiro.
 * Ejecuta funciones, ámbitos léxicos, ciclos, condicionales y E/S en consola.
 */
public class Interprete {
    // Señales internas de control de flujo para desenrollar bloques sin acoplar cada nodo
    private static class SenalRomper extends RuntimeException {}
    private static class SenalContinuar extends RuntimeException {}
    private static class SenalRetornar extends RuntimeException {
        final Object valor;
        SenalRetornar(Object valor) { this.valor = valor; }
    }

    public static class ErrorEjecucion extends RuntimeException {
        ErrorEjecucion(String msg) { super(msg); }
    }

    private static class Ambito {
        final Map<String, Object> valores = new LinkedHashMap<>();
        final Map<String, String> tipos = new LinkedHashMap<>();
    }

    private final Map<String, Nodo> funciones = new LinkedHashMap<>();
    private Deque<Ambito> pilaAmbitos = new ArrayDeque<>();
    private final Scanner scanner;

    public Interprete() {
        this.scanner = new Scanner(System.in, "UTF-8");
    }

    public void ejecutar(Nodo raiz) {
        if (raiz == null) return;

        Nodo listaFunciones = raiz.buscarHijo("LISTA_FUNCIONES");
        if (listaFunciones != null) {
            for (Nodo fn : listaFunciones.buscarHijos("FUNCION")) {
                Nodo id = fn.buscarHijo("ID");
                if (id != null) {
                    funciones.put(id.valor, fn);
                }
            }
        }

        pilaAmbitos = new ArrayDeque<>();
        pilaAmbitos.push(new Ambito());
        ejecutarListaSentencias(raiz.buscarHijo("LISTA_SENTENCIAS"));
    }

    private void ejecutarListaSentencias(Nodo lista) {
        if (lista == null) return;
        for (Nodo s : lista.buscarHijos("SENTENCIA")) {
            ejecutarSentencia(s);
        }
    }

    private void ejecutarSentencia(Nodo s) {
        if (s.hijos.isEmpty()) return;
        Nodo principal = s.hijos.get(0);

        switch (principal.etiqueta) {
            case "DECLARACION":     ejecutarDeclaracion(principal); break;
            case "ASIGNACION":      ejecutarAsignacion(principal); break;
            case "CONDICIONAL":     ejecutarCondicional(principal); break;
            case "CICLO":           ejecutarCicloMientras(principal); break;
            case "CICLO_PARA":      ejecutarCicloPara(principal); break;
            case "LECTURA":         ejecutarLectura(principal); break;
            case "ESCRITURA":       ejecutarEscritura(principal); break;
            case "LLAMADA_FUNCION": ejecutarLlamadaFuncion(principal); break;
            case "RETORNAR":
                Object val = (principal.hijos.size() > 1) ? evaluarExpresion(principal.hijos.get(1)) : null;
                throw new SenalRetornar(val);
            case "ROMPER":
                throw new SenalRomper();
            case "CONTINUAR":
                throw new SenalContinuar();
            default:
                break;
        }
    }

    private void ejecutarDeclaracion(Nodo decl) {
        Nodo nodoTipo = decl.buscarHijo("TIPO");
        Nodo listaVars = decl.buscarHijo("LISTA_VARS");
        if (nodoTipo == null || listaVars == null) return;

        String tipo = nodoTipo.hijos.get(0).valor;
        Ambito actual = pilaAmbitos.peek();

        for (Nodo item : listaVars.buscarHijos("VAR_ITEM")) {
            Nodo nodoId = item.buscarHijo("ID");
            if (nodoId == null) continue;

            Object valor = (item.hijos.size() >= 3)
                           ? ajustarTipo(tipo, evaluarExpresion(item.hijos.get(2)))
                           : valorPorDefecto(tipo);
            actual.tipos.put(nodoId.valor, tipo);
            actual.valores.put(nodoId.valor, valor);
        }
    }

    private void ejecutarAsignacion(Nodo asig) {
        if (asig.hijos.size() < 3) return;
        String nombre = asig.hijos.get(0).valor;
        Object valor = evaluarExpresion(asig.hijos.get(2));
        Ambito ambito = buscarAmbitoVariable(nombre);
        if (ambito != null) {
            ambito.valores.put(nombre, ajustarTipo(ambito.tipos.get(nombre), valor));
        }
    }

    private void ejecutarCondicional(Nodo condNode) {
        boolean condicion = Boolean.TRUE.equals(evaluarExpresion(condNode.hijos.get(2)));
        List<Nodo> bloques = condNode.buscarHijos("LISTA_SENTENCIAS");

        Nodo bloqueObjetivo = null;
        if (condicion && !bloques.isEmpty()) {
            bloqueObjetivo = bloques.get(0);
        } else if (!condicion && bloques.size() > 1) {
            bloqueObjetivo = bloques.get(1);
        }

        if (bloqueObjetivo != null) {
            pilaAmbitos.push(new Ambito());
            try {
                ejecutarListaSentencias(bloqueObjetivo);
            } finally {
                pilaAmbitos.pop();
            }
        }
    }

    private void ejecutarCicloMientras(Nodo ciclo) {
        Nodo exprCond = ciclo.hijos.get(2);
        Nodo cuerpo = ciclo.buscarHijo("LISTA_SENTENCIAS");

        while (Boolean.TRUE.equals(evaluarExpresion(exprCond))) {
            pilaAmbitos.push(new Ambito());
            try {
                ejecutarListaSentencias(cuerpo);
            } catch (SenalContinuar c) {
                // Pasar a la siguiente evaluación de la condición
            } catch (SenalRomper r) {
                break;
            } finally {
                pilaAmbitos.pop();
            }
        }
    }

    private void ejecutarCicloPara(Nodo para) {
        Nodo init = para.hijos.get(2);
        Nodo cond = para.hijos.get(4);
        Nodo paso = para.hijos.get(6);
        Nodo cuerpo = para.buscarHijo("LISTA_SENTENCIAS");

        pilaAmbitos.push(new Ambito());
        try {
            if ("DECLARACION".equals(init.etiqueta)) {
                ejecutarDeclaracion(init);
            } else if ("ASIGNACION".equals(init.etiqueta)) {
                ejecutarAsignacion(init);
            }

            while (Boolean.TRUE.equals(evaluarExpresion(cond))) {
                pilaAmbitos.push(new Ambito());
                boolean romper = false;
                try {
                    ejecutarListaSentencias(cuerpo);
                } catch (SenalContinuar c) {
                    // Continuar ejecutando el paso antes de reevaluar la condición
                } catch (SenalRomper r) {
                    romper = true;
                } finally {
                    pilaAmbitos.pop();
                }
                if (romper) break;
                ejecutarAsignacion(paso);
            }
        } finally {
            pilaAmbitos.pop();
        }
    }

    private void ejecutarLectura(Nodo lect) {
        Nodo nodoId = lect.buscarHijo("ID");
        if (nodoId == null) return;

        Ambito ambito = buscarAmbitoVariable(nodoId.valor);
        if (ambito == null) return;

        String tipo = ambito.tipos.get(nodoId.valor);
        String entrada = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        try {
            switch (tipo) {
                case "ENTERO":
                    ambito.valores.put(nodoId.valor, Integer.parseInt(entrada));
                    break;
                case "DECIMAL":
                    ambito.valores.put(nodoId.valor, Double.parseDouble(entrada));
                    break;
                case "BOOLEANO":
                    if ("VERDADERO".equalsIgnoreCase(entrada) || "true".equalsIgnoreCase(entrada)) {
                        ambito.valores.put(nodoId.valor, Boolean.TRUE);
                    } else if ("FALSO".equalsIgnoreCase(entrada) || "false".equalsIgnoreCase(entrada)) {
                        ambito.valores.put(nodoId.valor, Boolean.FALSE);
                    } else {
                        throw new ErrorEjecucion("Línea " + nodoId.linea + ": Valor booleano inválido '" + entrada +
                                                 "' (use VERDADERO o FALSO).");
                    }
                    break;
                case "CADENA":
                default:
                    ambito.valores.put(nodoId.valor, entrada);
                    break;
            }
        } catch (NumberFormatException e) {
            throw new ErrorEjecucion("Línea " + nodoId.linea + ": Se esperaba un valor de tipo " + tipo +
                                     " para '" + nodoId.valor + "', se recibió '" + entrada + "'.");
        }
    }

    private void ejecutarEscritura(Nodo esc) {
        System.out.println(formatearValor(evaluarExpresion(esc.hijos.get(2))));
    }

    private Object ejecutarLlamadaFuncion(Nodo llamada) {
        Nodo nodoId = llamada.buscarHijo("ID");
        Nodo fn = funciones.get(nodoId.valor);
        if (fn == null) {
            throw new ErrorEjecucion("Línea " + nodoId.linea + ": Función '" + nodoId.valor + "' no encontrada.");
        }

        List<Object> argsEvaluados = new ArrayList<>();
        Nodo listaArgs = llamada.buscarHijo("LISTA_ARGUMENTOS");
        if (listaArgs != null) {
            for (Nodo h : listaArgs.hijos) {
                if (!"DEL_COMA".equals(h.etiqueta)) {
                    argsEvaluados.add(evaluarExpresion(h));
                }
            }
        }

        Ambito ambitoFn = new Ambito();
        Nodo listaParams = fn.buscarHijo("LISTA_PARAMETROS");
        if (listaParams != null) {
            int idx = 0;
            for (Nodo param : listaParams.buscarHijos("PARAMETRO")) {
                String pTipo = param.buscarHijo("TIPO").hijos.get(0).valor;
                String pNombre = param.buscarHijo("ID").valor;
                ambitoFn.tipos.put(pNombre, pTipo);
                ambitoFn.valores.put(pNombre, ajustarTipo(pTipo, argsEvaluados.get(idx++)));
            }
        }

        // Aísla la pila de ámbitos durante la llamada y la restaura al regresar
        Deque<Ambito> pilaAnterior = pilaAmbitos;
        pilaAmbitos = new ArrayDeque<>();
        pilaAmbitos.push(ambitoFn);

        String tipoRet = fn.buscarHijo("TIPO").hijos.get(0).valor;
        Object resultado = "VACIO".equals(tipoRet) ? null : valorPorDefecto(tipoRet);

        try {
            ejecutarListaSentencias(fn.buscarHijo("LISTA_SENTENCIAS"));
        } catch (SenalRetornar ret) {
            resultado = "VACIO".equals(tipoRet) ? null : ajustarTipo(tipoRet, ret.valor);
        } finally {
            pilaAmbitos = pilaAnterior;
        }
        return resultado;
    }

    private Object evaluarExpresion(Nodo expr) {
        if (expr == null) return null;

        switch (expr.etiqueta) {
            case "LIT_ENTERO":    return Integer.parseInt(expr.valor);
            case "LIT_DECIMAL":   return Double.parseDouble(expr.valor);
            case "LIT_CADENA":    return desescaparCadena(expr.valor);
            case "LIT_VERDADERO": return Boolean.TRUE;
            case "LIT_FALSO":     return Boolean.FALSE;
            case "LLAMADA_FUNCION":
                return ejecutarLlamadaFuncion(expr);
            case "ID": {
                Ambito ambito = buscarAmbitoVariable(expr.valor);
                if (ambito == null) {
                    throw new ErrorEjecucion("Línea " + expr.linea + ": Variable '" + expr.valor + "' no definida.");
                }
                return ambito.valores.get(expr.valor);
            }
            case "EXPRESION":
                break;
            default:
                return null;
        }

        if (expr.hijos.size() == 3 && "DEL_PAR_IZQ".equals(expr.hijos.get(0).etiqueta)) {
            return evaluarExpresion(expr.hijos.get(1));
        }

        if (expr.hijos.size() == 2) {
            String op = expr.hijos.get(0).etiqueta;
            Object val = evaluarExpresion(expr.hijos.get(1));
            if ("OP_NO".equals(op)) {
                return !Boolean.TRUE.equals(val);
            }
            if ("OP_RESTA".equals(op)) {
                return (val instanceof Double) ? -((Double) val) : -((Integer) val);
            }
        }

        if (expr.hijos.size() == 3) {
            String op = expr.hijos.get(1).etiqueta;

            // Evaluación en cortocircuito estilo Java para Y / O
            if ("OP_Y".equals(op)) {
                return Boolean.TRUE.equals(evaluarExpresion(expr.hijos.get(0)))
                    && Boolean.TRUE.equals(evaluarExpresion(expr.hijos.get(2)));
            }
            if ("OP_O".equals(op)) {
                return Boolean.TRUE.equals(evaluarExpresion(expr.hijos.get(0)))
                    || Boolean.TRUE.equals(evaluarExpresion(expr.hijos.get(2)));
            }

            Object v1 = evaluarExpresion(expr.hijos.get(0));
            Object v2 = evaluarExpresion(expr.hijos.get(2));
            return evaluarBinaria(op, v1, v2, expr.linea);
        }
        return null;
    }

    private Object evaluarBinaria(String op, Object v1, Object v2, int linea) {
        switch (op) {
            case "OP_SUMA":
                if (v1 instanceof String || v2 instanceof String) {
                    return formatearValor(v1) + formatearValor(v2);
                }
                return operarNumero(op, v1, v2, linea);
            case "OP_RESTA":
            case "OP_MULT":
            case "OP_DIV":
            case "OP_MOD":
                return operarNumero(op, v1, v2, linea);
            case "OP_MENOR":
                return ((Number) v1).doubleValue() < ((Number) v2).doubleValue();
            case "OP_MENOR_IGUAL":
                return ((Number) v1).doubleValue() <= ((Number) v2).doubleValue();
            case "OP_MAYOR":
                return ((Number) v1).doubleValue() > ((Number) v2).doubleValue();
            case "OP_MAYOR_IGUAL":
                return ((Number) v1).doubleValue() >= ((Number) v2).doubleValue();
            case "OP_IGUAL":
                if (v1 instanceof Number && v2 instanceof Number) {
                    return ((Number) v1).doubleValue() == ((Number) v2).doubleValue();
                }
                return Objects.equals(v1, v2);
            case "OP_DIFERENTE":
                if (v1 instanceof Number && v2 instanceof Number) {
                    return ((Number) v1).doubleValue() != ((Number) v2).doubleValue();
                }
                return !Objects.equals(v1, v2);
            default:
                return null;
        }
    }

    private Number operarNumero(String op, Object v1, Object v2, int linea) {
        boolean esDecimal = (v1 instanceof Double || v2 instanceof Double);
        if (esDecimal) {
            double a = ((Number) v1).doubleValue();
            double b = ((Number) v2).doubleValue();
            switch (op) {
                case "OP_SUMA":  return a + b;
                case "OP_RESTA": return a - b;
                case "OP_MULT":  return a * b;
                case "OP_DIV":
                    if (b == 0.0) throw new ErrorEjecucion("Línea " + linea + ": División entre cero.");
                    return a / b;
                case "OP_MOD":
                    if (b == 0.0) throw new ErrorEjecucion("Línea " + linea + ": Módulo entre cero.");
                    return a % b;
                default: return 0.0;
            }
        }

        int a = ((Number) v1).intValue();
        int b = ((Number) v2).intValue();
        switch (op) {
            case "OP_SUMA":  return a + b;
            case "OP_RESTA": return a - b;
            case "OP_MULT":  return a * b;
            case "OP_DIV":
                if (b == 0) throw new ErrorEjecucion("Línea " + linea + ": División entre cero.");
                return a / b;
            case "OP_MOD":
                if (b == 0) throw new ErrorEjecucion("Línea " + linea + ": Módulo entre cero.");
                return a % b;
            default: return 0;
        }
    }

    private Ambito buscarAmbitoVariable(String nombre) {
        for (Ambito a : pilaAmbitos) {
            if (a.valores.containsKey(nombre)) return a;
        }
        return null;
    }

    private Object valorPorDefecto(String tipo) {
        switch (tipo) {
            case "ENTERO":   return 0;
            case "DECIMAL":  return 0.0;
            case "BOOLEANO": return Boolean.FALSE;
            case "CADENA":   return "";
            default:         return null;
        }
    }

    private Object ajustarTipo(String tipo, Object valor) {
        if ("DECIMAL".equals(tipo) && valor instanceof Integer) {
            return ((Integer) valor).doubleValue();
        }
        return valor;
    }

    private String formatearValor(Object val) {
        if (val == null) return "VACIO";
        if (val instanceof Boolean) {
            return ((Boolean) val) ? "VERDADERO" : "FALSO";
        }
        return val.toString();
    }

    private String desescaparCadena(String raw) {
        if (raw == null || raw.length() < 2) return "";
        String contenido = raw.substring(1, raw.length() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (c == '\\' && i + 1 < contenido.length()) {
                char sig = contenido.charAt(++i);
                switch (sig) {
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case 'r':  sb.append('\r'); break;
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    default:   sb.append(sig);  break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
