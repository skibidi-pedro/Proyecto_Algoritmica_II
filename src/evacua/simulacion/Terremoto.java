package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.Persona;
import java.util.List;

class Terremoto extends Desastre {
    @Override void iniciar(MotorSimulacion m) {
        for (Persona p : m.mapa.personas) p.aumentarEstres(4 * m.params.intensidad);
    }

    @Override double velocidad(MotorSimulacion m, Persona p) { return 0.8; }

    @Override void evento(MotorSimulacion m) {
        List<Integer> opciones = candidatos(m);
        opciones.removeIf(id -> {
            TipoNodo t = m.mapa.n[id / m.mapa.cols][id % m.mapa.cols].tipo;
            return t != TipoNodo.PASILLO && t != TipoNodo.ESCALERA;
        });
        for (int i = 0; i < m.params.intensidad && !opciones.isEmpty(); i++)
            bloquear(m, opciones.remove(m.rnd.nextInt(opciones.size())));
    }
}
