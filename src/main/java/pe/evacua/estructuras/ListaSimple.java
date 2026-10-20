package pe.evacua.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista enlazada simple con referencia a la cabeza y a la cola.
 * Insertar al inicio/final y eliminar el primero son O(1).
 */
public class ListaSimple<T> implements Iterable<T> {

    private static final class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamanio;

    public void agregarInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamanio++;
    }

    public void agregarFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cola == null) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamanio++;
    }

    public T obtenerPrimero() {
        if (cabeza == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        return cabeza.dato;
    }

    public T obtenerUltimo() {
        if (cola == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        return cola.dato;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango (tamaño " + tamanio + ")");
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        if (cabeza == null) {
            cola = null;
        }
        tamanio--;
        return dato;
    }

    /** Elimina la primera aparición del dato (según equals). Devuelve true si lo encontró. */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (Objects.equals(actual.dato, dato)) {
                if (anterior == null) {
                    cabeza = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == cola) {
                    cola = anterior;
                }
                tamanio--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    public boolean contiene(T dato) {
        for (T elemento : this) {
            if (Objects.equals(elemento, dato)) {
                return true;
            }
        }
        return false;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public void limpiar() {
        cabeza = null;
        cola = null;
        tamanio = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ListaSimple<?> lista) || lista.tamanio != tamanio) {
            return false;
        }
        Iterator<?> a = iterator();
        Iterator<?> b = lista.iterator();
        while (a.hasNext()) {
            if (!Objects.equals(a.next(), b.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (T dato : this) {
            hash = 31 * hash + Objects.hashCode(dato);
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean primero = true;
        for (T dato : this) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(dato);
            primero = false;
        }
        return sb.append(']').toString();
    }
}
