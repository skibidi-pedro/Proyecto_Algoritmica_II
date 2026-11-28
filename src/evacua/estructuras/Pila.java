package evacua.estructuras;

import java.util.NoSuchElementException;

public class Pila<T> {
    private final ListaSimple<T> datos = new ListaSimple<>();

    public void apilar(T x) {
        datos.agregarInicio(x);
    }

    public T desapilar() {
        if (datos.estaVacia()) throw new NoSuchElementException("pila vacia");
        return datos.quitarInicio();
    }

    public T cima() {
        if (datos.estaVacia()) throw new NoSuchElementException("pila vacia");
        return datos.obtener(0);
    }

    public int tamano() {
        return datos.tamano();
    }

    public boolean estaVacia() {
        return datos.estaVacia();
    }
}
