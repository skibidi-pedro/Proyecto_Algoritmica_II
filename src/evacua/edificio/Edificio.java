package evacua.edificio;

import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;

import java.util.ArrayList;
import java.util.List;

/**
 * El edificio: cuadricula de nodos y las personas registradas en el. Tiene uno o mas pisos
 * apilados en la misma cuadricula: el piso k (desde 0) ocupa las filas [k*filasPorPiso, (k+1)*filasPorPiso).
 * Las escaleras en la misma (fila, col) de pisos consecutivos conectan los pisos.
 */
public class Edificio {
    public final int filas, cols;
    public final int filasPorPiso, pisos;
    public final Nodo[][] n;
    public final List<Persona> personas = new ArrayList<>();
    public int sigId = 1;
    /** Conexiones definidas explicitamente (plano importado o editadas). */
    public final List<Conexion> conexiones = new ArrayList<>();
    /** Si es true, el grafo usa solo {@link #conexiones}; si no, usa la adyacencia de la cuadricula. */
    public boolean conexionesExplicitas;
    /** Identificadores de nodo (RN-01). Se generan como N{fila}_{col} si no se definieron. */
    public final java.util.Map<String, Integer> idANodo = new java.util.LinkedHashMap<>();

    public Edificio(int filas, int cols) {
        this(filas, cols, 1);
    }

    public Edificio(int filasPorPiso, int cols, int pisos) {
        if (pisos < 1) throw new IllegalArgumentException("El edificio necesita al menos un piso.");
        this.filasPorPiso = filasPorPiso;
        this.pisos = pisos;
        this.filas = filasPorPiso * pisos;
        this.cols = cols;
        n = new Nodo[filas][cols];
        for (int i = 0; i < filas; i++)
            for (int j = 0; j < cols; j++) n[i][j] = new Nodo();
    }

    /** Piso (desde 0) al que pertenece una fila. */
    public int piso(int f) {
        return f / filasPorPiso;
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
        Persona p = Persona.crear(sigId++, t, s, f, c);
        personas.add(p);
        return p;
    }


    // ---------- CRUD de nodos (RN-01, RN-02) ----------

    public String idDe(int f, int c) {
        for (java.util.Map.Entry<String, Integer> en : idANodo.entrySet())
            if (en.getValue() == f * cols + c) return en.getKey();
        return "N" + f + "_" + c;
    }

    /** Crea o modifica el nodo de la celda. Si id es null se genera uno unico. */
    public void crearNodo(TipoNodo tipo, int f, int c, String id) {
        if (!dentro(f, c)) throw new IllegalArgumentException("Celda fuera del edificio: (" + f + "," + c + ")");
        if (tipo == TipoNodo.VACIO) throw new IllegalArgumentException("Use eliminarNodo para quitar un nodo.");
        String nuevo = id != null ? id : idDe(f, c);
        Integer ocupado = idANodo.get(nuevo);
        if (ocupado != null && ocupado != f * cols + c) throw new IllegalArgumentException("Identificador repetido: " + nuevo);
        idANodo.values().removeIf(v -> v == f * cols + c);
        idANodo.put(nuevo, f * cols + c);
        n[f][c].tipo = tipo;
    }

    public void modificarNodo(int f, int c, TipoNodo tipo) {
        if (n[f][c].tipo == TipoNodo.VACIO) throw new IllegalArgumentException("No hay nodo en (" + f + "," + c + ")");
        n[f][c].tipo = tipo;
    }

    /** Elimina el nodo, las personas sobre el y las conexiones que lo tocan. */
    public void eliminarNodo(int f, int c) {
        int id = f * cols + c;
        n[f][c].tipo = TipoNodo.VACIO;
        n[f][c].bloqueado = false;
        idANodo.values().removeIf(v -> v == id);
        conexiones.removeIf(k -> k.origen == id || k.destino == id);
        personas.removeIf(p -> p.f == f && p.c == c);
    }

    public void bloquear(int f, int c, boolean valor) {
        if (n[f][c].tipo == TipoNodo.VACIO) throw new IllegalArgumentException("No hay nodo en (" + f + "," + c + ")");
        n[f][c].bloqueado = valor;
    }

    // ---------- conexiones (RN-03) ----------

    public Conexion conectar(int f1, int c1, int f2, int c2, double tiempo, int capacidad) {
        if (!dentro(f1, c1) || !dentro(f2, c2) || n[f1][c1].tipo == TipoNodo.VACIO || n[f2][c2].tipo == TipoNodo.VACIO)
            throw new IllegalArgumentException("Las conexiones deben unir dos nodos existentes.");
        int a = f1 * cols + c1, b = f2 * cols + c2;
        conexiones.removeIf(k -> (k.origen == a && k.destino == b) || (k.origen == b && k.destino == a));
        Conexion k = new Conexion("C" + a + "_" + b, a, b, tiempo, capacidad);
        conexiones.add(k);
        return k;
    }

    public void desconectar(int f1, int c1, int f2, int c2) {
        int a = f1 * cols + c1, b = f2 * cols + c2;
        conexiones.removeIf(k -> (k.origen == a && k.destino == b) || (k.origen == b && k.destino == a));
    }

    public void bloquearConexion(int f1, int c1, int f2, int c2, boolean valor) {
        int a = f1 * cols + c1, b = f2 * cols + c2;
        for (Conexion k : conexiones)
            if ((k.origen == a && k.destino == b) || (k.origen == b && k.destino == a)) k.bloqueada = valor;
    }

    /** Copia profunda (nodos, bloqueos, conexiones y personas) para simular sin alterar el original. */
    public Edificio copia() {
        return copiaConPisos(pisos);
    }

    /** Copia profunda con otra cantidad de pisos (los pisos nuevos quedan vacios al final). */
    public Edificio copiaConPisos(int nuevosPisos) {
        if (nuevosPisos < pisos) throw new IllegalArgumentException("No se pueden quitar pisos con esta operacion.");
        Edificio e = new Edificio(filasPorPiso, cols, nuevosPisos);
        for (int i = 0; i < filas; i++)
            for (int j = 0; j < cols; j++) {
                e.n[i][j].tipo = n[i][j].tipo;
                e.n[i][j].bloqueado = n[i][j].bloqueado;
            }
        e.idANodo.putAll(idANodo);
        e.conexionesExplicitas = conexionesExplicitas;
        for (Conexion k : conexiones) {
            Conexion nk = new Conexion(k.id, k.origen, k.destino, k.tiempo, k.capacidad);
            nk.bloqueada = k.bloqueada;
            e.conexiones.add(nk);
        }
        for (Persona p : personas) e.agregar(p.tipo, p.salud, p.f, p.c);
        return e;
    }
}
