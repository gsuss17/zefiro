import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla de Símbolos con pila de ámbitos anidados y registro histórico
 * para su impresión al finalizar el análisis semántico.
 */
public class TablaSimbolos {
    private final Deque<Map<String, Simbolo>> ambitos = new ArrayDeque<>();
    private final Deque<String> nombresAmbitos = new ArrayDeque<>();
    private final List<Simbolo> historial = new ArrayList<>();

    public TablaSimbolos() {
        entrarAmbito("global");
    }

    public void entrarAmbito(String nombre) {
        ambitos.push(new LinkedHashMap<>());
        nombresAmbitos.push(nombre);
    }

    public void salirAmbito() {
        if (ambitos.size() > 1) {
            ambitos.pop();
            nombresAmbitos.pop();
        }
    }

    public String ambitoActual() {
        return nombresAmbitos.peek();
    }

    /**
     * Registra un símbolo en el ámbito actual.
     * Siguiendo las reglas de Java, impide redeclarar variables o parámetros
     * que ya existan en ámbitos contenedores visibles.
     */
    public boolean declarar(Simbolo simbolo) {
        Map<String, Simbolo> actual = ambitos.peek();
        if (actual == null || actual.containsKey(simbolo.nombre)) {
            return false;
        }
        if (!"FUNCION".equals(simbolo.categoria)) {
            Simbolo existente = buscar(simbolo.nombre);
            if (existente != null && !"FUNCION".equals(existente.categoria)) {
                return false;
            }
        }
        actual.put(simbolo.nombre, simbolo);
        historial.add(simbolo);
        return true;
    }

    public Simbolo buscar(String nombre) {
        for (Map<String, Simbolo> ambito : ambitos) {
            if (ambito.containsKey(nombre)) {
                return ambito.get(nombre);
            }
        }
        return null;
    }

    public List<Simbolo> obtenerHistorial() {
        return historial;
    }

    public void imprimirTabla() {
        System.out.printf("%-20s | %-10s | %-12s | %-15s | %-6s%n",
                          "IDENTIFICADOR", "TIPO", "CATEGORÍA", "ÁMBITO", "LÍNEA");
        System.out.println("--------------------------------------------------------------------------");
        for (Simbolo s : historial) {
            String tipoMostrar = "FUNCION".equals(s.categoria)
                                 ? s.tipo + "(" + String.join(", ", s.parametrosTipos) + ")"
                                 : s.tipo;
            System.out.printf("%-20s | %-10s | %-12s | %-15s | %-6d%n",
                              s.nombre, tipoMostrar, s.categoria, s.ambito, s.linea);
        }
    }
}
