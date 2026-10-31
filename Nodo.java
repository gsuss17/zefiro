import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Nodo {
    public String etiqueta;
    public String valor;
    public List<Nodo> hijos;
    public int id;
    private static int contadorId = 0;

    public Nodo(String etiqueta) {
        this.etiqueta = (etiqueta != null) ? etiqueta : "NODO";
        this.valor = "";
        this.hijos = new ArrayList<>();
        this.id = ++contadorId;
    }

    public Nodo(String etiqueta, String valor) {
        this.etiqueta = (etiqueta != null) ? etiqueta : "NODO";
        this.valor = (valor != null) ? valor : "";
        this.hijos = new ArrayList<>();
        this.id = ++contadorId;
    }

    public void agregarHijo(Nodo hijo) {
        if (hijo != null) {
            this.hijos.add(hijo);
        }
    }

    // =========================================================================
    // 1. EXPORTACIÓN A GRAPHVIZ (.DOT) CON UTF-8
    // =========================================================================
    public void exportarDot(String rutaArchivo) throws IOException {
        // Se fuerza codificación UTF-8 para garantizar que Graphviz interprete acentos y caracteres especiales
        try (PrintWriter out = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(rutaArchivo), StandardCharsets.UTF_8))) {
            out.println("digraph AST {");
            out.println("    rankdir=TB;");
            out.println("    node [shape=box, style=\"rounded,filled\", fillcolor=\"#EBF5FB\", fontname=\"Consolas\", fontsize=11, color=\"#2874A6\"];");
            out.println("    edge [color=\"#566573\", arrowsize=0.7];");
            generarNodosDot(out);
            out.println("}");
        }
    }

    private void generarNodosDot(PrintWriter out) {
        String textoMostrar = sanitizarTextoDot(etiqueta);
        if (!valor.isEmpty()) {
            textoMostrar += "\\n(" + sanitizarTextoDot(valor) + ")";
        }

        // Color distintivo: azul suave para nodos internos, amarillo claro para hojas con valor
        String colorRelleno = !hijos.isEmpty() ? "#EBF5FB" : "#FEF9E7";
        String colorBorde   = !hijos.isEmpty() ? "#2874A6" : "#B7950B";

        out.printf("    n%d [label=\"%s\", fillcolor=\"%s\", color=\"%s\"];%n",
                   this.id, textoMostrar, colorRelleno, colorBorde);

        for (Nodo hijo : hijos) {
            if (hijo != null) {
                hijo.generarNodosDot(out);
                out.printf("    n%d -> n%d;%n", this.id, hijo.id);
            }
        }
    }

    private String sanitizarTextoDot(String entrada) {
        if (entrada == null) return "";
        return entrada.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\r", "")
                      .replace("\n", "\\n")
                      .replace("\t", "\\t");
    }

    // =========================================================================
    // 2. IMPRESIÓN EN CONSOLA (ASCII)
    // =========================================================================
    public void imprimirConsola(String prefijo, boolean esUltimo) {
        String textoMostrar = etiqueta + (valor.isEmpty() ? "" : ": " + valor);
        System.out.println(prefijo + (esUltimo ? "└── " : "├── ") + textoMostrar);
        for (int i = 0; i < hijos.size(); i++) {
            if (hijos.get(i) != null) {
                hijos.get(i).imprimirConsola(prefijo + (esUltimo ? "    " : "│   "), i == hijos.size() - 1);
            }
        }
    }
}