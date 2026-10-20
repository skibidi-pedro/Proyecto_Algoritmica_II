package pe.evacua.reportes;

import pe.evacua.escenario.CongestionNodo;
import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.ResultadoSimulacion;
import pe.evacua.estructuras.ListaSimple;

/** Compara los resultados de distintos escenarios guardados sobre el mismo edificio. */
public final class ComparadorEscenarios {

    private ComparadorEscenarios() {
    }

    public static ComparacionEscenarios comparar(ListaSimple<Escenario> escenarios) throws ComparacionException {
        if (escenarios == null || escenarios.tamanio() < 2) {
            throw new ComparacionException("Se necesitan al menos dos escenarios para comparar");
        }
        String edificio = escenarios.obtenerPrimero().edificio();
        ListaSimple<FilaComparacion> filas = new ListaSimple<>();
        FilaComparacion mejor = null;
        for (Escenario e : escenarios) {
            if (!e.edificio().equals(edificio)) {
                throw new ComparacionException("Solo se pueden comparar escenarios del mismo edificio: \""
                        + e.nombre() + "\" es de \"" + e.edificio() + "\" y se esperaba \"" + edificio + "\"");
            }
            ResultadoSimulacion r = e.resultado();
            if (r == null) {
                throw new ComparacionException("El escenario \"" + e.nombre() + "\" todavía no tiene una simulación guardada");
            }
            FilaComparacion fila = new FilaComparacion(
                    e.nombre(), r.evacuadas(), r.noEvacuadas(), r.tiempoTotal(), r.tiempoPromedio(),
                    masCongestionado(r));
            filas.agregarFinal(fila);
            if (mejor == null || esMejor(fila, mejor)) {
                mejor = fila;
            }
        }
        return new ComparacionEscenarios(edificio, filas, mejor.escenario());
    }

    private static boolean esMejor(FilaComparacion a, FilaComparacion b) {
        if (a.evacuadas() != b.evacuadas()) {
            return a.evacuadas() > b.evacuadas();
        }
        return a.tiempoTotal() < b.tiempoTotal();
    }

    /** Nodo de mayor ocupación acumulada (no se asume el orden de la lista). */
    private static String masCongestionado(ResultadoSimulacion r) {
        CongestionNodo mayor = null;
        for (CongestionNodo c : r.nodosMasCongestionados()) {
            if (mayor == null || c.ocupacionAcumulada() > mayor.ocupacionAcumulada()) {
                mayor = c;
            }
        }
        return mayor == null ? "-" : mayor.nodoId();
    }
}
