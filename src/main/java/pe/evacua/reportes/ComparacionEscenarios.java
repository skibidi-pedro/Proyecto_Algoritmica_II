package pe.evacua.reportes;

import java.util.Locale;
import pe.evacua.estructuras.ListaSimple;

/**
 * Comparación de varios escenarios simulados sobre el mismo edificio (sección 2.7).
 *
 * @param mejorEscenario el que evacuó a más personas y, en caso de empate, con menor tiempo total
 */
public record ComparacionEscenarios(String edificio, ListaSimple<FilaComparacion> filas, String mejorEscenario) {

    /** Tabla de texto lista para mostrar en consola o guardar en un archivo. */
    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("COMPARACIÓN DE ESCENARIOS - Edificio: ").append(edificio).append('\n');
        sb.append(String.format(Locale.ROOT, "%-22s %9s %12s %13s %14s  %s%n",
                "Escenario", "Evacuadas", "No evacuadas", "Tiempo total", "Tiempo prom.", "Nodo más congestionado"));
        for (FilaComparacion f : filas) {
            String marca = f.escenario().equals(mejorEscenario) ? " *" : "";
            sb.append(String.format(Locale.ROOT, "%-22s %9d %12d %13.2f %14.2f  %s%s%n",
                    f.escenario(), f.evacuadas(), f.noEvacuadas(), f.tiempoTotal(), f.tiempoPromedio(),
                    f.nodoMasCongestionado(), marca));
        }
        sb.append("* Mejor escenario: ").append(mejorEscenario)
                .append(" (más personas evacuadas; en empate, menor tiempo total)\n");
        return sb.toString();
    }
}
