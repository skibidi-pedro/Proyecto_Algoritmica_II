package pe.evacua.escenario;

/** Datos de una conexión (puerta, pasillo, escalera) entre dos nodos: RN-03. */
public record ConexionDatos(String id, String origen, String destino, double tiempoRecorrido, int capacidad) {
}
