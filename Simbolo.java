import java.util.ArrayList;
import java.util.List;

public class Simbolo {
    public String nombre;
    public String tipo;
    public String categoria; // VARIABLE, PARAMETRO, FUNCION
    public String ambito;
    public int linea;
    public boolean inicializada;
    public List<String> parametrosTipos;

    public Simbolo(String nombre, String tipo, String categoria, String ambito, int linea, boolean inicializada) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.ambito = ambito;
        this.linea = linea;
        this.inicializada = inicializada;
        this.parametrosTipos = new ArrayList<>();
    }
}
