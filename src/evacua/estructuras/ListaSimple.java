package evacua.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Lista enlazada simple (propia). */
public class ListaSimple<T> implements Iterable<T> {
    private static class Nodo<T> {
        T dato;
        Nodo<T> sig;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza, cola;
    private int tam;

    public void agregarInicio(T x) {
        Nodo<T> n = new Nodo<>(x);
        n.sig = cabeza;
        cabeza = n;
        if (cola == null) cola = n;
        tam++;
    }

    public void agregarFinal(T x) {
        Nodo<T> n = new Nodo<>(x);
        if (cola == null) cabeza = cola = n;
        else {
            cola.sig = n;
            cola = n;
        }
        tam++;
    }

    public T obtener(int i) {
        if (i < 0 || i >= tam) throw new IndexOutOfBoundsException("indice " + i);
        Nodo<T> n = cabeza;
        while (i-- > 0) n = n.sig;
        return n.dato;
    }

    public T quitarInicio() {
        if (cabeza == null) throw new NoSuchElementException("lista vacia");
        T d = cabeza.dato;
        cabeza = cabeza.sig;
        if (cabeza == null) cola = null;
        tam--;
        return d;
    }

    /** Elimina la primera aparicion de x. Devuelve true si estaba. */
    public boolean eliminar(T x) {
        Nodo<T> prev = null, n = cabeza;
        while (n != null) {
            if (n.dato == null ? x == null : n.dato.equals(x)) {
                if (prev == null) cabeza = n.sig;
                else prev.sig = n.sig;
                if (n == cola) cola = prev;
                tam--;
                return true;
            }
            prev = n;
            n = n.sig;
        }
        return false;
    }

    public boolean contiene(T x) {
        for (T d : this) if (d == null ? x == null : d.equals(x)) return true;
        return false;
    }

    public int tamano() {
        return tam;
    }

    public boolean estaVacia() {
        return tam == 0;
    }

    public void limpiar() {
        cabeza = cola = null;
        tam = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Nodo<T> n = cabeza;

            public boolean hasNext() {
                return n != null;
            }

            public T next() {
                if (n == null) throw new NoSuchElementException();
                T d = n.dato;
                n = n.sig;
                return d;
            }
        };
    }
}
