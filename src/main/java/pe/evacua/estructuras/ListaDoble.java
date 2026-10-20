package pe.evacua.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista doblemente enlazada. Se usa, por ejemplo, para la bitácora de recorrido
 * de cada persona (RN-11): se agrega al final y se puede recorrer en ambos sentidos.
 */
public class ListaDoble<T> implements Iterable<T> {

    private static final class Nodo<T> {
        T dato;
        Nodo<T> anterior;
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
        if (cabeza != null) {
            cabeza.anterior = nuevo;
        } else {
            cola = nuevo;
        }
        cabeza = nuevo;
        tamanio++;
    }

    public void agregarFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.anterior = cola;
        if (cola != null) {
            cola.siguiente = nuevo;
        } else {
            cabeza = nuevo;
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

    /** Acceso por índice recorriendo desde el extremo más cercano. */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango (tamaño " + tamanio + ")");
        }
        Nodo<T> actual;
        if (indice < tamanio / 2) {
            actual = cabeza;
            for (int i = 0; i < indice; i++) {
                actual = actual.siguiente;
            }
        } else {
            actual = cola;
            for (int i = tamanio - 1; i > indice; i--) {
                actual = actual.anterior;
            }
        }
        return actual.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        T dato = cabeza.dato;
        desenlazar(cabeza);
        return dato;
    }

    public T eliminarUltimo() {
        if (cola == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        T dato = cola.dato;
        desenlazar(cola);
        return dato;
    }

    /** Elimina la primera aparición del dato (según equals). Devuelve true si lo encontró. */
    public boolean eliminar(T dato) {
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            if (Objects.equals(actual.dato, dato)) {
                desenlazar(actual);
                return true;
            }
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

    private void desenlazar(Nodo<T> nodo) {
        if (nodo.anterior != null) {
            nodo.anterior.siguiente = nodo.siguiente;
        } else {
            cabeza = nodo.siguiente;
        }
        if (nodo.siguiente != null) {
            nodo.siguiente.anterior = nodo.anterior;
        } else {
            cola = nodo.anterior;
        }
        nodo.anterior = null;
        nodo.siguiente = null;
        tamanio--;
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

    /** Recorrido desde el último elemento hasta el primero. */
    public Iterable<T> enOrdenInverso() {
        return () -> new Iterator<>() {
            private Nodo<T> actual = cola;

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
                actual = actual.anterior;
                return dato;
            }
        };
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ListaDoble<?> lista) || lista.tamanio != tamanio) {
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
