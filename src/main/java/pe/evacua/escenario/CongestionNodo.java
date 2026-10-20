package pe.evacua.escenario;

/** Ocupación acumulada de un nodo durante una simulación (base del mapa de calor, RN-19). */
public record CongestionNodo(String nodoId, long ocupacionAcumulada) {
}
