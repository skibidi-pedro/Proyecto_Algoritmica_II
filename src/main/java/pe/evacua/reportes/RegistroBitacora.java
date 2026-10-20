package pe.evacua.reportes;

import pe.evacua.escenario.EstadoNodo;

/** Una fila de la bitácora de recorrido (RN-11): nodo visitado, paso de simulación y estado del nodo. */
public record RegistroBitacora(int paso, String nodoId, EstadoNodo estadoNodo) {
}
