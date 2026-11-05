package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.edificio.Grafo;

import java.util.List;

/** Fachada: construye el grafo del edificio y ejecuta Dijkstra o A*. */
public class BuscadorRutas {
    private BuscadorRutas() {
    }

    public static ResultadoRuta buscar(Edificio e, int sf, int sc, List<int[]> metas, boolean astar) {
        AlgoritmoRuta alg = astar ? new AEstrella() : new Dijkstra();
        return alg.buscar(e, Grafo.desde(e), sf, sc, metas);
    }
}
