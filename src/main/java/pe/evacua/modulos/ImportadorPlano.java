package pe.evacua.modulos;

import java.io.IOException;
import java.nio.file.Path;
import pe.evacua.escenario.Escenario;

/**
 * Contrato que implementa el Integrante 2 (parser de .json, .txt o .xml, sección 2.10).
 * El menú lo invoca y recibe el edificio ya validado, como un escenario sin personas.
 */
public interface ImportadorPlano {

    /**
     * Lee y valida el archivo (RN-15) y construye el escenario con el nombre indicado.
     *
     * @throws PlanoInvalidoException si el archivo incumple las reglas; en ese caso no se cambia nada
     */
    Escenario importar(Path archivo, String nombreEscenario) throws PlanoInvalidoException, IOException;
}
