package evacua.edificio;

import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;

import java.util.ArrayList;
import java.util.List;

/** El edificio: cuadricula de nodos y las personas registradas en el. */
public class Edificio {
    public final int filas, cols;
    public final Nodo[][] n;
    public final List<Persona> personas = new ArrayList<>();
    public int sigId = 1;

    public Edificio(int filas, int cols) {
        this.filas = filas;
        this.cols = cols;
        n = new Nodo[filas][cols];
        for (int i = 0; i < filas; i++)
            for (int j = 0; j < cols; j++) n[i][j] = new Nodo();
    }

    public boolean dentro(int f, int c) {
        return f >= 0 && c >= 0 && f < filas && c < cols;
    }

    public boolean caminable(int f, int c) {
        return dentro(f, c) && n[f][c].tipo != TipoNodo.VACIO && !n[f][c].bloqueado;
    }

    public void rect(TipoNodo t, int f0, int c0, int f1, int c1) {
        for (int i = f0; i <= f1; i++)
            for (int j = c0; j <= c1; j++) n[i][j].tipo = t;
    }

    public List<int[]> salidas() {
        List<int[]> r = new ArrayList<>();
        for (int i = 0; i < filas; i++)
            for (int j = 0; j < cols; j++)
                if (n[i][j].tipo == TipoNodo.SALIDA && !n[i][j].bloqueado) r.add(new int[]{i, j});
        return r;
    }

    public Persona agregar(TipoPersona t, EstadoSalud s, int f, int c) {
        Persona p = new Persona(sigId++, t, s, f, c);
        personas.add(p);
        return p;
    }

}
