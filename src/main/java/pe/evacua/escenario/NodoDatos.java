package pe.evacua.escenario;

/** Datos de un nodo del edificio tal como se guardan en un escenario. */
public record NodoDatos(String id, String nombre, TipoNodo tipo, double x, double y, int capacidad) {
}
