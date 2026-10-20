package pe.evacua.persistencia;

/** Error al leer o escribir escenarios (base de datos no disponible, nombre repetido, etc.). */
public class RepositorioException extends Exception {

    public RepositorioException(String mensaje) {
        super(mensaje);
    }

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
