package evacua.importacion;

import java.util.ArrayList;
import java.util.List;

/** Contenido leido de un plano (.json o .xml), antes de validar y construir el edificio. */
public class PlanoDatos {
    public static class NodoDef {
        public String id, tipo;
        public Integer x, y;       // x = columna, y = fila (dentro del piso)
        public Integer piso = 1;   // desde 1
        public boolean bloqueado;
    }

    public static class ConexionDef {
        public String origen, destino;
        public Double tiempo;
        public Integer capacidad;
        public boolean bloqueada;
    }

    public static class PersonaDef {
        public String nodo, tipo = "ADULTO", salud = "SANO";
    }

    /** Si es true, el grafo usa la adyacencia de la cuadricula y las conexiones solo la ajustan. */
    public boolean cuadricula;
    /** Tamano opcional del plano (si falta, se usa el minimo que contiene a todos los nodos). */
    public Integer filas, columnas, pisos;
    public final List<NodoDef> nodos = new ArrayList<>();
    public final List<ConexionDef> conexiones = new ArrayList<>();
    public final List<PersonaDef> personas = new ArrayList<>();
}
