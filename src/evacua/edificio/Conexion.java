package evacua.edificio;

public class Conexion {
    public final String id;
    public final int origen, destino;
    public final double tiempo;
    public final int capacidad;
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
