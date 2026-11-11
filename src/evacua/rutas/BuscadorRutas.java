package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.edificio.Grafo;

import java.util.List;

public class BuscadorRutas {
    private BuscadorRutas() {
    }

    public static ResultadoRuta buscar(Edificio e, int sf, int sc, List<int[]> metas, boolean astar) {
        return buscar(e, sf, sc, metas, astar, null);
    }

    public static ResultadoRuta buscar(Edificio e, int sf, int sc, List<int[]> metas, boolean astar, java.util.function.IntPredicate evitar) {
        AlgoritmoRuta alg = astar ? new AEstrella() : new Dijkstra();
        return alg.buscar(e, Grafo.desde(e), sf, sc, metas, evitar);
    }
}
