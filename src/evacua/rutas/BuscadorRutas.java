package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.edificio.Grafo;

import java.util.List;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;

public class BuscadorRutas {
    private BuscadorRutas() {
    }

    public static ResultadoRuta paraPersona(Edificio e, Persona p, List<int[]> metas, boolean astar,
                                             java.util.function.IntPredicate evitar) {
        AlgoritmoRuta alg = astar ? new AEstrella() : new Dijkstra();
        return alg.buscar(e, Grafo.desde(e), p.f, p.c, metas, evitar, (a,b) -> false, p.tipo == TipoPersona.RESCATE);
    }

    public static ResultadoRuta buscar(Edificio e, int sf, int sc, List<int[]> metas, boolean astar) {
        return buscar(e, sf, sc, metas, astar, null);
    }

    public static ResultadoRuta buscar(Edificio e, int sf, int sc, List<int[]> metas, boolean astar, java.util.function.IntPredicate evitar) {
        AlgoritmoRuta alg = astar ? new AEstrella() : new Dijkstra();
        return alg.buscar(e, Grafo.desde(e), sf, sc, metas, evitar);
    }
}
