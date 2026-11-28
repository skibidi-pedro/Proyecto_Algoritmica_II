package evacua.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListaDoble<T> implements Iterable<T> {
    private static class Nodo<T> {
        T dato;
        Nodo<T> sig, ant;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza, cola;
    private int tam;

    public void agregarInicio(T x) {
        Nodo<T> n = new Nodo<>(x);
        n.sig = cabeza;
        if (cabeza != null) cabeza.ant = n;
        cabeza = n;
        if (cola == null) cola = n;
        tam++;
    }

    public void agregarFinal(T x) {
        Nodo<T> n = new Nodo<>(x);
        n.ant = cola;
        if (cola != null) cola.sig = n;
        cola = n;
        if (cabeza == null) cabeza = n;
        tam++;
    }

    public T primero() {
        if (cabeza == null) throw new NoSuchElementException("lista vacia");
        return cabeza.dato;
    }

    public T ultimo() {
        if (cola == null) throw new NoSuchElementException("lista vacia");
        return cola.dato;
    }

    public T quitarInicio() {
        if (cabeza == null) throw new NoSuchElementException("lista vacia");
        T d = cabeza.dato;
        cabeza = cabeza.sig;
        if (cabeza == null) cola = null;
        else cabeza.ant = null;
        tam--;
        return d;
    }

    public T quitarFinal() {
        if (cola == null) throw new NoSuchElementException("lista vacia");
        T d = cola.dato;
        cola = cola.ant;
        if (cola == null) cabeza = null;
        else cola.sig = null;
        tam--;
        return d;
    }

    public T obtener(int i) {
        if (i < 0 || i >= tam) throw new IndexOutOfBoundsException("indice " + i);
        Nodo<T> n;
        if (i < tam / 2) {
            n = cabeza;
            while (i-- > 0) n = n.sig;
        } else {
            n = cola;
            for (int k = tam - 1; k > i; k--) n = n.ant;
        }
        return n.dato;
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

    public Iterable<T> alReves() {
        return () -> new Iterator<T>() {
            private Nodo<T> n = cola;

            public boolean hasNext() {
                return n != null;
            }

            public T next() {
                if (n == null) throw new NoSuchElementException();
                T d = n.dato;
                n = n.ant;
                return d;
            }
        };
    }
}
