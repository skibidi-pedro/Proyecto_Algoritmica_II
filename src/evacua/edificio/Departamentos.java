package evacua.edificio;

import evacua.estructuras.Cola;
import java.util.Locale;

public final class Departamentos {
    private static final int[][] DIRECCIONES = {{-1,0},{0,-1},{0,1},{1,0}};

    private Departamentos() {}

    public static void detectar(Edificio e) {
        for (Nodo[] fila : e.n) for (Nodo n : fila) n.departamento = 0;
        int numero = 0;
        for (int f = 0; f < e.filas; f++) for (int c = 0; c < e.cols; c++) {
            Nodo puerta = e.n[f][c];
            if (puerta.tipo != TipoNodo.PUERTA) continue;
            boolean pasillo = false;
            int inicio = -1;
            for (int[] d : DIRECCIONES) {
                int y = f + d[0], x = c + d[1];
                if (!e.dentro(y,x) || e.piso(y) != e.piso(f)) continue;
                Nodo vecino = e.n[y][x];
                if (vecino.tipo == TipoNodo.PASILLO) pasillo = true;
                if (inicio < 0 && vecino.tipo == TipoNodo.HABITACION) inicio = y * e.cols + x;
            }
            if (!pasillo || inicio < 0) continue;
            Nodo primera = e.n[inicio / e.cols][inicio % e.cols];
            if (primera.departamento != 0) continue;
            puerta.departamento = ++numero;
            primera.departamento = numero;
            Cola<Integer> cola = new Cola<>();
            cola.encolar(inicio);
            while (!cola.estaVacia()) {
                int actual = cola.desencolar();
                for (int[] d : DIRECCIONES) {
                    int y = actual / e.cols + d[0], x = actual % e.cols + d[1];
                    if (!e.dentro(y,x) || e.piso(y) != e.piso(f)) continue;
                    Nodo vecino = e.n[y][x];
                    if (vecino.tipo == TipoNodo.HABITACION && vecino.departamento == 0) {
                        vecino.departamento = numero;
                        cola.encolar(y * e.cols + x);
                    }
                }
            }
        }
    }

    public static String identificador(int numero) {
        return String.format(Locale.ROOT, "Dep-%03d", numero);
    }
}
