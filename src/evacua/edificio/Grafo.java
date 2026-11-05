package evacua.edificio;

import evacua.rutas.Heuristica;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafo del edificio. Nodo = fila * cols + col. Si el edificio no tiene conexiones explicitas,
 * cada nodo se conecta con sus vecinos de la cuadricula (escalera = tiempo 3, resto = 1).
 * Las conexiones personalizadas reemplazan o agregan aristas. Los bloqueos se leen en vivo.
 */
public class Grafo {
    public static final int CAPACIDAD_DEFECTO = 10;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static class Arista {
        public final int destino;
        public final double tiempo;
        public final int capacidad;
        final Conexion conexion; // null si es una arista de cuadricula

        Arista(int destino, double tiempo, int capacidad, Conexion conexion) {
            this.destino = destino;
            this.tiempo = tiempo;
            this.capacidad = capacidad;
            this.conexion = conexion;
        }

        public boolean bloqueada() {
            return conexion != null && conexion.bloqueada;
        }
    }

    public final Edificio edificio;
    public final int cols;
    public final List<Arista>[] ady;
    /** Tiempo minimo por unidad de distancia Manhattan: convierte la heuristica a tiempo sin sobreestimar (RN-16). */
    public final double factorTiempo;

    @SuppressWarnings("unchecked")
    private Grafo(Edificio e) {
        this.edificio = e;
        this.cols = e.cols;
        ady = new List[e.filas * e.cols];
        for (int i = 0; i < ady.length; i++) ady[i] = new ArrayList<>();
        if (!e.conexionesExplicitas) {
            for (int f = 0; f < e.filas; f++)
                for (int c = 0; c < e.cols; c++) {
                    if (e.n[f][c].tipo == TipoNodo.VACIO) continue;
                    for (int[] d : DIRS) {
                        int nf = f + d[0], nc = c + d[1];
                        if (!e.dentro(nf, nc) || e.n[nf][nc].tipo == TipoNodo.VACIO || e.piso(nf) != e.piso(f)) continue;
                        double t = e.n[nf][nc].tipo == TipoNodo.ESCALERA ? 3 : 1;
                        ady[f * cols + c].add(new Arista(nf * cols + nc, t, CAPACIDAD_DEFECTO, null));
                    }
                }
            // escaleras: unen la misma (fila, col) de pisos consecutivos
            for (int f = 0; f + e.filasPorPiso < e.filas; f++)
                for (int c = 0; c < e.cols; c++)
                    if (e.n[f][c].tipo == TipoNodo.ESCALERA && e.n[f + e.filasPorPiso][c].tipo == TipoNodo.ESCALERA) {
                        int a = f * cols + c, b = (f + e.filasPorPiso) * cols + c;
                        ady[a].add(new Arista(b, 3, CAPACIDAD_DEFECTO, null));
                        ady[b].add(new Arista(a, 3, CAPACIDAD_DEFECTO, null));
                    }
        }
        for (Conexion k : e.conexiones) {
            ady[k.origen].removeIf(a -> a.destino == k.destino);
            ady[k.destino].removeIf(a -> a.destino == k.origen);
            ady[k.origen].add(new Arista(k.destino, k.tiempo, k.capacidad, k));
            ady[k.destino].add(new Arista(k.origen, k.tiempo, k.capacidad, k));
        }
        double fac = Double.MAX_VALUE;
        for (int a = 0; a < ady.length; a++)
            for (Arista ar : ady[a]) {
                int dist = Heuristica.distancia(e, a / cols, a % cols, ar.destino / cols, ar.destino % cols);
                if (dist > 0) fac = Math.min(fac, ar.tiempo / dist);
            }
        factorTiempo = fac == Double.MAX_VALUE ? 1 : fac;
    }

    public static Grafo desde(Edificio e) {
        return new Grafo(e);
    }

    public int total() {
        return ady.length;
    }
}
