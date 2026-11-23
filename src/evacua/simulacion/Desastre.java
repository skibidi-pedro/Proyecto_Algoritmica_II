package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import java.util.ArrayList;
import java.util.List;

/** Estado de una emergencia, desde el tick cero hasta el fin de una ejecución. */
public class Desastre {
    private final TipoDesastre tipo;
    private final Intensidad intensidad;
    private final int origen;
    private int llegada = -1;
    private boolean confusionAsignada;

    Desastre(MotorSimulacion m) {
        ParametrosSimulacion p = m.params;
        tipo = p.desastre;
        intensidad = p.nivel;
        if (tipo == null || intensidad == null) throw new IllegalArgumentException("Seleccione desastre e intensidad.");
        if (!tipo.requiereOrigen()) origen = -1;
        else if (p.origenManual) {
            if (p.pisoOrigen < 0 || p.pisoOrigen >= m.mapa.pisos || p.filaOrigen < 0
                    || p.filaOrigen >= m.mapa.filasPorPiso || p.colOrigen < 0 || p.colOrigen >= m.mapa.cols)
                throw new IllegalArgumentException("Seleccione un origen válido en el plano.");
            int f = p.pisoOrigen * m.mapa.filasPorPiso + p.filaOrigen;
            if (m.mapa.n[f][p.colOrigen].tipo == TipoNodo.VACIO) throw new IllegalArgumentException("El origen está vacío.");
            origen = f * m.mapa.cols + p.colOrigen;
        } else {
            List<Integer> nodos = new ArrayList<>();
            for (int f = 0; f < m.mapa.filas; f++) for (int c = 0; c < m.mapa.cols; c++)
                if (m.mapa.n[f][c].tipo != TipoNodo.VACIO) nodos.add(f * m.mapa.cols + c);
            if (nodos.isEmpty()) throw new IllegalArgumentException("El plano no tiene un origen válido.");
            origen = nodos.get(m.rnd.nextInt(nodos.size()));
        }
        if (tipo == TipoDesastre.TERREMOTO) llegada = intensidad.demora(tipo);
    }

    public int origen() { return origen; }
    public int tickLlegada() { return llegada; }
    public int restantes(int tick) { return llegada < 0 ? -1 : Math.max(0, llegada - tick); }

    void estres(MotorSimulacion m, Persona p, double incremento) {
        if (tipo == TipoDesastre.NINGUNO || !p.activa() || !Double.isFinite(incremento) || incremento <= 0) return;
        double previo = p.estres;
        p.aumentarEstres(incremento);
        if (tipo.requiereOrigen() && llegada < 0 && p.estres > previo) llegada = m.tick + intensidad.demora(tipo);
    }

    void iniciarConfusion(MotorSimulacion m) {
        if (tipo != TipoDesastre.TERREMOTO || m.tick != 0 || confusionAsignada) return;
        confusionAsignada = true;
        for (Persona p : m.mapa.personas)
            if (p.activa() && !(p instanceof PersonalRescate)) p.confusion = 3 + m.rnd.nextInt(3);
    }

    void paso(MotorSimulacion m) {
        if (tipo == TipoDesastre.NINGUNO && m.rnd.nextDouble() < m.params.prob) evento(m);
        // Los futuros módulos específicos actualizarán el entorno aquí.
    }

    void evento(MotorSimulacion m) {
        if (tipo != TipoDesastre.NINGUNO) return;
        int[] celda = EventoAleatorio.colapsarPasillo(m.mapa, m.rnd);
        if (celda != null) m.alBloquear(celda[0], celda[1]);
    }
}
