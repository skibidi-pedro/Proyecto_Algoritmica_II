package evacua.importacion;

import java.util.ArrayList;
import java.util.List;

/** Contenido leido de un plano (.json o .xml), antes de validar y construir el edificio. */
public class PlanoDatos {
    public static class NodoDef {
        public String id, tipo;
        public Integer x, y;       // x = columna, y = fila
        public boolean bloqueado;
    }

    public static class ConexionDef {
        public String origen, destino;
        public Double tiempo;
        public Integer capacidad;
    }

    public static class PersonaDef {
        public String nodo, tipo = "ADULTO", salud = "SANO";
    }

    public final List<NodoDef> nodos = new ArrayList<>();
    public final List<ConexionDef> conexiones = new ArrayList<>();
    public final List<PersonaDef> personas = new ArrayList<>();
}
