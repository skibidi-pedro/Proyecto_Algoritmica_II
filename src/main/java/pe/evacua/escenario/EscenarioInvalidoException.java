package pe.evacua.escenario;

import pe.evacua.estructuras.ListaSimple;

/** El escenario incumple alguna regla de negocio (RN-01, RN-03, RN-10) y no puede guardarse. */
public class EscenarioInvalidoException extends Exception {

    private final ListaSimple<String> errores;

    public EscenarioInvalidoException(ListaSimple<String> errores) {
        super(construirMensaje(errores));
        this.errores = errores;
    }

    public ListaSimple<String> errores() {
        return errores;
    }

    private static String construirMensaje(ListaSimple<String> errores) {
        StringBuilder sb = new StringBuilder("Escenario inválido:");
        for (String error : errores) {
            sb.append("\n - ").append(error);
        }
        return sb.toString();
    }
}
