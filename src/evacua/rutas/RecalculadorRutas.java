package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.personas.Persona;

import java.util.List;

public final class RecalculadorRutas {
    private RecalculadorRutas() {
    }

    public static ResultadoRuta evitandoCongestion(Edificio e, Persona p, List<int[]> metas, boolean astar, int capacidad) {
        java.util.function.IntPredicate lleno = id -> {
            evacua.edificio.Nodo n = e.n[id / e.cols][id % e.cols];
            return n.ocupacion + p.tipo.ocupacion() > n.capacidadEfectiva(capacidad);
        };
        return BuscadorRutas.paraPersona(e, p, metas, astar, lleno);
    }

    public static ResultadoRuta desdePosicion(Edificio e, Persona p, List<int[]> metas, boolean astar) {
        return BuscadorRutas.paraPersona(e, p, metas, astar, null);
    }
}
