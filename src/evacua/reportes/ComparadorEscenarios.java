package evacua.reportes;

import evacua.edificio.Edificio;
import evacua.persistencia.BaseDatos;
import evacua.simulacion.MotorSimulacion;
import evacua.simulacion.ParametrosSimulacion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ComparadorEscenarios {
    private ComparadorEscenarios() {
    }

    public static class Fila {
        public String nombre, algoritmo;
        public long semilla;
        public int personas, evacuadas, ticks;
        public double tiempoPromedio, estresPromedio;

        public String aCsv() {
            return String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%.2f,%.2f", nombre, algoritmo, semilla, personas, evacuadas, ticks, tiempoPromedio, estresPromedio);
        }
    }

    public static Fila simular(String nombre, Edificio e, ParametrosSimulacion params) {
        MotorSimulacion m = new MotorSimulacion(e.copia(), params.copia(), s -> { });
        m.iniciar();
        while (m.paso()) {  }
        Fila f = new Fila();
        f.nombre = nombre;
        f.algoritmo = params.astar ? "A*" : "Dijkstra";
        f.semilla = params.semilla;
        f.personas = m.mapa.personas.size();
        f.evacuadas = m.evacuadas();
        f.ticks = m.tick;
        f.tiempoPromedio = m.promedioEvac();
        f.estresPromedio = m.promedioEstres();
        return f;
    }

    public static List<Fila> compararGuardados(BaseDatos bd, List<String> nombres, ParametrosSimulacion params) throws IOException {
        List<Fila> r = new ArrayList<>();
        for (String n : nombres) {
            Fila f = simular(n, bd.cargar(n), params);
            bd.registrarResultado(f.aCsv());
            r.add(f);
        }
        return r;
    }

    public static String aTexto(List<Fila> filas) {
        StringBuilder sb = new StringBuilder(String.format("%-22s %-9s %-9s %-10s %-7s %-12s %s%n", "Escenario", "Algoritmo", "Personas", "Evacuadas", "Ticks", "T. promedio", "Estres prom."));
        for (Fila f : filas)
            sb.append(String.format(Locale.ROOT, "%-22s %-9s %-9d %-10d %-7d %-12.1f %.2f%n", f.nombre, f.algoritmo, f.personas, f.evacuadas, f.ticks, f.tiempoPromedio, f.estresPromedio));
        return sb.toString();
    }
}
