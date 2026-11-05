package evacua.edificio;

/** Conexion bidireccional entre dos nodos (puerta, pasillo, escalera): tiempo de recorrido y capacidad maxima (RN-03). */
public class Conexion {
    public final String id;
    public final int origen, destino;   // indices de nodo: fila * cols + col
    public final double tiempo;         // > 0
    public final int capacidad;         // >= 1
    public boolean bloqueada;

    public Conexion(String id, int origen, int destino, double tiempo, int capacidad) {
        if (tiempo <= 0) throw new IllegalArgumentException("El tiempo de recorrido debe ser mayor a cero.");
        if (capacidad < 1) throw new IllegalArgumentException("La capacidad maxima debe ser al menos 1.");
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.tiempo = tiempo;
        this.capacidad = capacidad;
    }
}
