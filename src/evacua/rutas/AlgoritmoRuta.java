package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.edificio.Grafo;
import evacua.estructuras.ColaPrioridad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Busqueda de camino minimo en el grafo con cola de prioridad (heap propio).
 * Excluye nodos y conexiones bloqueados (RN-04, RN-05). Dijkstra usa h = 0; A* usa la heuristica.
 */
public abstract class AlgoritmoRuta {
    public abstract String nombre();

    /** Estimacion del costo restante desde (f, c) hasta la meta mas cercana. */
    protected abstract double h(Grafo g, int f, int c, List<int[]> metas);

    public ResultadoRuta buscar(Edificio e, Grafo g, int sf, int sc, List<int[]> metas) {
        return buscar(e, g, sf, sc, metas, null);
    }

    /** Igual que {@link #buscar(Edificio, Grafo, int, int, List)} pero sin pasar por los nodos que cumplan {@code evitar} (indice de nodo). */
    public ResultadoRuta buscar(Edificio e, Grafo g, int sf, int sc, List<int[]> metas, java.util.function.IntPredicate evitar) {
        long t0 = System.nanoTime();
        int C = g.cols, total = g.total();
        double[] dist = new double[total];
        Arrays.fill(dist, Double.MAX_VALUE);
        int[] prev = new int[total];
        Arrays.fill(prev, -1);
        boolean[] cerrado = new boolean[total];
        boolean[] esMeta = new boolean[total];
        List<int[]> validas = new ArrayList<>();
        for (int[] m : metas)
            if (e.caminable(m[0], m[1])) {
                esMeta[m[0] * C + m[1]] = true;
                validas.add(m);
            }
        ColaPrioridad<double[]> pq = new ColaPrioridad<>((a, b) -> Double.compare(a[0], b[0]));
        int ini = sf * C + sc;
        dist[ini] = 0;
        pq.agregar(new double[]{h(g, sf, sc, validas), ini});
        int explorados = 0, fin = -1;
        while (!pq.estaVacia()) {
            double[] cur = pq.sacar();
            int id = (int) cur[1];
            if (cerrado[id]) continue;
            cerrado[id] = true;
            explorados++;
            if (esMeta[id]) {
                fin = id;
                break;
            }
            for (Grafo.Arista a : g.ady[id]) {
                int nid = a.destino;
                if (a.bloqueada() || !e.caminable(nid / C, nid % C) || (evitar != null && evitar.test(nid))) continue;
                double nd = dist[id] + a.tiempo;
                if (nd < dist[nid]) {
                    dist[nid] = nd;
                    prev[nid] = id;
                    pq.agregar(new double[]{nd + h(g, nid / C, nid % C, validas), nid});
                }
            }
        }
        ResultadoRuta r = new ResultadoRuta();
        r.explorados = explorados;
        if (fin >= 0) {
            LinkedList<int[]> path = new LinkedList<>();
            for (int x = fin; x != ini; x = prev[x]) path.addFirst(new int[]{x / C, x % C});
            r.ruta = path;
            r.costo = dist[fin];
        }
        r.nanos = System.nanoTime() - t0;
        return r;
    }
}
