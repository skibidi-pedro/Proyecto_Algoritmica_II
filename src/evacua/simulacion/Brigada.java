package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import evacua.rutas.ResultadoRuta;
import java.util.*;

/** Las reservas duran un turno; un transporte ya iniciado conserva sus portadores. */
final class Brigada {
    private final MotorSimulacion m;
    private final Set<Persona> reservadas = new HashSet<>();
    private final Set<Bombero> asignados = new HashSet<>();

    Brigada(MotorSimulacion m) { this.m = m; }

    void planificar() {
        reservadas.clear();
        m.intervenciones.clear();
        asignados.clear();
        List<Bombero> libres = new ArrayList<>();
        for (Persona p : m.mapa.personas) {
            if (p instanceof Bombero) {
                Bombero b = (Bombero)p;
                b.foco = -1;
                b.objetivo = null;
                if (b.activa() && !b.retirado && !b.transportando && !b.asistida) libres.add(b);
            } else if (p instanceof PersonalRescate && p.activa() && p.objetivo != null) reservadas.add(p.objetivo);
        }
        for (int prioridad = 1; prioridad <= 4; prioridad++) {
            for (Bombero b : libres) {
                if (asignados.contains(b)) continue;
                if (prioridad == 3) asignarFoco(b);
                else asignarPaciente(b, libres, prioridad);
            }
        }
    }

    private void asignarPaciente(Bombero b, List<Bombero> libres, int prioridad) {
        Persona mejor = null;
        Bombero apoyo = null;
        double costo = Double.POSITIVE_INFINITY;
        for (Persona q : m.mapa.personas) {
            if (reservadas.contains(q) || b.prioridad(q, m.mapa.n[q.f][q.c]) != prioridad) continue;
            if (q instanceof PersonalRescate && ((PersonalRescate)q).transportando) continue;
            if (!m.rescateAlcanzable(b, q)) continue;
            ResultadoRuta ida = m.evaluarRuta(b, m.metasRescate(b, q), m.params.astar);
            if (ida.ruta == null || ida.costo >= costo) continue;
            Bombero segundo = null;
            if (q.tipo == TipoPersona.MOVILIDAD) {
                for (Bombero otro : libres)
                    if (otro != b && !asignados.contains(otro) && m.rescateAlcanzable(otro, q)) { segundo = otro; break; }
                if (segundo == null) continue;
            }
            costo = ida.costo;
            mejor = q;
            apoyo = segundo;
        }
        if (mejor == null) return;
        b.objetivo = mejor;
        reservadas.add(mejor);
        asignados.add(b);
        if (apoyo != null) {
            apoyo.objetivo = mejor;
            asignados.add(apoyo);
        }
    }

    List<int[]> posiciones(int foco) {
        int c = m.mapa.cols;
        List<int[]> metas = new ArrayList<>();
        metas.add(new int[]{foco / c, foco % c});
        for (Grafo.Arista a : m.grafo.ady[foco]) {
            Nodo vecino = m.mapa.n[a.destino / c][a.destino % c];
            if (!a.bloqueada() && vecino.combustible() && m.mapa.caminable(a.destino / c, a.destino % c, true))
                metas.add(new int[]{a.destino / c, a.destino % c});
        }
        return metas;
    }

    private void asignarFoco(Bombero b) {
        Map<Integer,Integer> focoPorPosicion = new HashMap<>();
        List<int[]> metas = new ArrayList<>();
        for (int id = 0; id < m.grafo.total(); id++) {
            if (!m.mapa.n[id / m.mapa.cols][id % m.mapa.cols].fuegoActivo || m.intervenciones.containsKey(id)) continue;
            for (int[] q : posiciones(id)) {
                if (focoPorPosicion.putIfAbsent(q[0] * m.mapa.cols + q[1], id) == null) metas.add(q);
            }
        }
        if (metas.isEmpty()) return;
        ResultadoRuta ruta = m.evaluarRuta(b, metas, m.params.astar);
        if (ruta.ruta == null) return;
        int[] fin = ruta.ruta.isEmpty() ? new int[]{b.f, b.c} : ruta.ruta.get(ruta.ruta.size() - 1);
        b.foco = focoPorPosicion.get(fin[0] * m.mapa.cols + fin[1]);
        m.intervenciones.put(b.foco,b);
        asignados.add(b);
    }

    boolean extinguir(Bombero b) {
        if (b.foco < 0 || b.retirado || b.transportando) return false;
        for (int[] q : posiciones(b.foco)) if (q[0] == b.f && q[1] == b.c) {
            Nodo n = m.emergencia().incendio.nodos[b.foco];
            if (n.fuegoActivo && ++n.extincion == 2) {
                m.emergencia().incendio.apagar(b.foco, true);
                b.foco = -1;
            }
            return true;
        }
        return false;
    }
}
