package pe.evacua.escenario;

import java.util.HashSet;
import java.util.Set;
import pe.evacua.estructuras.ListaSimple;

/**
 * Valida un escenario antes de guardarlo.
 * <ul>
 *   <li>RN-01: identificadores únicos de nodos, conexiones y personas.</li>
 *   <li>RN-03: toda conexión une nodos existentes, con tiempo &gt; 0 y capacidad definida (&gt; 0).</li>
 *   <li>RN-10: al menos una salida y todas las personas ubicadas en un nodo válido.</li>
 * </ul>
 * Los bloqueos también deben referirse a elementos existentes.
 */
public final class ValidadorEscenario {

    private ValidadorEscenario() {
    }

    /** Devuelve la lista de errores encontrados; vacía si el escenario es válido. */
    public static ListaSimple<String> validar(Escenario e) {
        ListaSimple<String> errores = new ListaSimple<>();
        if (e == null) {
            errores.agregarFinal("El escenario es nulo");
            return errores;
        }
        if (esVacio(e.nombre())) {
            errores.agregarFinal("El escenario necesita un nombre");
        }
        if (esVacio(e.edificio())) {
            errores.agregarFinal("El escenario necesita el nombre del edificio");
        }

        Set<String> idsNodos = new HashSet<>();
        boolean haySalida = false;
        for (NodoDatos n : e.nodos()) {
            if (esVacio(n.id())) {
                errores.agregarFinal("Hay un nodo sin identificador");
                continue;
            }
            if (!idsNodos.add(n.id())) {
                errores.agregarFinal("RN-01: identificador de nodo repetido: " + n.id());
            }
            if (n.tipo() == null) {
                errores.agregarFinal("RN-02: el nodo " + n.id() + " no tiene tipo");
            } else if (n.tipo() == TipoNodo.SALIDA) {
                haySalida = true;
            }
        }
        if (!haySalida) {
            errores.agregarFinal("RN-10: el edificio debe tener al menos una salida");
        }

        Set<String> idsConexiones = new HashSet<>();
        for (ConexionDatos c : e.conexiones()) {
            if (esVacio(c.id())) {
                errores.agregarFinal("Hay una conexión sin identificador");
                continue;
            }
            if (!idsConexiones.add(c.id())) {
                errores.agregarFinal("RN-01: identificador de conexión repetido: " + c.id());
            }
            if (!idsNodos.contains(c.origen())) {
                errores.agregarFinal("RN-03: la conexión " + c.id() + " parte de un nodo inexistente: " + c.origen());
            }
            if (!idsNodos.contains(c.destino())) {
                errores.agregarFinal("RN-03: la conexión " + c.id() + " llega a un nodo inexistente: " + c.destino());
            }
            if (!(c.tiempoRecorrido() > 0)) {
                errores.agregarFinal("RN-03: la conexión " + c.id() + " debe tener tiempo de recorrido mayor a cero");
            }
            if (c.capacidad() <= 0) {
                errores.agregarFinal("RN-03: la conexión " + c.id() + " debe tener una capacidad máxima mayor a cero");
            }
        }

        Set<String> idsPersonas = new HashSet<>();
        for (PersonaDatos p : e.personas()) {
            if (esVacio(p.id())) {
                errores.agregarFinal("Hay una persona sin identificador");
                continue;
            }
            if (!idsPersonas.add(p.id())) {
                errores.agregarFinal("RN-01: identificador de persona repetido: " + p.id());
            }
            if (p.tipo() == null) {
                errores.agregarFinal("La persona " + p.id() + " no tiene tipo");
            }
            if (!idsNodos.contains(p.nodoInicial())) {
                errores.agregarFinal("RN-10: la persona " + p.id() + " está en un nodo inexistente: " + p.nodoInicial());
            }
        }

        for (BloqueoDatos b : e.bloqueos()) {
            if (b.elemento() == null) {
                errores.agregarFinal("Hay un bloqueo sin tipo de elemento");
            } else if (b.elemento() == TipoElemento.NODO && !idsNodos.contains(b.elementoId())) {
                errores.agregarFinal("El bloqueo apunta a un nodo inexistente: " + b.elementoId());
            } else if (b.elemento() == TipoElemento.CONEXION && !idsConexiones.contains(b.elementoId())) {
                errores.agregarFinal("El bloqueo apunta a una conexión inexistente: " + b.elementoId());
            }
        }
        return errores;
    }

    /** Lanza EscenarioInvalidoException si hay errores. */
    public static void exigirValido(Escenario e) throws EscenarioInvalidoException {
        ListaSimple<String> errores = validar(e);
        if (!errores.estaVacia()) {
            throw new EscenarioInvalidoException(errores);
        }
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
