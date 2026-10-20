package pe.evacua.escenario;

import pe.evacua.estructuras.ListaSimple;

/**
 * Resumen de una simulación (sección 2.7). Lo produce el motor de simulación (Integrante 4)
 * y se guarda junto al escenario para poder compararlo después.
 *
 * @param nodosMasCongestionados nodos ordenados de mayor a menor congestión
 */
public record ResultadoSimulacion(
        int evacuadas,
        int noEvacuadas,
        double tiempoTotal,
        double tiempoPromedio,
        ListaSimple<CongestionNodo> nodosMasCongestionados) {

    public ResultadoSimulacion {
        if (nodosMasCongestionados == null) {
            nodosMasCongestionados = new ListaSimple<>();
        }
    }
}
