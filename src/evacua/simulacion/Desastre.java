package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.Persona;
import java.util.ArrayList;
import java.util.List;

/** RN-18 y RN-20. El estado del desastre pertenece a una única simulación. */
class Desastre {
    void iniciar(MotorSimulacion m) { }
    double velocidad(MotorSimulacion m, Persona p) { return 1; }

    void paso(MotorSimulacion m) {
        if (m.rnd.nextDouble() < m.params.prob) evento(m);
    }

    void evento(MotorSimulacion m) {
        int[] celda = EventoAleatorio.colapsarPasillo(m.mapa, m.rnd);
        if (celda != null) m.alBloquear(celda[0], celda[1]);
    }

    List<Integer> candidatos(MotorSimulacion m) {
        List<Integer> ids = new ArrayList<>();
        for (int f = 0; f < m.mapa.filas; f++)
            for (int c = 0; c < m.mapa.cols; c++)
                if (m.mapa.caminable(f,c)) ids.add(f * m.mapa.cols + c);
        return ids;
    }

    void bloquear(MotorSimulacion m, int id) {
        int f = id / m.mapa.cols, c = id % m.mapa.cols;
        m.mapa.bloquear(f, c, true);
        m.log.accept("t=" + m.tick + " " + m.params.desastre + " bloquea (" + f + "," + c + ")");
        m.alBloquear(f,c);
    }
}
