package evacua.estructuras;

import java.util.NoSuchElementException;

public class Cola<T> implements Iterable<T> {
    private final ListaSimple<T> datos = new ListaSimple<>();

    public void encolar(T x) {
        datos.agregarFinal(x);
    }

    public T desencolar() {
        if (datos.estaVacia()) throw new NoSuchElementException("cola vacia");
        return datos.quitarInicio();
    }

    public T frente() {
        if (datos.estaVacia()) throw new NoSuchElementException("cola vacia");
        return datos.obtener(0);
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return datos.iterator();
    }

    public int tamano() {
        return datos.tamano();
    }

    public boolean estaVacia() {
        return datos.estaVacia();
    }
}
