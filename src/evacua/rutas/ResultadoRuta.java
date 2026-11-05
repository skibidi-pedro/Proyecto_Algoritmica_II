package evacua.rutas;

import java.util.List;

public class ResultadoRuta {
    public List<int[]> ruta;   // null si no hay camino
    public int explorados;
    public long nanos;
    public double costo;
}
