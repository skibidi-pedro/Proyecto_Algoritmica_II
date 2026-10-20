package pe.evacua.modulos;

import pe.evacua.estructuras.ListaSimple;

/** El archivo de plano no cumple RN-15. El edificio actual no se modifica. */
public class PlanoInvalidoException extends Exception {

    private final ListaSimple<String> errores;

    public PlanoInvalidoException(ListaSimple<String> errores) {
        super("El plano no es válido (" + errores.tamanio() + " error(es))");
        this.errores = errores;
    }

    public ListaSimple<String> errores() {
        return errores;
    }
}
