package pe.evacua.estructuras;

/** Cola (FIFO) implementada sobre una lista enlazada simple. Todas las operaciones son O(1). */
public class Cola<T> {

    private final ListaSimple<T> elementos = new ListaSimple<>();

    public void encolar(T dato) {
        elementos.agregarFinal(dato);
    }

    /** Quita y devuelve el primer elemento. Lanza NoSuchElementException si está vacía. */
    public T desencolar() {
        return elementos.eliminarPrimero();
    }

    /** Devuelve el primer elemento sin quitarlo. */
    public T frente() {
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
