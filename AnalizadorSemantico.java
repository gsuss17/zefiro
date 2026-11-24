import java.util.ArrayList;
import java.util.List;

/**
 * Analizador semántico para Zefiro.
 * Valida ámbitos, declaraciones, inicialización, compatibilidad de tipos,
 * control de flujo (ROMPER/CONTINUAR) y retornos de funciones estilo Java.
 */
public class AnalizadorSemantico {
    public final TablaSimbolos tabla = new TablaSimbolos();
    public final List<String> errores = new ArrayList<>();

    private String tipoRetornoActual = null;
    private int profundidadCiclo = 0;
    private int contadorBloques = 0;

    public void analizar(Nodo raiz) {
        if (raiz == null) return;

        // Paso 1: registrar firmas de funciones para permitir llamadas en cualquier orden y recursión
        Nodo listaFunciones = raiz.buscarHijo("LISTA_FUNCIONES");
        if (listaFunciones != null) {
            for (Nodo fn : listaFunciones.hijos) {
                if ("FUNCION".equals(fn.etiqueta)) {
                    registrarFirmaFuncion(fn);
                }
            }
            for (Nodo fn : listaFunciones.hijos) {
                if ("FUNCION".equals(fn.etiqueta)) {
                    analizarFuncion(fn);
                }
            }
        }

        // Paso 2: analizar el cuerpo principal del programa
        Nodo listaSentencias = raiz.buscarHijo("LISTA_SENTENCIAS");
        if (listaSentencias != null) {
            analizarListaSentencias(listaSentencias);
        }
    }

    private void registrarFirmaFuncion(Nodo fn) {
        if (fn.buscarHijo("ERROR_FUNCION") != null) return;

        Nodo nodoTipo = fn.buscarHijo("TIPO");
        Nodo nodoId = fn.buscarHijo("ID");
        if (nodoTipo == null || nodoId == null) return;

        Simbolo symFn = new Simbolo(nodoId.valor, extraerTipo(nodoTipo), "FUNCION",
                                    "global", nodoId.linea, true);

        Nodo listaParams = fn.buscarHijo("LISTA_PARAMETROS");
        if (listaParams != null) {
            for (Nodo param : listaParams.buscarHijos("PARAMETRO")) {
                Nodo pTipo = param.buscarHijo("TIPO");
                if (pTipo != null) {
                    symFn.parametrosTipos.add(extraerTipo(pTipo));
                }
            }
        }

        if (!tabla.declarar(symFn)) {
            agregarError(nodoId.linea, nodoId.columna, "La función '" + nodoId.valor + "' ya fue declarada.");
        }
    }

    private void analizarFuncion(Nodo fn) {
        if (fn.buscarHijo("ERROR_FUNCION") != null) return;

        Nodo nodoTipo = fn.buscarHijo("TIPO");
        Nodo nodoId = fn.buscarHijo("ID");
        if (nodoTipo == null || nodoId == null) return;

        String tipoAnterior = tipoRetornoActual;
        tipoRetornoActual = extraerTipo(nodoTipo);
        tabla.entrarAmbito("fn_" + nodoId.valor);

        Nodo listaParams = fn.buscarHijo("LISTA_PARAMETROS");
        if (listaParams != null) {
            for (Nodo param : listaParams.buscarHijos("PARAMETRO")) {
                Nodo pTipo = param.buscarHijo("TIPO");
                Nodo pId = param.buscarHijo("ID");
                if (pTipo != null && pId != null) {
                    Simbolo symParam = new Simbolo(pId.valor, extraerTipo(pTipo), "PARAMETRO",
                                                   tabla.ambitoActual(), pId.linea, true);
                    if (!tabla.declarar(symParam)) {
                        agregarError(pId.linea, pId.columna,
                                     "Parámetro duplicado '" + pId.valor + "' en función '" + nodoId.valor + "'.");
                    }
                }
            }
        }

        Nodo cuerpo = fn.buscarHijo("LISTA_SENTENCIAS");
        if (cuerpo != null) {
            analizarListaSentencias(cuerpo);
        }

        if (!"VACIO".equals(tipoRetornoActual) && !garantizaRetorno(cuerpo)) {
            agregarError(nodoId.linea, nodoId.columna,
                         "La función '" + nodoId.valor + "' debe retornar un valor de tipo " +
                         tipoRetornoActual + " en todos sus caminos.");
        }

        tabla.salirAmbito();
        tipoRetornoActual = tipoAnterior;
    }

