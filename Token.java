public class Token {
    private String tipo;
    private String lexema;
    private int linea;
    private int columna;

    public Token(String tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public String getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    @Override
    public String toString() {
        return String.format("<%-16s | %-18s | Línea: %-3d | Col: %-3d>", 
            tipo, "'" + lexema + "'", linea, columna);
    }
}