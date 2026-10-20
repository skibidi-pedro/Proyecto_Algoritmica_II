package pe.evacua.persistencia;

/** No existe un escenario guardado con el nombre pedido. */
public class EscenarioNoEncontradoException extends RepositorioException {

    public EscenarioNoEncontradoException(String nombre) {
        super("No existe un escenario guardado con el nombre \"" + nombre + "\"");
    }
}
