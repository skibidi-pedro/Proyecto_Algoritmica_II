package evacua.rutas;

import evacua.edificio.Edificio;
import evacua.personas.Persona;

import java.util.List;

/** Recalculo dinamico (RN-07): nueva ruta desde la posicion actual de la persona, evitando bloqueos. */
public final class RecalculadorRutas {
    private RecalculadorRutas() {
    }

    public static ResultadoRuta desdePosicion(Edificio e, Persona p, List<int[]> metas, boolean astar) {
        return BuscadorRutas.buscar(e, p.f, p.c, metas, astar);
    }
}
