package pe.evacua.escenario;

import pe.evacua.estructuras.ListaSimple;

/** Escenarios de ejemplo reutilizados por las pruebas. */
public final class EscenariosDePrueba {

    private EscenariosDePrueba() {
    }

    @SafeVarargs
    public static <T> ListaSimple<T> lista(T... elementos) {
        ListaSimple<T> l = new ListaSimple<>();
        for (T e : elementos) {
            l.agregarFinal(e);
        }
        return l;
    }

    /** Edificio mínimo válido: sala -> pasillo -> salida, con 3 personas y un bloqueo. */
    public static Escenario basico(String nombre, String edificio) {
        return new Escenario(
                nombre,
                edificio,
                lista(
                        new NodoDatos("N1", "Sala", TipoNodo.HABITACION, 0, 0, 10),
                        new NodoDatos("N2", "Pasillo A", TipoNodo.PASILLO, 5, 0, 6),
                        new NodoDatos("S1", "Salida principal", TipoNodo.SALIDA, 10, 0, 50)),
                lista(
                        new ConexionDatos("C1", "N1", "N2", 2.5, 4),
                        new ConexionDatos("C2", "N2", "S1", 3.0, 4)),
                lista(
                        new PersonaDatos("P1", TipoPersona.ADULTO, "N1"),
                        new PersonaDatos("P2", TipoPersona.NINO, "N1"),
                        new PersonaDatos("P3", TipoPersona.RESCATE, "N2")),
                lista(new BloqueoDatos(TipoElemento.CONEXION, "C2")),
                null);
    }

    public static ResultadoSimulacion resultado(int evacuadas, int noEvacuadas, double total, double promedio, String nodoCongestionado) {
        return new ResultadoSimulacion(
                evacuadas, noEvacuadas, total, promedio,
                lista(new CongestionNodo(nodoCongestionado, 42), new CongestionNodo("S1", 10)));
    }
}
