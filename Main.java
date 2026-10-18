import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java_cup.runtime.Symbol;

public class Main {

    public static void main(String[] args) {
        String archivoFuente = (args.length > 0) ? args[0] : "codigo.zefiro";
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

        try (FileReader readerLex = new FileReader(archivo)) {
            Lexer lexerPrueba = new Lexer(readerLex);
            Symbol s;

            while ((s = lexerPrueba.next_token()) != null && s.sym != sym.EOF) {
                String lexema = (s.value != null) ? s.value.toString() : "";
                String nombreToken;

                if (s.sym == sym.error) {
                    nombreToken = "ERROR_LEXICO";
                    String motivo = diagnosticarErrorLexico(lexema);
                    erroresLexicosLista.add(String.format("Línea %-3d | Columna %-3d | %s: '%s'", 
                                            s.left, s.right, motivo, lexema));
                } else {
                    nombreToken = (s.sym >= 0 && s.sym < sym.terminalNames.length) 
                                  ? sym.terminalNames[s.sym] 
                                  : ("TOKEN_" + s.sym);
                    totalTokensValidos++;
                }

                System.out.printf("%-8d | %-8d | %-22s | %-30s%n", s.left, s.right, nombreToken, lexema);
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
        // FASE 2: ANÁLISIS SINTÁCTICO (CUP)
        // ======================================================================
        System.out.println("\n==========================================================================");
        System.out.println("                       FASE 2: ANÁLISIS SINTÁCTICO                        ");
        System.out.println("==========================================================================");

        int erroresSintacticosTotal = 0;

        try (FileReader readerParser = new FileReader(archivo)) {
            Lexer lexerSintactico = new Lexer(readerParser);
            parser p = new parser(lexerSintactico);

            p.parse();
            erroresSintacticosTotal = p.erroresSintacticos;

            if (erroresSintacticosTotal == 0) {
                System.out.println("[ÉXITO SINTÁCTICO] La estructura del programa cumple con la gramática.");
            }

        } catch (Exception e) {
            System.out.println(" -> Error no recuperable en análisis sintáctico: " + e.getMessage());
            erroresSintacticosTotal++;
        }

        // ======================================================================
        // RESUMEN GLOBAL DE COMPILACIÓN
        // ======================================================================
        System.out.println("\n==========================================================================");
        System.out.println("                         RESUMEN DE COMPILACIÓN                           ");
        System.out.println("==========================================================================");
        System.out.printf("Errores Léxicos detectados    : %d%n", erroresLexicosTotal);
        System.out.printf("Errores Sintácticos detectados: %d%n", erroresSintacticosTotal);
        System.out.println("--------------------------------------------------------------------------");

        if (erroresLexicosTotal == 0 && erroresSintacticosTotal == 0) {
            System.out.println("[RESULTADO FINAL: ÉXITO] El código fue aceptado completamente.");
        } else {
            System.out.println("[RESULTADO FINAL: FALLO] El código contiene errores que impiden la compilación.");
        }
        System.out.println("==========================================================================");
    }

    private static String diagnosticarErrorLexico(String lexema) {
        if (lexema.startsWith("\"")) {
            return "Cadena de texto no cerrada";
        } else if (lexema.endsWith(".") && lexema.length() > 1) {
            return "Número decimal incompleto";
        } else if (lexema.length() >= 1 && Character.isUpperCase(lexema.charAt(0))) {
            return "Identificador o palabra no permitida (inicia en mayúscula)";
        } else {
            return "Carácter o símbolo no válido";
        }
    }
}