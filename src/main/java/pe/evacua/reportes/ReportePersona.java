package pe.evacua.reportes;

import pe.evacua.escenario.TipoPersona;
import pe.evacua.estructuras.ListaDoble;

/**
 * Todo lo que se exporta de una persona al terminar la simulación (RN-14).
 * El Integrante 3 llena estos datos desde la clase Persona (bitácora, estrés, historial)
 * y el Integrante 4 los entrega al terminar el motor de simulación.
 *
 * @param pasoSalida paso en que llegó a una salida; se ignora si no evacuó
 * @param pasosEsperaCuelloBotella tiempo total de espera en cuellos de botella (RN-13)
 * @param bitacora   nodos visitados en orden, sin modificarse una vez escritos (RN-11)
 */
public record ReportePersona(
        String personaId,
        TipoPersona tipo,
        boolean evacuada,
        int pasoSalida,
        int estresFinal,
        int bloqueosSufridos,
        int recalculosRuta,
        int pasosEsperaCuelloBotella,
        ListaDoble<RegistroBitacora> bitacora) {

    public ReportePersona {
        if (bitacora == null) {
            bitacora = new ListaDoble<>();
        }
    }
}
