import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Nodo {
    public String etiqueta;
    public String valor;
    public List<Nodo> hijos;
    public int id;
    private static int contadorId = 0;

    public Nodo(String etiqueta) {
        this.etiqueta = etiqueta;
        this.valor = "";
        this.hijos = new ArrayList<>();
        this.id = ++contadorId;
    }

    public Nodo(String etiqueta, String valor) {
        this.etiqueta = etiqueta;
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
    // 1. EXPORTACIÓN A GRAPHVIZ (.DOT)
    // =========================================================================
    public void exportarDot(String rutaArchivo) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(rutaArchivo))) {
            out.println("digraph AST {");
            out.println("    rankdir=TB;");
            out.println("    node [shape=box, style=\"rounded,filled\", fillcolor=\"#EBF5FB\", fontname=\"Consolas\", fontsize=11, color=\"#2874A6\"];");
            out.println("    edge [color=\"#566573\", arrowsize=0.7];");
            generarNodosDot(out);
            out.println("}");
        }
    }

    private void generarNodosDot(PrintWriter out) {
        String textoMostrar = etiqueta;
        if (!valor.isEmpty()) {
            String valEscapado = valor.replace("\\", "\\\\").replace("\"", "\\\"");
            textoMostrar += "\\n(" + valEscapado + ")";
        }

        // Color distintivo para hojas (terminales con valor) vs nodos internos
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