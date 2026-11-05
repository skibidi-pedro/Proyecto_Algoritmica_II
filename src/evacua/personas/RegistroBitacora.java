package evacua.personas;

import evacua.edificio.EstadoNodo;

/** Un registro de la bitacora: inmutable una vez escrito (RN-11). */
public final class RegistroBitacora {
    public final int paso;          // paso de simulacion (timestamp)
    public final int fila, col;     // nodo visitado
    public final EstadoNodo estado; // estado del nodo en ese instante

    public RegistroBitacora(int paso, int fila, int col, EstadoNodo estado) {
        this.paso = paso;
        this.fila = fila;
        this.col = col;
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "t=" + paso + " nodo(" + fila + "," + col + ") " + estado;
    }
}
