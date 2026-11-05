package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/** Dijkstra y A* sobre la cuadricula (A* si el parametro astar es true). */
public class BuscadorRutas {
    private BuscadorRutas() {
    }

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static ResultadoRuta buscar(Edificio m, int sf, int sc, List<int[]> metas, boolean astar) {
        long t0 = System.nanoTime();
        int C = m.cols;
        int total = m.filas * m.cols;
        double[] dist = new double[total];
        Arrays.fill(dist, Double.MAX_VALUE);
        int[] prev = new int[total];
        Arrays.fill(prev, -1);
        boolean[] cerrado = new boolean[total];
        boolean[] esMeta = new boolean[total];
        List<int[]> validas = new ArrayList<>();
        for (int[] g : metas)
            if (m.caminable(g[0], g[1])) {
                esMeta[g[0] * C + g[1]] = true;
                validas.add(g);
            }
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        int ini = sf * C + sc;
        dist[ini] = 0;
        pq.add(new double[]{astar ? Heuristica.distanciaMinima(sf, sc, validas) : 0, ini});
        int explorados = 0, fin = -1;
        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int id = (int) cur[1];
            if (cerrado[id]) continue;
            cerrado[id] = true;
            explorados++;
            if (esMeta[id]) {
                fin = id;
                break;
            }
            int f = id / C, c = id % C;
            for (int[] d : DIRS) {
                int nf = f + d[0], nc = c + d[1];
                if (!m.caminable(nf, nc)) continue;
                int nid = nf * C + nc;
                double costo = m.n[nf][nc].tipo == TipoNodo.ESCALERA ? 3 : 1;
                double nd = dist[id] + costo;
                if (nd < dist[nid]) {
                    dist[nid] = nd;
                    prev[nid] = id;
                    pq.add(new double[]{nd + (astar ? Heuristica.distanciaMinima(nf, nc, validas) : 0), nid});
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
