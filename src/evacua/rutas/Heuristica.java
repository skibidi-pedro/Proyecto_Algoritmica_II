package evacua.rutas;

import java.util.List;

/** Heuristica de A*: distancia Manhattan a la meta mas cercana. */
public class Heuristica {
    private Heuristica() {
    }

    public static double distanciaMinima(int f, int c, List<int[]> metas) {
        double best = Double.MAX_VALUE;
        for (int[] g : metas) best = Math.min(best, Math.abs(g[0] - f) + Math.abs(g[1] - c));
        return best == Double.MAX_VALUE ? 0 : best;
    }
}