    private void analizarListaSentencias(Nodo lista) {
        if (lista == null) return;
        for (Nodo s : lista.buscarHijos("SENTENCIA")) {
            analizarSentencia(s);
        }
    }

    private void analizarSentencia(Nodo s) {
        if (s.hijos.isEmpty()) return;
        Nodo principal = s.hijos.get(0);

        switch (principal.etiqueta) {
            case "DECLARACION":     analizarDeclaracion(principal); break;
            case "ASIGNACION":      analizarAsignacion(principal); break;
            case "CONDICIONAL":     analizarCondicional(principal); break;
            case "CICLO":           analizarCicloMientras(principal); break;
            case "CICLO_PARA":      analizarCicloPara(principal); break;
            case "LECTURA":         analizarLectura(principal); break;
            case "ESCRITURA":       analizarEscritura(principal); break;
            case "LLAMADA_FUNCION": analizarLlamadaFuncion(principal); break;
            case "RETORNAR":        analizarRetornar(principal); break;
            case "ROMPER":
            case "CONTINUAR":
                if (profundidadCiclo == 0) {
                    agregarError(principal.linea, principal.columna,
                                 "'" + principal.etiqueta + "' solo puede usarse dentro de un ciclo (MIENTRAS o PARA).");
                }
                break;
            default:
                break;
        }
    }

    private void analizarDeclaracion(Nodo decl) {
        Nodo nodoTipo = decl.buscarHijo("TIPO");
        Nodo listaVars = decl.buscarHijo("LISTA_VARS");
        if (nodoTipo == null || listaVars == null) return;

        String tipo = extraerTipo(nodoTipo);
        for (Nodo item : listaVars.buscarHijos("VAR_ITEM")) {
            Nodo nodoId = item.buscarHijo("ID");
            if (nodoId == null) continue;

            boolean tieneInit = item.hijos.size() >= 3;
            if (tieneInit) {
                String tipoExpr = inferirTipoExpresion(item.hijos.get(2));
                if (!"ERROR".equals(tipoExpr) && !esCompatible(tipo, tipoExpr)) {
                    agregarError(nodoId.linea, nodoId.columna,
                                 "Tipo incompatible al inicializar '" + nodoId.valor + "': se esperaba " +
                                 tipo + " y se recibió " + tipoExpr + ".");
                }
            }

            Simbolo sym = new Simbolo(nodoId.valor, tipo, "VARIABLE",
                                      tabla.ambitoActual(), nodoId.linea, tieneInit);
            if (!tabla.declarar(sym)) {
                agregarError(nodoId.linea, nodoId.columna,
                             "La variable '" + nodoId.valor + "' ya fue declarada en este ámbito.");
            }
        }
    }

    private void analizarAsignacion(Nodo asig) {
        if (asig.hijos.size() < 3) return;
        Nodo nodoId = asig.hijos.get(0);
        Nodo expr = asig.hijos.get(2);

        Simbolo sym = tabla.buscar(nodoId.valor);
        if (sym == null) {
            agregarError(nodoId.linea, nodoId.columna, "Variable '" + nodoId.valor + "' no declarada.");
            inferirTipoExpresion(expr);
            return;
        }
        if ("FUNCION".equals(sym.categoria)) {
            agregarError(nodoId.linea, nodoId.columna,
                         "No se puede asignar un valor a la función '" + nodoId.valor + "'.");
            return;
        }

        String tipoExpr = inferirTipoExpresion(expr);
        if (!"ERROR".equals(tipoExpr) && !esCompatible(sym.tipo, tipoExpr)) {
            agregarError(nodoId.linea, nodoId.columna,
                         "Tipo incompatible en asignación a '" + nodoId.valor + "': se esperaba " +
                         sym.tipo + " y se recibió " + tipoExpr + ".");
        }
        sym.inicializada = true;
    }

