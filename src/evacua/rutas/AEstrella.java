package evacua.rutas;

import evacua.edificio.Grafo;

import java.util.List;

public class AEstrella extends AlgoritmoRuta {
    @Override
    public String nombre() {
        return "A*";
    }

    @Override
    protected double h(Grafo g, int f, int c, List<int[]> metas) {
        return Heuristica.aTiempo(f, c, metas, g.factorTiempo);
    }
}
