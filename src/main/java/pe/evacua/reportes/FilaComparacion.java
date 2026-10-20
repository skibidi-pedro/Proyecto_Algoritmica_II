package pe.evacua.reportes;

/** Resultado de un escenario dentro de una comparación. */
public record FilaComparacion(
        String escenario,
        int evacuadas,
        int noEvacuadas,
        double tiempoTotal,
        double tiempoPromedio,
        String nodoMasCongestionado) {
}
