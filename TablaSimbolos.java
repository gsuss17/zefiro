import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public boolean declarar(Simbolo simbolo) {
        Map<String, Simbolo> actual = ambitos.peek();
        if (actual == null || actual.containsKey(simbolo.nombre)) {
            return false;
        }
        // Estilo Java: no permitir sombrear variables/parámetros de ámbitos contenedores
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
            String tipoMostrar = s.tipo;
            if ("FUNCION".equals(s.categoria)) {
                tipoMostrar = s.tipo + "(" + String.join(", ", s.parametrosTipos) + ")";
            }
            System.out.printf("%-20s | %-10s | %-12s | %-15s | %-6d%n",
                              s.nombre, tipoMostrar, s.categoria, s.ambito, s.linea);
        }
    }
}
