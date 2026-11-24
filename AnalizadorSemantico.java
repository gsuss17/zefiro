import java.util.ArrayList;
import java.util.List;

public class AnalizadorSemantico {
    public final TablaSimbolos tabla = new TablaSimbolos();
    public final List<String> errores = new ArrayList<>();

    private String tipoRetornoActual = null;
    private int profundidadCiclo = 0;
    private int contadorBloques = 0;

    public void analizar(Nodo raiz) {
        if (raiz == null) return;

        Nodo listaFunciones = buscarHijo(raiz, "LISTA_FUNCIONES");
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

        Nodo listaSentencias = buscarHijo(raiz, "LISTA_SENTENCIAS");
        if (listaSentencias != null) {
            analizarListaSentencias(listaSentencias);
        }
    }

    private void registrarFirmaFuncion(Nodo fn) {
        if (buscarHijo(fn, "ERROR_FUNCION") != null) return;

        Nodo nodoTipo = buscarHijo(fn, "TIPO");
        Nodo nodoId = buscarHijo(fn, "ID");
        if (nodoTipo == null || nodoId == null) return;

        String tipoRet = extraerTipo(nodoTipo);
        String nombre = nodoId.valor;
        Simbolo symFn = new Simbolo(nombre, tipoRet, "FUNCION", "global", nodoId.linea, true);

        Nodo listaParams = buscarHijo(fn, "LISTA_PARAMETROS");
        if (listaParams != null) {
            for (Nodo hijo : listaParams.hijos) {
                if ("PARAMETRO".equals(hijo.etiqueta)) {
                    Nodo pTipo = buscarHijo(hijo, "TIPO");
                    if (pTipo != null) {
                        symFn.parametrosTipos.add(extraerTipo(pTipo));
                    }
                }
            }
        }

        if (!tabla.declarar(symFn)) {
            agregarError(nodoId.linea, nodoId.columna, "La función '" + nombre + "' ya fue declarada.");
        }
    }

    private void analizarFuncion(Nodo fn) {
        if (buscarHijo(fn, "ERROR_FUNCION") != null) return;

        Nodo nodoTipo = buscarHijo(fn, "TIPO");
        Nodo nodoId = buscarHijo(fn, "ID");
        if (nodoTipo == null || nodoId == null) return;

        String tipoAnterior = tipoRetornoActual;
        tipoRetornoActual = extraerTipo(nodoTipo);
        tabla.entrarAmbito("fn_" + nodoId.valor);

        Nodo listaParams = buscarHijo(fn, "LISTA_PARAMETROS");
        if (listaParams != null) {
            for (Nodo hijo : listaParams.hijos) {
                if ("PARAMETRO".equals(hijo.etiqueta)) {
                    Nodo pTipo = buscarHijo(hijo, "TIPO");
                    Nodo pId = buscarHijo(hijo, "ID");
                    if (pTipo != null && pId != null) {
                        Simbolo param = new Simbolo(pId.valor, extraerTipo(pTipo), "PARAMETRO",
                                                    tabla.ambitoActual(), pId.linea, true);
                        if (!tabla.declarar(param)) {
                            agregarError(pId.linea, pId.columna,
                                         "Parámetro duplicado '" + pId.valor + "' en función '" + nodoId.valor + "'.");
                        }
                    }
                }
            }
        }

        Nodo cuerpo = buscarHijo(fn, "LISTA_SENTENCIAS");
        if (cuerpo != null) {
            analizarListaSentencias(cuerpo);
        }

        tabla.salirAmbito();
        tipoRetornoActual = tipoAnterior;
    }

    private void analizarListaSentencias(Nodo lista) {
        if (lista == null) return;
        for (Nodo s : lista.hijos) {
            if ("SENTENCIA".equals(s.etiqueta)) {
                analizarSentencia(s);
            }
        }
    }

    private void analizarSentencia(Nodo s) {
        if (s.hijos.isEmpty()) return;
        Nodo principal = s.hijos.get(0);

        switch (principal.etiqueta) {
            case "DECLARACION":
                analizarDeclaracion(principal);
                break;
            case "ASIGNACION":
                analizarAsignacion(principal);
                break;
            case "CONDICIONAL":
                analizarCondicional(principal);
                break;
            case "CICLO":
                analizarCicloMientras(principal);
                break;
            case "CICLO_PARA":
                analizarCicloPara(principal);
                break;
            case "LECTURA":
                analizarLectura(principal);
                break;
            case "ESCRITURA":
                analizarEscritura(principal);
                break;
            case "LLAMADA_FUNCION":
                analizarLlamadaFuncion(principal);
                break;
            case "RETORNAR":
                analizarRetornar(principal);
                break;
            case "ROMPER":
                if (profundidadCiclo == 0) {
                    agregarError(principal.linea, principal.columna,
                                 "'ROMPER' solo puede usarse dentro de un ciclo (MIENTRAS o PARA).");
                }
                break;
            case "CONTINUAR":
                if (profundidadCiclo == 0) {
                    agregarError(principal.linea, principal.columna,
                                 "'CONTINUAR' solo puede usarse dentro de un ciclo (MIENTRAS o PARA).");
                }
                break;
            default:
                break;
        }
    }

