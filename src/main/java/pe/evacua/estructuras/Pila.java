package pe.evacua.estructuras;

/** Pila (LIFO) implementada sobre una lista enlazada simple. Todas las operaciones son O(1). */
public class Pila<T> {

    private final ListaSimple<T> elementos = new ListaSimple<>();

    public void apilar(T dato) {
        elementos.agregarInicio(dato);
    }

    /** Quita y devuelve el elemento de la cima. Lanza NoSuchElementException si está vacía. */
    public T desapilar() {
        return elementos.eliminarPrimero();
    }

    /** Devuelve el elemento de la cima sin quitarlo. */
    public T cima() {
        return elementos.obtenerPrimero();
    }

    public int tamanio() {
        return elementos.tamanio();
    }

    public boolean estaVacia() {
        return elementos.estaVacia();
    }

    public void limpiar() {
        elementos.limpiar();
    }

    @Override
    public String toString() {
        return elementos.toString();
    }
}