    private void analizarCondicional(Nodo condNode) {
        if (condNode.buscarHijo("ERROR_CONDICIONAL") != null) return;
        validarCondicionBooleana(condNode.hijos.get(2), "SI");

        for (Nodo bloque : condNode.buscarHijos("LISTA_SENTENCIAS")) {
            tabla.entrarAmbito("si_" + (++contadorBloques));
            analizarListaSentencias(bloque);
            tabla.salirAmbito();
        }
    }

    private void analizarCicloMientras(Nodo ciclo) {
        if (ciclo.buscarHijo("ERROR_CICLO") != null) return;
        validarCondicionBooleana(ciclo.hijos.get(2), "MIENTRAS");

        tabla.entrarAmbito("mientras_" + (++contadorBloques));
        profundidadCiclo++;
        analizarListaSentencias(ciclo.buscarHijo("LISTA_SENTENCIAS"));
        profundidadCiclo--;
        tabla.salirAmbito();
    }

    private void analizarCicloPara(Nodo para) {
        if (para.buscarHijo("ERROR_CICLO_PARA") != null) return;
        tabla.entrarAmbito("para_" + (++contadorBloques));

        Nodo init = para.hijos.get(2);
        if ("DECLARACION".equals(init.etiqueta)) {
            analizarDeclaracion(init);
        } else if ("ASIGNACION".equals(init.etiqueta)) {
            analizarAsignacion(init);
        }

        validarCondicionBooleana(para.hijos.get(4), "PARA");
        analizarAsignacion(para.hijos.get(6));

        profundidadCiclo++;
        analizarListaSentencias(para.buscarHijo("LISTA_SENTENCIAS"));
        profundidadCiclo--;

        tabla.salirAmbito();
    }

    private void analizarLectura(Nodo lect) {
        Nodo nodoId = lect.buscarHijo("ID");
        if (nodoId == null) return;

        Simbolo sym = tabla.buscar(nodoId.valor);
        if (sym == null) {
            agregarError(nodoId.linea, nodoId.columna,
                         "Variable '" + nodoId.valor + "' no declarada en 'LEER'.");
        } else if ("FUNCION".equals(sym.categoria)) {
            agregarError(nodoId.linea, nodoId.columna,
                         "No se puede leer sobre la función '" + nodoId.valor + "'.");
        } else {
            sym.inicializada = true;
        }
    }

    private void analizarEscritura(Nodo esc) {
        if (esc.hijos.size() < 3) return;
        Nodo expr = esc.hijos.get(2);
        if ("VACIO".equals(inferirTipoExpresion(expr))) {
            agregarError(expr.linea, expr.columna,
                         "No se puede imprimir una expresión de tipo VACIO.");
        }
    }

    private void analizarRetornar(Nodo ret) {
        if (tipoRetornoActual == null) {
            agregarError(ret.linea, ret.columna, "'RETORNAR' solo puede usarse dentro de una función.");
            return;
        }

        if (ret.hijos.size() == 1) {
            if (!"VACIO".equals(tipoRetornoActual)) {
                agregarError(ret.linea, ret.columna,
                             "La función debe retornar un valor de tipo " + tipoRetornoActual + ".");
            }
            return;
        }

        Nodo expr = ret.hijos.get(1);
        String tipoExpr = inferirTipoExpresion(expr);
        if ("VACIO".equals(tipoRetornoActual)) {
            agregarError(ret.linea, ret.columna, "Una función VACIO no debe retornar un valor.");
        } else if (!"ERROR".equals(tipoExpr) && !esCompatible(tipoRetornoActual, tipoExpr)) {
            agregarError(expr.linea, expr.columna,
                         "Tipo de retorno incompatible: se esperaba " + tipoRetornoActual +
                         " y se recibió " + tipoExpr + ".");
        }
    }

