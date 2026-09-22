import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String archivoFuente = (args.length > 0) ? args[0] : "codigo.zefiro";
        File archivo = new File(archivoFuente);

        if (!archivo.exists()) {
            System.err.println("Error: No se encontró el archivo: " + archivoFuente);
            return;
        }

        int totalTokensValidos = 0;
        List<Token> listaErrores = new ArrayList<>();

        System.out.println("==========================================================================");
        System.out.println("                       ANÁLISIS LÉXICO (ZÉFIRO)                           ");
        System.out.println("==========================================================================");
        System.out.printf("%-8s | %-8s | %-20s | %-30s%n", "LÍNEA", "COLUMNA", "TOKEN", "LEXEMA");
        System.out.println("--------------------------------------------------------------------------");

        try (FileReader reader = new FileReader(archivo)) {
            Lexer lexer = new Lexer(reader);
            Token token;

            while ((token = lexer.yylex()) != null) {
                if ("ERROR".equals(token.getTipo())) {
                    listaErrores.add(token);
                } else {
                    totalTokensValidos++;
                }

                System.out.printf("%-8d | %-8d | %-20s | %-30s%n", 
                    token.getLinea(), 
                    token.getColumna(), 
                    token.getTipo(), 
                    token.getLexema()
                );
            }
            
        } catch (Exception e) {
            System.err.println("Error fatal durante la lectura del archivo: " + e.getMessage());
            return;
        }

        System.out.println("==========================================================================");

        // Despliegue final de resultados y detalle de errores
        if (!listaErrores.isEmpty()) {
            System.out.println("\n==========================================================================");
            System.out.println("                     LISTA DE ERRORES LÉXICOS                             ");
            System.out.println("==========================================================================");
            System.out.printf("%-8s | %-8s | %-50s%n", "LÍNEA", "COLUMNA", "DETALLE");
            System.out.println("--------------------------------------------------------------------------");

            for (Token err : listaErrores) {
                System.out.printf("%-8d | %-8d | Carácter no reconocido: '%s'%n", 
                    err.getLinea(), 
                    err.getColumna(), 
                    err.getLexema()
                );
            }

            System.out.println("--------------------------------------------------------------------------");
            System.err.println("[FALLO] Se detectaron " + listaErrores.size() + " error(es) léxico(s).");
            System.out.println("==========================================================================");
        } else {
            System.out.println("[ÉXITO] Análisis léxico completado sin errores.");
            System.out.println("Total de tokens válidos procesados: " + totalTokensValidos);
            System.out.println("==========================================================================");
        }
    }
}