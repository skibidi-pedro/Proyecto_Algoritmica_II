package evacua.importacion;

import evacua.edificio.Conexion;
import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.TipoPersona;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Valida un {@link PlanoDatos} y construye el grafo del edificio. Si hay errores no construye nada (RN-15). */
public final class ConstructorEdificio {
    private ConstructorEdificio() {
    }

    public static Edificio construir(PlanoDatos d) {
        List<String> err = ValidadorPlano.validar(d);
        if (!err.isEmpty()) {
            StringBuilder sb = new StringBuilder("El plano tiene " + err.size() + " error(es):");
            for (String e : err) sb.append("\n - ").append(e);
            throw new IllegalArgumentException(sb.toString());
        }
        int maxX = 0, maxY = 0;
        for (PlanoDatos.NodoDef n : d.nodos) {
            maxX = Math.max(maxX, n.x);
            maxY = Math.max(maxY, n.y);
        }
        Edificio e = new Edificio(maxY + 1, maxX + 1);
        Map<String, int[]> pos = new HashMap<>();
        for (PlanoDatos.NodoDef n : d.nodos) {
            e.crearNodo(ValidadorPlano.tipo(n.tipo), n.y, n.x, n.id);
            e.n[n.y][n.x].bloqueado = n.bloqueado;
            pos.put(n.id, new int[]{n.y, n.x});
        }
        e.conexionesExplicitas = true;
        for (PlanoDatos.ConexionDef c : d.conexiones) {
            int[] a = pos.get(c.origen), b = pos.get(c.destino);
            e.conexiones.removeIf(k -> k.origen == a[0] * e.cols + a[1] && k.destino == b[0] * e.cols + b[1]);
            e.conexiones.add(new Conexion("C" + c.origen + "_" + c.destino, a[0] * e.cols + a[1], b[0] * e.cols + b[1], c.tiempo, c.capacidad));
        }
        for (PlanoDatos.PersonaDef p : d.personas) {
            int[] a = pos.get(p.nodo);
            e.agregar(TipoPersona.valueOf(p.tipo), EstadoSalud.valueOf(p.salud), a[0], a[1]);
        }
        return e;
    }
}
