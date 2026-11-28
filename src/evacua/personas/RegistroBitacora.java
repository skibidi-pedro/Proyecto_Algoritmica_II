package evacua.personas;

import evacua.edificio.EstadoNodo;

public final class RegistroBitacora {
    public final int paso;
    public final int fila, col;
    public final EstadoNodo estado;

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
