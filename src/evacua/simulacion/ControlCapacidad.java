package evacua.simulacion;

import evacua.edificio.EstadoNodo;
import evacua.edificio.Nodo;

public final class ControlCapacidad {
    private ControlCapacidad() {
    }

    public static EstadoNodo estado(Nodo n, int capacidad) {
        if (n.bloqueado) return EstadoNodo.BLOQUEADO;
        return n.ocupacion >= capacidad ? EstadoNodo.CONGESTIONADO : EstadoNodo.LIBRE;
    }

    public static boolean lleno(Nodo n, int capacidad) {
        return n.ocupacion >= capacidad;
    }
}
