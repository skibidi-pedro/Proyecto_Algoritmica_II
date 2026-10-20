package pe.evacua.estructuras;

import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Cola de prioridad implementada como heap binario mínimo sobre un arreglo dinámico.
 * El elemento "menor" según el comparador sale primero. Insertar y extraer son O(log n).
 * Pensada para Dijkstra y A* (Integrante 2): se inserta de nuevo en vez de decrementar clave
 * y se descartan las entradas obsoletas al extraer.
 */
public class ColaPrioridad<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private final Comparator<? super T> comparador;
    private Object[] heap = new Object[CAPACIDAD_INICIAL];
    private int tamanio;

    public ColaPrioridad(Comparator<? super T> comparador) {
        if (comparador == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.comparador = comparador;
    }

    /** Usa el orden natural; los elementos deben implementar Comparable. */
    @SuppressWarnings("unchecked")
    public static <E extends Comparable<? super E>> ColaPrioridad<E> ordenNatural() {
        return new ColaPrioridad<>((a, b) -> a.compareTo(b));
    }

    public void insertar(T dato) {
        if (tamanio == heap.length) {
            crecer();
        }
        heap[tamanio] = dato;
        subir(tamanio);
        tamanio++;
    }

    /** Quita y devuelve el elemento de mayor prioridad (el menor). */
    @SuppressWarnings("unchecked")
    public T extraerMinimo() {
        if (tamanio == 0) {
            throw new NoSuchElementException("La cola de prioridad está vacía");
        }
        T minimo = (T) heap[0];
        tamanio--;
        heap[0] = heap[tamanio];
        heap[tamanio] = null;
        if (tamanio > 0) {
            bajar(0);
        }
        return minimo;
    }

    @SuppressWarnings("unchecked")
    public T verMinimo() {
        if (tamanio == 0) {
            throw new NoSuchElementException("La cola de prioridad está vacía");
        }
        return (T) heap[0];
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public void limpiar() {
        for (int i = 0; i < tamanio; i++) {
            heap[i] = null;
        }
        tamanio = 0;
    }

    private void crecer() {
        Object[] nuevo = new Object[heap.length * 2];
        System.arraycopy(heap, 0, nuevo, 0, tamanio);
        heap = nuevo;
    }

    @SuppressWarnings("unchecked")
    private int comparar(int i, int j) {
        return comparador.compare((T) heap[i], (T) heap[j]);
    }

    private void intercambiar(int i, int j) {
        Object tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
    }

    private void subir(int indice) {
        while (indice > 0) {
            int padre = (indice - 1) / 2;
            if (comparar(indice, padre) >= 0) {
                break;
            }
            intercambiar(indice, padre);
            indice = padre;
        }
    }

    private void bajar(int indice) {
        while (true) {
            int izquierdo = 2 * indice + 1;
            int derecho = izquierdo + 1;
            int menor = indice;
            if (izquierdo < tamanio && comparar(izquierdo, menor) < 0) {
                menor = izquierdo;
            }
            if (derecho < tamanio && comparar(derecho, menor) < 0) {
                menor = derecho;
            }
            if (menor == indice) {
                return;
            }
            intercambiar(indice, menor);
            indice = menor;
        }
    }
}
