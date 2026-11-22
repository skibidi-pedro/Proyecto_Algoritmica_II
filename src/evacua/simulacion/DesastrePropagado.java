package evacua.simulacion;

import evacua.edificio.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** La frontera sigue las conexiones del edificio, incluidas las escaleras. */
abstract class DesastrePropagado extends Desastre {
    final Set<Integer> zona = new LinkedHashSet<>();

    @Override void evento(MotorSimulacion m) {
        List<Integer> opciones;
        if (zona.isEmpty()) opciones = candidatos(m);
        else {
            Set<Integer> frontera = new LinkedHashSet<>();
            for (int id : zona)
                for (Grafo.Arista a : m.grafo.ady[id])
                    if (!a.bloqueada() && !zona.contains(a.destino)
                            && m.mapa.caminable(a.destino / m.mapa.cols, a.destino % m.mapa.cols))
                        frontera.add(a.destino);
            opciones = new ArrayList<>(frontera);
        }
        int cantidad = zona.isEmpty() ? 1 : m.params.intensidad;
        for (int i = 0; i < cantidad && !opciones.isEmpty(); i++) {
            int id = opciones.remove(m.rnd.nextInt(opciones.size()));
            zona.add(id);
            afectar(m, id);
        }
    }

    abstract void afectar(MotorSimulacion m, int id);
}