    private String analizarLlamadaFuncion(Nodo llamada) {
        Nodo nodoId = llamada.buscarHijo("ID");
        if (nodoId == null) return "ERROR";

        Simbolo sym = tabla.buscar(nodoId.valor);
        if (sym == null || !"FUNCION".equals(sym.categoria)) {
            agregarError(nodoId.linea, nodoId.columna, "Función '" + nodoId.valor + "' no declarada.");
            return "ERROR";
        }

        List<Nodo> args = new ArrayList<>();
        Nodo listaArgs = llamada.buscarHijo("LISTA_ARGUMENTOS");
        if (listaArgs != null) {
            for (Nodo h : listaArgs.hijos) {
                if (!"DEL_COMA".equals(h.etiqueta)) {
                    args.add(h);
                }
            }
        }

        if (args.size() != sym.parametrosTipos.size()) {
            agregarError(nodoId.linea, nodoId.columna,
                         "La función '" + nodoId.valor + "' espera " + sym.parametrosTipos.size() +
                         " argumento(s), pero recibió " + args.size() + ".");
            return sym.tipo;
        }

        for (int i = 0; i < args.size(); i++) {
            String tipoEsperado = sym.parametrosTipos.get(i);
            String tipoReal = inferirTipoExpresion(args.get(i));
            if (!"ERROR".equals(tipoReal) && !esCompatible(tipoEsperado, tipoReal)) {
                agregarError(args.get(i).linea, args.get(i).columna,
                             "Argumento " + (i + 1) + " de '" + nodoId.valor + "' incompatible: se esperaba " +
                             tipoEsperado + " y se recibió " + tipoReal + ".");
            }
        }
        return sym.tipo;
    }

