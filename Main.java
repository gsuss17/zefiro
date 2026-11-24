import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java_cup.runtime.Symbol;

public class Main {
    public static void main(String[] args) {
        String archivoFuente = "becas.zefiro";
        boolean soloCompilar = false;

        for (String arg : args) {
            if ("--solo-compilar".equals(arg)) {
                soloCompilar = true;
            } else {
                archivoFuente = arg;
            }
        }

        File archivo = new File(archivoFuente);
        if (!archivo.exists()) {
            System.err.println("Error: No se encontró el archivo fuente: " + archivoFuente);
            return;
        }

        int totalTokensValidos = 0;
        List<String> erroresLexicosLista = new ArrayList<>();
        int erroresLexicosTotal = 0;

        // ======================================================================
        // FASE 1: ANÁLISIS LÉXICO (TABLA DE TOKENS)
        // ======================================================================
        System.out.println("==========================================================================");
        System.out.println("                        FASE 1: ANÁLISIS LÉXICO                           ");
        System.out.println("==========================================================================");
        System.out.printf("%-8s | %-8s | %-22s | %-30s%n", "LÍNEA", "COLUMNA", "TOKEN", "LEXEMA");
        System.out.println("--------------------------------------------------------------------------");

        try (InputStreamReader readerLex = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            Lexer lexerPrueba = new Lexer(readerLex);
            Symbol s;

            while ((s = lexerPrueba.next_token()) != null && s.sym != sym.EOF) {
                String lexema = (s.value != null) ? s.value.toString() : "";
                String lexemaVisual = lexema.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
                String nombreToken;

                if (s.sym == sym.error) {
                    nombreToken = "ERROR_LEXICO";
                    String motivo = diagnosticarErrorLexico(lexema);
                    erroresLexicosLista.add(String.format("Línea %-3d | Columna %-3d | %s: '%s'",
                                            s.left, s.right, motivo, lexemaVisual));
                } else {
                    nombreToken = (s.sym >= 0 && s.sym < sym.terminalNames.length)
                                  ? sym.terminalNames[s.sym]
                                  : ("TOKEN_" + s.sym);
                    totalTokensValidos++;
                }

                System.out.printf("%-8d | %-8d | %-22s | %-30s%n", s.left, s.right, nombreToken, lexemaVisual);
            }

            erroresLexicosTotal = lexerPrueba.erroresLexicos;
        } catch (Exception e) {
            System.out.println("Error fatal en fase léxica: " + e.getMessage());
            return;
        }

        System.out.println("--------------------------------------------------------------------------");
        if (!erroresLexicosLista.isEmpty()) {
            System.out.println("[DETALLE DE ERRORES LÉXICOS]");
            for (String err : erroresLexicosLista) {
                System.out.println(" -> " + err);
            }
        } else {
            System.out.println("[ÉXITO LÉXICO] Cero errores léxicos encontrados.");
        }
        System.out.println("Total tokens válidos reconocidos: " + totalTokensValidos);

        // ======================================================================
        // FASE 2: ANÁLISIS SINTÁCTICO (CUP) Y GENERACIÓN DEL AST
        // ======================================================================
        System.out.println("\n==========================================================================");
        System.out.println("                       FASE 2: ANÁLISIS SINTÁCTICO                         ");
        System.out.println("==========================================================================");

        int erroresSintacticosTotal = 0;
        Nodo arbolSintactico = null;
        Nodo.resetearContador();

        if (erroresLexicosTotal > 0) {
            System.out.println("[OMITIDO] No se ejecuta el análisis sintáctico porque existen errores léxicos.");
            System.out.println("          Corrige los errores léxicos para continuar con la compilación.");
        } else {
            try (InputStreamReader readerParser = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
                Lexer lexerSintactico = new Lexer(readerParser);
                parser p = new parser(lexerSintactico);

                Symbol resultado = p.parse();
                erroresSintacticosTotal = p.erroresSintacticos;

                if (resultado != null && resultado.value instanceof Nodo) {
                    arbolSintactico = (Nodo) resultado.value;
                } else {
                    arbolSintactico = p.raizAST;
                }

                if (erroresSintacticosTotal == 0 && arbolSintactico != null) {
                    System.out.println("[ÉXITO SINTÁCTICO] La estructura del programa cumple con la gramática.");
                }
            } catch (Exception e) {
                System.out.println(" -> Error no recuperable en análisis sintáctico: " + e.getMessage());
                erroresSintacticosTotal++;
            }
        }

        // ======================================================================
        // FASE 3: ANÁLISIS SEMÁNTICO Y TABLA DE SÍMBOLOS
        // ======================================================================
        int erroresSemanticosTotal = 0;
        if (erroresLexicosTotal == 0 && erroresSintacticosTotal == 0 && arbolSintactico != null) {
            System.out.println("\n==========================================================================");
            System.out.println("                       FASE 3: ANÁLISIS SEMÁNTICO                          ");
            System.out.println("==========================================================================");

            AnalizadorSemantico semantico = new AnalizadorSemantico();
            semantico.analizar(arbolSintactico);
            erroresSemanticosTotal = semantico.errores.size();

            semantico.tabla.imprimirTabla();
            System.out.println("--------------------------------------------------------------------------");
            if (erroresSemanticosTotal == 0) {
                System.out.println("[ÉXITO SEMÁNTICO] Cero errores semánticos encontrados.");
            } else {
                System.out.println("[DETALLE DE ERRORES SEMÁNTICOS]");
                for (String err : semantico.errores) {
                    System.out.println(" -> " + err);
                }
            }
        }

        // ======================================================================
        // FASE 4: EXPORTACIÓN DEL ÁRBOL SINTÁCTICO (AST)
        // ======================================================================
        if (erroresLexicosTotal == 0 && erroresSintacticosTotal == 0 && arbolSintactico != null) {
            System.out.println("\n==========================================================================");
            System.out.println("                    FASE 4: ÁRBOL SINTÁCTICO (AST)                        ");
            System.out.println("==========================================================================");
            arbolSintactico.imprimirConsola("", true);

            try {
                String archivoDot = "arbol_ast.dot";
                arbolSintactico.exportarDot(archivoDot);
                System.out.println("\n[AST EXPORTADO] Archivo Graphviz generado correctamente: " + archivoDot);
            } catch (Exception ex) {
                System.out.println(" -> No se pudo guardar el archivo .dot: " + ex.getMessage());
            }
        }

        // ======================================================================
        // RESUMEN GLOBAL DE COMPILACIÓN
        // ======================================================================
        System.out.println("\n==========================================================================");
        System.out.println("                        RESUMEN DE COMPILACIÓN                            ");
        System.out.println("==========================================================================");
        System.out.printf("Errores Léxicos detectados    : %d%n", erroresLexicosTotal);
        System.out.printf("Errores Sintácticos detectados: %d%n", erroresSintacticosTotal);
        System.out.printf("Errores Semánticos detectados : %d%n", erroresSemanticosTotal);
        System.out.println("--------------------------------------------------------------------------");

        boolean compilacionExitosa = (erroresLexicosTotal == 0 && erroresSintacticosTotal == 0 && erroresSemanticosTotal == 0);
        if (compilacionExitosa) {
            System.out.println("[RESULTADO FINAL: ÉXITO] El código fue aceptado completamente.");
        } else {
            System.out.println("[RESULTADO FINAL: FALLO] El código contiene errores que impiden la compilación.");
        }
        System.out.println("==========================================================================");

        // ======================================================================
        // FASE 5: EJECUCIÓN DEL PROGRAMA (INTÉRPRETE)
        // ======================================================================
        if (compilacionExitosa && !soloCompilar) {
            System.out.println("\n==========================================================================");
            System.out.println("                      FASE 5: EJECUCIÓN (INTÉRPRETE)                      ");
            System.out.println("==========================================================================");
            try {
                Interprete interprete = new Interprete();
                interprete.ejecutar(arbolSintactico);
                System.out.println("--------------------------------------------------------------------------");
                System.out.println("[EJECUCIÓN FINALIZADA SIN ERRORES]");
            } catch (Interprete.ErrorEjecucion e) {
                System.out.println(" -> Error en tiempo de ejecución: " + e.getMessage());
            }
        }
    }

    private static String diagnosticarErrorLexico(String lexema) {
        if ("/*".equals(lexema)) {
            return "Comentario de bloque no cerrado";
        } else if (lexema.startsWith("\"")) {
            return "Cadena de texto no cerrada";
        } else if ((lexema.startsWith(".") || lexema.endsWith(".")) && lexema.length() > 1) {
            return "Número decimal incompleto";
        } else if (lexema.length() >= 1 && Character.isUpperCase(lexema.charAt(0))) {
            return "Identificador o palabra no permitida (inicia en mayúscula)";
        } else {
            return "Carácter o símbolo no válido";
        }
    }
}
