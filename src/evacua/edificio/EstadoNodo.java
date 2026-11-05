package evacua.edificio;

/** Estado de un nodo (RN-04). Solo LIBRE y CONGESTIONADO pueden formar parte de una ruta. */
public enum EstadoNodo {
    LIBRE, CONGESTIONADO, BLOQUEADO
}
