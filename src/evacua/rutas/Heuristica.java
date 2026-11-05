package evacua.rutas;

import evacua.edificio.Edificio;

import java.util.List;

/** Heuristica de A*: distancia Manhattan a la meta mas cercana, convertida a tiempo (RN-16). */
public class Heuristica {
    private Heuristica() {
    }

    /** Distancia entre dos nodos: Manhattan en el plano del piso + diferencia de pisos (cada piso cuenta 1). */
    public static int distancia(Edificio e, int f1, int c1, int f2, int c2) {
        return Math.abs(f1 % e.filasPorPiso - f2 % e.filasPorPiso) + Math.abs(c1 - c2) + Math.abs(e.piso(f1) - e.piso(f2));
    }

    public static double distanciaMinima(Edificio e, int f, int c, List<int[]> metas) {
        double best = Double.MAX_VALUE;
        for (int[] g : metas) best = Math.min(best, distancia(e, f, c, g[0], g[1]));
        return best == Double.MAX_VALUE ? 0 : best;
    }

    /** Convierte la distancia a tiempo con el menor tiempo por unidad del grafo: nunca sobreestima el costo real. */
    public static double aTiempo(Edificio e, int f, int c, List<int[]> metas, double factorTiempo) {
        return distanciaMinima(e, f, c, metas) * factorTiempo;
    }
}
