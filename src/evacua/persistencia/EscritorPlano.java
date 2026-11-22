package evacua.persistencia;

import evacua.edificio.*;
import evacua.personas.Persona;
import java.util.StringJoiner;

final class EscritorPlano {
    private final boolean xml;

    EscritorPlano(boolean xml) {
        this.xml = xml;
    }

    private String valor(Object v) {
        String s = v instanceof Enum<?> ? ((Enum<?>) v).name() : v.toString();
        if (xml) return "\"" + s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;") + "\"";
        if (v instanceof Number || v instanceof Boolean) return s;
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") + "\"";
    }

    private String campos(Object... pares) {
        StringJoiner j = new StringJoiner(xml ? " " : ",");
        for (int i = 0; i < pares.length; i += 2)
            j.add((xml ? pares[i] + "=" : valor(pares[i]) + ":") + valor(pares[i + 1]));
        return j.toString();
    }

    private String registro(String nombre, Object... pares) {
        return xml ? "<" + nombre + " " + campos(pares) + "/>" : "{" + campos(pares) + "}";
    }

    private StringJoiner lista(String nombre) {
        return xml ? new StringJoiner("\n", "<" + nombre + ">\n", "\n</" + nombre + ">")
                : new StringJoiner(",\n", valor(nombre) + ":[\n", "\n]");
    }

    String escribir(Edificio e) {
        String cabecera = campos("modo", e.conexionesExplicitas ? "grafo" : "cuadricula",
                "pisos", e.pisos, "filas", e.filasPorPiso, "columnas", e.cols);
        StringJoiner nodos = lista("nodos"), conexiones = lista("conexiones"), personas = lista("personas");
        for (int f = 0; f < e.filas; f++) for (int c = 0; c < e.cols; c++) {
            Nodo n = e.n[f][c];
            if (n.tipo != TipoNodo.VACIO) nodos.add(registro("nodo", "id", e.idDe(f,c), "tipo", n.tipo,
                    "x", c, "y", f % e.filasPorPiso, "piso", e.piso(f) + 1, "bloqueado", n.bloqueado,
                    "danado", n.danado, "peligroso", n.peligroso, "reparable", n.bloqueoReparable));
        }
        for (Conexion k : e.conexiones) conexiones.add(registro("conexion", "origen", e.idDe(k.origen/e.cols,k.origen%e.cols),
                "destino", e.idDe(k.destino/e.cols,k.destino%e.cols), "tiempo", k.tiempo, "capacidad", k.capacidad,
                "condicion", k.condicion, "bloqueada", k.bloqueada));
        for (Persona p : e.personas) personas.add(registro("persona", "nodo", e.idDe(p.f,p.c), "tipo", p.tipo, "salud", p.salud));
        return xml ? "<edificio " + cabecera + ">\n" + nodos + "\n" + conexiones + "\n" + personas + "\n</edificio>"
                : "{" + cabecera + ",\n" + nodos + ",\n" + conexiones + ",\n" + personas + "}";
    }
}
