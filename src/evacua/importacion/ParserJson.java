package evacua.importacion;

import java.util.List;
import java.util.Map;

/**
 * Lee un plano .json:
 * {"nodos":[{"id":"A","tipo":"HABITACION","x":1,"y":2,"bloqueado":false}],
 *  "conexiones":[{"origen":"A","destino":"B","tiempo":2,"capacidad":3}],
 *  "personas":[{"nodo":"A","tipo":"ADULTO","salud":"SANO"}]}
 */
public final class ParserJson {
    private ParserJson() {
    }

    @SuppressWarnings("unchecked")
    public static PlanoDatos parse(String texto) {
        Object raiz = JsonMini.parse(texto);
        if (!(raiz instanceof Map)) throw new IllegalArgumentException("El JSON debe ser un objeto con \"nodos\" y \"conexiones\".");
        Map<String, Object> m = (Map<String, Object>) raiz;
        PlanoDatos d = new PlanoDatos();
        d.cuadricula = "cuadricula".equals(m.get("modo"));
        d.filas = entero(m.get("filas"));
        d.columnas = entero(m.get("columnas"));
        for (Object o : lista(m, "nodos")) {
            Map<String, Object> n = obj(o, "nodos");
            PlanoDatos.NodoDef nd = new PlanoDatos.NodoDef();
            nd.id = texto(n.get("id"));
            nd.tipo = texto(n.get("tipo"));
            nd.x = entero(n.get("x"));
            nd.y = entero(n.get("y"));
            nd.bloqueado = Boolean.TRUE.equals(n.get("bloqueado"));
            d.nodos.add(nd);
        }
        for (Object o : lista(m, "conexiones")) {
            Map<String, Object> n = obj(o, "conexiones");
            PlanoDatos.ConexionDef cd = new PlanoDatos.ConexionDef();
            cd.origen = texto(n.get("origen"));
            cd.destino = texto(n.get("destino"));
            cd.tiempo = n.get("tiempo") instanceof Double ? (Double) n.get("tiempo") : null;
            cd.capacidad = entero(n.get("capacidad"));
            cd.bloqueada = Boolean.TRUE.equals(n.get("bloqueada"));
            d.conexiones.add(cd);
        }
        for (Object o : lista(m, "personas")) {
            Map<String, Object> n = obj(o, "personas");
            PlanoDatos.PersonaDef pd = new PlanoDatos.PersonaDef();
            pd.nodo = texto(n.get("nodo"));
            if (n.get("tipo") != null) pd.tipo = texto(n.get("tipo"));
            if (n.get("salud") != null) pd.salud = texto(n.get("salud"));
            d.personas.add(pd);
        }
        return d;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return new java.util.ArrayList<>();
        if (!(v instanceof List)) throw new IllegalArgumentException("\"" + k + "\" debe ser un arreglo.");
        return (List<Object>) v;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Object o, String k) {
        if (!(o instanceof Map)) throw new IllegalArgumentException("Cada elemento de \"" + k + "\" debe ser un objeto.");
        return (Map<String, Object>) o;
    }

    private static String texto(Object o) {
        if (o == null) return null;
        if (o instanceof Double && ((Double) o) % 1 == 0) return String.valueOf(((Double) o).longValue());
        return String.valueOf(o);
    }

    private static Integer entero(Object o) {
        if (!(o instanceof Double)) return null;
        double d = (Double) o;
        return d % 1 == 0 ? Integer.valueOf((int) d) : null;
    }
}
