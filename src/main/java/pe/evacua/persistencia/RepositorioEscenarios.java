package pe.evacua.persistencia;

import pe.evacua.escenario.Escenario;
import pe.evacua.escenario.EscenarioInvalidoException;
import pe.evacua.estructuras.ListaSimple;

/**
 * Registro, guardado y carga de escenarios (sección 2.6).
 * La implementación de producción es {@link EscenarioSqliteDao}.
 */
public interface RepositorioEscenarios {

    /**
     * Guarda un escenario. Solo se guarda si cumple RN-01, RN-03 y RN-10.
     *
     * @param reemplazar si es true y ya existe un escenario con ese nombre, lo sobrescribe;
     *                   si es false, lanza RepositorioException
     */
    void guardar(Escenario escenario, boolean reemplazar) throws EscenarioInvalidoException, RepositorioException;

    /** Carga un escenario completo (con su resultado, si lo tiene). */
    Escenario cargar(String nombre) throws RepositorioException;

    boolean existe(String nombre) throws RepositorioException;

    /** Escenarios guardados, del más antiguo al más reciente. */
    ListaSimple<ResumenEscenario> listar() throws RepositorioException;

    void eliminar(String nombre) throws RepositorioException;
}
