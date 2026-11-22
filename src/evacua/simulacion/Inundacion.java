package evacua.simulacion;

import evacua.edificio.*;
import java.util.List;

class Inundacion extends Desastre {
    int pisoAfectado = -1;

    @Override void evento(MotorSimulacion m) {
        List<Integer> opciones = candidatos(m);
        if (opciones.isEmpty()) return;
        int piso = m.mapa.piso(opciones.get(0) / m.mapa.cols);
        pisoAfectado = piso;
        opciones.removeIf(id -> m.mapa.piso(id / m.mapa.cols) != piso);
        for (int i = 0; i < m.params.intensidad && !opciones.isEmpty(); i++)
            bloquear(m, opciones.remove(m.rnd.nextInt(opciones.size())));
    }
}
