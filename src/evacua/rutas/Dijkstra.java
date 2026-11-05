package evacua.rutas;

import evacua.edificio.Grafo;

import java.util.List;

public class Dijkstra extends AlgoritmoRuta {
    @Override
    public String nombre() {
        return "Dijkstra";
    }

    @Override
    protected double h(Grafo g, int f, int c, List<int[]> metas) {
        return 0;
    }
}
