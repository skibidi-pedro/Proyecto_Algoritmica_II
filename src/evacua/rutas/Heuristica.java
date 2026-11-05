package evacua.rutas;

import java.util.List;

/** Heuristica de A*: distancia Manhattan a la meta mas cercana, convertida a tiempo (RN-16). */
public class Heuristica {
    private Heuristica() {
    }

    public static double distanciaMinima(int f, int c, List<int[]> metas) {
        double best = Double.MAX_VALUE;
        for (int[] g : metas) best = Math.min(best, Math.abs(g[0] - f) + Math.abs(g[1] - c));
        return best == Double.MAX_VALUE ? 0 : best;
    }

    /** Convierte la distancia a tiempo con el menor tiempo por unidad del grafo: nunca sobreestima el costo real. */
    public static double aTiempo(int f, int c, List<int[]> metas, double factorTiempo) {
        return distanciaMinima(f, c, metas) * factorTiempo;
    }
}
