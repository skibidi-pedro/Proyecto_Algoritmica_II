package evacua.simulacion;

import evacua.edificio.EstadoNodo;
import evacua.edificio.Nodo;

public final class ControlCapacidad {
    private ControlCapacidad() {
    }

    public static EstadoNodo estado(Nodo n, int capacidad) {
        if (n.bloqueado) return EstadoNodo.BLOQUEADO;
        if (lleno(n, capacidad)) return EstadoNodo.CONGESTIONADO;
        if (n.peligroso) return EstadoNodo.PELIGROSO;
        return n.danado ? EstadoNodo.DANADO : EstadoNodo.LIBRE;
    }

    public static boolean lleno(Nodo n, int capacidad) {
        return n.ocupacion >= n.capacidadEfectiva(capacidad);
    }
}
