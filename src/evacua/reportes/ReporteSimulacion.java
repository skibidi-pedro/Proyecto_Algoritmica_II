package evacua.reportes;

import evacua.personas.Persona;
import evacua.personas.RegistroBitacora;
import evacua.simulacion.MotorSimulacion;

import java.util.ArrayList;
import java.util.List;

/** Reportes de texto de una simulacion (general e individual por persona). */
public class ReporteSimulacion {
    private ReporteSimulacion() {
    }

        public static String general(MotorSimulacion m) {
            StringBuilder sb = new StringBuilder();
            sb.append("REPORTE DE SIMULACION\n=====================\n");
            sb.append("Algoritmo: ").append(m.params.astar ? "A*" : "Dijkstra").append("   Semilla: ").append(m.params.semilla).append('\n');
            sb.append("Ticks simulados: ").append(m.tick).append('\n');
            sb.append("Personas: ").append(m.mapa.personas.size()).append("   Evacuadas: ").append(m.evacuadas())
                    .append("   No evacuadas: ").append(m.mapa.personas.size() - m.evacuadas()).append('\n');
            sb.append(String.format("Tiempo promedio de evacuacion: %.1f ticks%n", m.promedioEvac()));
            sb.append(String.format("Estres promedio final: %.2f%n", m.promedioEstres()));
            sb.append("Nodos explorados por los algoritmos: ").append(m.exploradosTotal).append('\n');
            List<int[]> cuellos = new ArrayList<>();
            for (int i = 0; i < m.mapa.filas; i++)
                for (int j = 0; j < m.mapa.cols; j++)
                    if (m.mapa.n[i][j].esperas > 0) cuellos.add(new int[]{i, j, m.mapa.n[i][j].esperas});
            cuellos.sort((a, b) -> Integer.compare(b[2], a[2]));
            sb.append("\nCuellos de botella (nodos con mas esperas):\n");
            if (cuellos.isEmpty()) sb.append("  ninguno\n");
            for (int k = 0; k < Math.min(3, cuellos.size()); k++)
                sb.append("  (").append(cuellos.get(k)[0]).append(',').append(cuellos.get(k)[1])
                        .append(") -> ").append(cuellos.get(k)[2]).append(" esperas\n");
            sb.append("\nPor persona:\n");
            for (Persona p : m.mapa.personas)
                sb.append(String.format("  %-34s %s  pasos=%d  recalculos=%d  estres=%.1f%n", p.etiqueta(),
                        p.evacuada ? "EVACUADA t=" + p.tEvac : "NO EVACUADA", p.pasos, p.historial.recalculos, p.estres));
            return sb.toString();
        }

        public static String individual(Persona p) {
            StringBuilder sb = new StringBuilder();
            sb.append("REPORTE INDIVIDUAL - ").append(p.etiqueta()).append('\n');
            sb.append("Estado final: ").append(p.evacuada ? "evacuada en t=" + p.tEvac : "no evacuada").append('\n');
            sb.append("Pasos: ").append(p.pasos).append("  Recalculos: ").append(p.historial.recalculos)
                    .append("  Bloqueos sufridos: ").append(p.historial.bloqueosSufridos)
                    .append("  Espera en cuellos de botella: ").append(p.historial.esperaCuelloBotella)
                    .append(String.format("  Estres final: %.1f%n", p.estres));
            sb.append("\nBitacora de recorrido (nodo visitado, paso y estado del nodo):\n");
            for (RegistroBitacora r : p.bitacora) sb.append("  ").append(r).append('\n');
            sb.append("\nLinea de tiempo:\n");
            for (String h : p.hist) sb.append("  ").append(h).append('\n');
            return sb.toString();
        }
}