    private String inferirTipoExpresion(Nodo expr) {
        if (expr == null) return "ERROR";

        switch (expr.etiqueta) {
            case "LIT_ENTERO":    return "ENTERO";
            case "LIT_DECIMAL":   return "DECIMAL";
            case "LIT_CADENA":    return "CADENA";
            case "LIT_VERDADERO":
            case "LIT_FALSO":     return "BOOLEANO";
            case "LLAMADA_FUNCION":
                return analizarLlamadaFuncion(expr);
            case "ID": {
                Simbolo sym = tabla.buscar(expr.valor);
                if (sym == null) {
                    agregarError(expr.linea, expr.columna, "Variable '" + expr.valor + "' no declarada.");
                    return "ERROR";
                }
                if ("FUNCION".equals(sym.categoria)) {
                    agregarError(expr.linea, expr.columna,
                                 "'" + expr.valor + "' es una función; debe invocarse con '()'.");
                    return "ERROR";
                }
                if (!sym.inicializada) {
                    agregarError(expr.linea, expr.columna,
                                 "La variable '" + expr.valor + "' podría no haber sido inicializada.");
                }
                return sym.tipo;
            }
            case "EXPRESION":
                break;
            default:
                return "ERROR";
        }

        if (expr.hijos.size() == 3 && "DEL_PAR_IZQ".equals(expr.hijos.get(0).etiqueta)) {
            return inferirTipoExpresion(expr.hijos.get(1));
        }

        if (expr.hijos.size() == 2) {
            String op = expr.hijos.get(0).etiqueta;
            String t = inferirTipoExpresion(expr.hijos.get(1));
            if ("ERROR".equals(t)) return "ERROR";

            if ("OP_NO".equals(op)) {
                if (!"BOOLEANO".equals(t)) {
                    agregarError(expr.linea, expr.columna,
                                 "El operador 'NO' requiere un operando BOOLEANO, se recibió " + t + ".");
                    return "ERROR";
                }
                return "BOOLEANO";
            }
            if ("OP_RESTA".equals(op)) {
                if (!esNumerico(t)) {
                    agregarError(expr.linea, expr.columna,
                                 "El operador '-' unario requiere un operando numérico, se recibió " + t + ".");
                    return "ERROR";
                }
                return t;
            }
        }

        if (expr.hijos.size() == 3) {
            String t1 = inferirTipoExpresion(expr.hijos.get(0));
            String op = expr.hijos.get(1).etiqueta;
            String t2 = inferirTipoExpresion(expr.hijos.get(2));
            if ("ERROR".equals(t1) || "ERROR".equals(t2)) return "ERROR";

            switch (op) {
                case "OP_SUMA":
                    if ("CADENA".equals(t1) || "CADENA".equals(t2)) {
                        if ("VACIO".equals(t1) || "VACIO".equals(t2)) {
                            agregarError(expr.linea, expr.columna, "No se puede concatenar un valor VACIO.");
                            return "ERROR";
                        }
                        return "CADENA";
                    }
                    // Si no hay cadenas, '+' sigue las mismas reglas aritméticas que -, *, /, %
                case "OP_RESTA":
                case "OP_MULT":
                case "OP_DIV":
                case "OP_MOD":
                    if (esNumerico(t1) && esNumerico(t2)) {
                        return ("DECIMAL".equals(t1) || "DECIMAL".equals(t2)) ? "DECIMAL" : "ENTERO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "Operación aritmética inválida entre " + t1 + " y " + t2 + ".");
                    return "ERROR";

                case "OP_MENOR":
                case "OP_MENOR_IGUAL":
                case "OP_MAYOR":
                case "OP_MAYOR_IGUAL":
                    if (esNumerico(t1) && esNumerico(t2)) {
                        return "BOOLEANO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "Comparación relacional inválida entre " + t1 + " y " + t2 + ".");
                    return "ERROR";

                case "OP_IGUAL":
                case "OP_DIFERENTE":
                    if ((esNumerico(t1) && esNumerico(t2)) || t1.equals(t2)) {
                        return "BOOLEANO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "No se puede comparar igualdad entre " + t1 + " y " + t2 + ".");
                    return "ERROR";

                case "OP_Y":
                case "OP_O":
                    if ("BOOLEANO".equals(t1) && "BOOLEANO".equals(t2)) {
                        return "BOOLEANO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "Operador lógico requiere operandos BOOLEANO, se recibió " + t1 + " y " + t2 + ".");
                    return "ERROR";
            }
        }
        return "ERROR";
    }

    private void validarCondicionBooleana(Nodo exprCond, String estructura) {
        String tipoCond = inferirTipoExpresion(exprCond);
        if (!"ERROR".equals(tipoCond) && !"BOOLEANO".equals(tipoCond)) {
            agregarError(exprCond.linea, exprCond.columna,
                         "La condición de '" + estructura + "' debe ser BOOLEANO, se recibió " + tipoCond + ".");
        }
    }

    private boolean garantizaRetorno(Nodo lista) {
        if (lista == null) return false;
        for (Nodo s : lista.buscarHijos("SENTENCIA")) {
            if (s.hijos.isEmpty()) continue;
            Nodo p = s.hijos.get(0);
            if ("RETORNAR".equals(p.etiqueta)) {
                return true;
            }
            if ("CONDICIONAL".equals(p.etiqueta)) {
                List<Nodo> bloques = p.buscarHijos("LISTA_SENTENCIAS");
                if (bloques.size() == 2 && garantizaRetorno(bloques.get(0)) && garantizaRetorno(bloques.get(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean esNumerico(String tipo) {
        return "ENTERO".equals(tipo) || "DECIMAL".equals(tipo);
    }

    private boolean esCompatible(String esperado, String real) {
        if (esperado.equals(real)) return true;
        return "DECIMAL".equals(esperado) && "ENTERO".equals(real);
    }

    private String extraerTipo(Nodo nodoTipo) {
        if (nodoTipo == null || nodoTipo.hijos.isEmpty()) return "ERROR";
        return nodoTipo.hijos.get(0).valor;
    }

    private void agregarError(int linea, int columna, String mensaje) {
        errores.add(String.format("Línea %-3d | Columna %-3d | %s", linea, columna, mensaje));
    }
}
