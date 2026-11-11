package evacua.estructuras;

import java.util.Comparator;
import java.util.NoSuchElementException;

public class ColaPrioridad<T> {
    private Object[] a = new Object[16];
    private int n;
    private final Comparator<? super T> cmp;

    public ColaPrioridad(Comparator<? super T> cmp) {
        this.cmp = cmp;
    }

    @SuppressWarnings("unchecked")
    private T at(int i) {
        return (T) a[i];
    }

    public void agregar(T x) {
        if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2);
        a[n] = x;
        subir(n++);
    }

    public T sacar() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacia");
        T top = at(0);
        a[0] = a[--n];
        a[n] = null;
        if (n > 0) bajar(0);
        return top;
    }

    public T ver() {
        if (n == 0) throw new NoSuchElementException("cola de prioridad vacia");
        return at(0);
    }

    public int tamano() {
        return n;
    }

    public boolean estaVacia() {
        return n == 0;
    }

    private void subir(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (cmp.compare(at(i), at(p)) >= 0) break;
            intercambiar(i, p);
            i = p;
        }
    }

    private void bajar(int i) {
        while (true) {
            int l = 2 * i + 1, r = l + 1, m = i;
            if (l < n && cmp.compare(at(l), at(m)) < 0) m = l;
            if (r < n && cmp.compare(at(r), at(m)) < 0) m = r;
            if (m == i) return;
            intercambiar(i, m);
            i = m;
        }
    }

    private void intercambiar(int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