    private void analizarDeclaracion(Nodo decl) {
        Nodo nodoTipo = buscarHijo(decl, "TIPO");
        Nodo listaVars = buscarHijo(decl, "LISTA_VARS");
        if (nodoTipo == null || listaVars == null) return;

        String tipo = extraerTipo(nodoTipo);
        for (Nodo hijo : listaVars.hijos) {
            if ("VAR_ITEM".equals(hijo.etiqueta)) {
                Nodo nodoId = buscarHijo(hijo, "ID");
                if (nodoId == null) continue;

                boolean tieneInit = hijo.hijos.size() >= 3;
                if (tieneInit) {
                    Nodo expr = hijo.hijos.get(2);
                    String tipoExpr = inferirTipoExpresion(expr);
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
    }

    private void analizarAsignacion(Nodo asig) {
        if (asig.hijos.size() < 3) return;
        Nodo nodoId = asig.hijos.get(0);
        Nodo expr = asig.hijos.get(2);

        Simbolo sym = tabla.buscar(nodoId.valor);
        if (sym == null) {
            agregarError(nodoId.linea, nodoId.columna,
                         "Variable '" + nodoId.valor + "' no declarada.");
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
        if (buscarHijo(condNode, "ERROR_CONDICIONAL") != null) return;
        // Hijos: PR_SI, (, cond, ), ls1, [SINO, ls2], FINSI, ;
        Nodo exprCond = condNode.hijos.get(2);
        String tipoCond = inferirTipoExpresion(exprCond);
        if (!"ERROR".equals(tipoCond) && !"BOOLEANO".equals(tipoCond)) {
            agregarError(exprCond.linea, exprCond.columna,
                         "La condición de 'SI' debe ser BOOLEANO, se recibió " + tipoCond + ".");
        }

        List<Nodo> bloques = new ArrayList<>();
        for (Nodo h : condNode.hijos) {
            if ("LISTA_SENTENCIAS".equals(h.etiqueta)) {
                bloques.add(h);
            }
        }

        for (Nodo bloque : bloques) {
            tabla.entrarAmbito("si_" + (++contadorBloques));
            analizarListaSentencias(bloque);
            tabla.salirAmbito();
        }
    }

    private void analizarCicloMientras(Nodo ciclo) {
        if (buscarHijo(ciclo, "ERROR_CICLO") != null) return;
        Nodo exprCond = ciclo.hijos.get(2);
        String tipoCond = inferirTipoExpresion(exprCond);
        if (!"ERROR".equals(tipoCond) && !"BOOLEANO".equals(tipoCond)) {
            agregarError(exprCond.linea, exprCond.columna,
                         "La condición de 'MIENTRAS' debe ser BOOLEANO, se recibió " + tipoCond + ".");
        }

        Nodo cuerpo = buscarHijo(ciclo, "LISTA_SENTENCIAS");
        tabla.entrarAmbito("mientras_" + (++contadorBloques));
        profundidadCiclo++;
        analizarListaSentencias(cuerpo);
        profundidadCiclo--;
        tabla.salirAmbito();
    }

    private void analizarCicloPara(Nodo para) {
        if (buscarHijo(para, "ERROR_CICLO_PARA") != null) return;
        // Hijos: PARA, (, init, ;, cond, ;, paso, ), ls, FINPARA, ;
        tabla.entrarAmbito("para_" + (++contadorBloques));

        Nodo init = para.hijos.get(2);
        if ("DECLARACION".equals(init.etiqueta)) {
            analizarDeclaracion(init);
        } else if ("ASIGNACION".equals(init.etiqueta)) {
            analizarAsignacion(init);
        }

        Nodo cond = para.hijos.get(4);
        String tipoCond = inferirTipoExpresion(cond);
        if (!"ERROR".equals(tipoCond) && !"BOOLEANO".equals(tipoCond)) {
            agregarError(cond.linea, cond.columna,
                         "La condición de 'PARA' debe ser BOOLEANO, se recibió " + tipoCond + ".");
        }

        Nodo paso = para.hijos.get(6);
        analizarAsignacion(paso);

        Nodo cuerpo = buscarHijo(para, "LISTA_SENTENCIAS");
        profundidadCiclo++;
        analizarListaSentencias(cuerpo);
        profundidadCiclo--;

        tabla.salirAmbito();
    }

    private void analizarLectura(Nodo lect) {
        Nodo nodoId = buscarHijo(lect, "ID");
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
        String tipo = inferirTipoExpresion(expr);
        if ("VACIO".equals(tipo)) {
            agregarError(expr.linea, expr.columna,
                         "No se puede imprimir una expresión de tipo VACIO.");
        }
    }

    private void analizarRetornar(Nodo ret) {
        if (tipoRetornoActual == null) {
            agregarError(ret.linea, ret.columna,
                         "'RETORNAR' solo puede usarse dentro de una función.");
            return;
        }

        if (ret.hijos.size() == 1) {
            if (!"VACIO".equals(tipoRetornoActual)) {
                agregarError(ret.linea, ret.columna,
                             "La función debe retornar un valor de tipo " + tipoRetornoActual + ".");
            }
        } else {
            Nodo expr = ret.hijos.get(1);
            String tipoExpr = inferirTipoExpresion(expr);
            if ("VACIO".equals(tipoRetornoActual)) {
                agregarError(ret.linea, ret.columna,
                             "Una función VACIO no debe retornar un valor.");
            } else if (!"ERROR".equals(tipoExpr) && !esCompatible(tipoRetornoActual, tipoExpr)) {
                agregarError(expr.linea, expr.columna,
                             "Tipo de retorno incompatible: se esperaba " + tipoRetornoActual +
                             " y se recibió " + tipoExpr + ".");
            }
        }
    }

    private String analizarLlamadaFuncion(Nodo llamada) {
        Nodo nodoId = buscarHijo(llamada, "ID");
        if (nodoId == null) return "ERROR";

        Simbolo sym = tabla.buscar(nodoId.valor);
        if (sym == null || !"FUNCION".equals(sym.categoria)) {
            agregarError(nodoId.linea, nodoId.columna,
                         "Función '" + nodoId.valor + "' no declarada.");
            return "ERROR";
        }

        List<Nodo> args = new ArrayList<>();
        Nodo listaArgs = buscarHijo(llamada, "LISTA_ARGUMENTOS");
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
            case "LIT_ENTERO": return "ENTERO";
            case "LIT_DECIMAL": return "DECIMAL";
            case "LIT_CADENA": return "CADENA";
            case "LIT_VERDADERO":
            case "LIT_FALSO": return "BOOLEANO";
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
                return sym.tipo;
            }
            case "LLAMADA_FUNCION":
                return analizarLlamadaFuncion(expr);
            case "EXPRESION":
                break;
            default:
                return "ERROR";
        }

        // Paréntesis: (, expr, )
        if (expr.hijos.size() == 3 && "DEL_PAR_IZQ".equals(expr.hijos.get(0).etiqueta)) {
            return inferirTipoExpresion(expr.hijos.get(1));
        }

        // Unarios: OP_NO expr | OP_RESTA expr
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
            } else if ("OP_RESTA".equals(op)) {
                if (!esNumerico(t)) {
                    agregarError(expr.linea, expr.columna,
                                 "El operador '-' unario requiere un operando numérico, se recibió " + t + ".");
                    return "ERROR";
                }
                return t;
            }
        }

        // Binarios: e1 op e2
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
                    if (esNumerico(t1) && esNumerico(t2)) {
                        return ("DECIMAL".equals(t1) || "DECIMAL".equals(t2)) ? "DECIMAL" : "ENTERO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "Operación '+' inválida entre " + t1 + " y " + t2 + ".");
                    return "ERROR";

                case "OP_RESTA":
                case "OP_MULT":
                case "OP_DIV":
                    if (esNumerico(t1) && esNumerico(t2)) {
                        return ("DECIMAL".equals(t1) || "DECIMAL".equals(t2)) ? "DECIMAL" : "ENTERO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "Operación aritmética inválida entre " + t1 + " y " + t2 + ".");
                    return "ERROR";

                case "OP_MOD":
                    if ("ENTERO".equals(t1) && "ENTERO".equals(t2)) {
                        return "ENTERO";
                    }
                    agregarError(expr.linea, expr.columna,
                                 "El operador '%' requiere operandos ENTERO, se recibió " + t1 + " y " + t2 + ".");
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

    private Nodo buscarHijo(Nodo padre, String etiqueta) {
        if (padre == null) return null;
        for (Nodo h : padre.hijos) {
            if (etiqueta.equals(h.etiqueta)) return h;
        }
        return null;
    }

    private void agregarError(int linea, int columna, String mensaje) {
        errores.add(String.format("Línea %-3d | Columna %-3d | %s", linea, columna, mensaje));
    }
}
