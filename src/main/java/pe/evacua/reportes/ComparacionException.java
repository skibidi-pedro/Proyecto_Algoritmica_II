package pe.evacua.reportes;

/** Los escenarios elegidos no se pueden comparar (distinto edificio, sin resultado, menos de dos). */
public class ComparacionException extends Exception {

    public ComparacionException(String mensaje) {
        super(mensaje);
    }
}
