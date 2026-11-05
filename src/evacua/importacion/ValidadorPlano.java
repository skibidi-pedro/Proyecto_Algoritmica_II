package evacua.importacion;

import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.TipoPersona;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Valida un plano antes de construir el grafo (RN-15). Devuelve TODOS los errores encontrados. */
public final class ValidadorPlano {
    private ValidadorPlano() {
    }

    public static List<String> validar(PlanoDatos d) {
        List<String> err = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Map<String, String> celdas = new HashMap<>();
        boolean haySalida = false;
        if (d.nodos.isEmpty()) err.add("El plano no define ningun nodo.");
        int k = 0;
        for (PlanoDatos.NodoDef n : d.nodos) {
            k++;
            String ref = "Nodo #" + k + (n.id != null ? " (" + n.id + ")" : "");
            if (n.id == null || n.id.isEmpty()) err.add(ref + ": falta el identificador.");
            else if (!ids.add(n.id)) err.add(ref + ": identificador repetido.");
            TipoNodo t = n.tipo == null ? null : tipo(n.tipo);
            if (t == null) err.add(ref + ": tipo invalido (use HABITACION, PASILLO, ESCALERA o SALIDA).");
            else if (t == TipoNodo.SALIDA) haySalida = true;
            if (n.x == null || n.y == null || n.x < 0 || n.y < 0) err.add(ref + ": coordenadas (x, y) invalidas; deben ser enteros >= 0.");
            else if (celdas.put(n.x + "," + n.y, n.id) != null) err.add(ref + ": la celda (" + n.x + "," + n.y + ") ya esta ocupada por otro nodo.");
        }
        if (!d.nodos.isEmpty() && !haySalida) err.add("El plano debe tener al menos una salida.");
        k = 0;
        for (PlanoDatos.ConexionDef c : d.conexiones) {
            k++;
            String ref = "Conexion #" + k;
            if (c.origen == null || !ids.contains(c.origen)) err.add(ref + ": el origen '" + c.origen + "' no es un nodo existente.");
            if (c.destino == null || !ids.contains(c.destino)) err.add(ref + ": el destino '" + c.destino + "' no es un nodo existente.");
            if (c.origen != null && c.origen.equals(c.destino)) err.add(ref + ": no puede unir un nodo consigo mismo.");
            if (c.tiempo == null || c.tiempo <= 0) err.add(ref + ": el peso (tiempo de recorrido) debe ser mayor a cero.");
            if (c.capacidad == null || c.capacidad < 1) err.add(ref + ": falta la capacidad maxima (entero >= 1).");
        }
        k = 0;
        for (PlanoDatos.PersonaDef p : d.personas) {
            k++;
            String ref = "Persona #" + k;
            if (p.nodo == null || !ids.contains(p.nodo)) err.add(ref + ": el nodo '" + p.nodo + "' no existe.");
            try {
                TipoPersona.valueOf(p.tipo);
            } catch (RuntimeException ex) {
                err.add(ref + ": tipo '" + p.tipo + "' invalido.");
            }
            try {
                EstadoSalud.valueOf(p.salud);
            } catch (RuntimeException ex) {
                err.add(ref + ": salud '" + p.salud + "' invalida.");
            }
        }
        return err;
    }

    static TipoNodo tipo(String s) {
        for (TipoNodo t : TipoNodo.values())
            if (t != TipoNodo.VACIO && t.name().equalsIgnoreCase(s.trim())) return t;
        return null;
    }
}
