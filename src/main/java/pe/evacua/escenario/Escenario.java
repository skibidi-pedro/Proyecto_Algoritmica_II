package pe.evacua.escenario;

import pe.evacua.estructuras.ListaSimple;

/**
 * Instantánea de una configuración: edificio, personas y bloqueos (sección 2.6).
 * Es el formato común con el que los módulos se pasan datos entre sí y con la base de datos,
 * de modo que la persistencia no dependa de las clases internas de los demás integrantes.
 *
 * @param nombre    nombre único del escenario
 * @param edificio  nombre del edificio; solo se pueden comparar escenarios del mismo edificio
 * @param resultado resultado de la última simulación, o null si aún no se ha simulado
 */
public record Escenario(
        String nombre,
        String edificio,
        ListaSimple<NodoDatos> nodos,
        ListaSimple<ConexionDatos> conexiones,
        ListaSimple<PersonaDatos> personas,
        ListaSimple<BloqueoDatos> bloqueos,
        ResultadoSimulacion resultado) {

    public Escenario {
        if (nodos == null) {
            nodos = new ListaSimple<>();
        }
        if (conexiones == null) {
            conexiones = new ListaSimple<>();
        }
        if (personas == null) {
            personas = new ListaSimple<>();
        }
        if (bloqueos == null) {
            bloqueos = new ListaSimple<>();
        }
    }

    public Escenario conResultado(ResultadoSimulacion nuevoResultado) {
        return new Escenario(nombre, edificio, nodos, conexiones, personas, bloqueos, nuevoResultado);
    }

    public Escenario conNombre(String nuevoNombre) {
        return new Escenario(nuevoNombre, edificio, nodos, conexiones, personas, bloqueos, resultado);
    }
}
