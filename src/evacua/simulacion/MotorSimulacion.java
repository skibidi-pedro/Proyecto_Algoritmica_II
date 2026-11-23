package evacua.simulacion;

import evacua.edificio.Edificio;
import evacua.estructuras.Cola;
import evacua.personas.NivelEstres;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.Adulto;
import evacua.personas.TipoPersona;
import evacua.edificio.Grafo;
import evacua.rutas.AlgoritmoRuta;
import evacua.rutas.AEstrella;
import evacua.rutas.Dijkstra;
import evacua.rutas.ResultadoRuta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MotorSimulacion {
    public final Edificio mapa;
    public final ParametrosSimulacion params;
    public final Random rnd;
    public final java.util.function.Consumer<String> log;
    public Grafo grafo;
    private Desastre desastre;
    private final java.util.Map<Long, Integer> cruces = new java.util.HashMap<>();
    private final AlgoritmoRuta dijkstra = new Dijkstra(), estrella = new AEstrella();

    private long enlace(int a, int b) {
        return (long) Math.min(a, b) * grafo.total() + Math.max(a, b);
    }

    private boolean sinCupo(int a, int b) {
        Integer usados = cruces.get(enlace(a, b));
        return usados != null && usados >= grafo.arista(a, b).capacidad;
    }

    private void actualizarOcupacion() {
        for (Nodo[] fila : mapa.n) for (Nodo n : fila) n.ocupacion = 0;
        for (Persona p : mapa.personas) if (!p.evacuada) mapa.n[p.f][p.c].ocupacion += p.tipo.ocupacion();
    }
    public int tick;
    public boolean terminado;

    public Cola<RegistroSalida> salidas = new Cola<>();

    public Runnable alTerminar;
    public int exploradosTotal;

    public MotorSimulacion(Edificio mapa, ParametrosSimulacion params, java.util.function.Consumer<String> log) {
        this.mapa = mapa;
        this.params = params;
        this.rnd = new Random(params.semilla);
        this.log = log;
    }

    public void iniciar() {
        grafo = Grafo.desde(mapa);
        desastre = params.desastre.crear();
        tick = 0;
        terminado = false;
        salidas = new Cola<>();
        for (Persona p : mapa.personas) {
            p.evacuada = false;
            p.asistida = false;
            p.ayudaFamiliar = false;
            p.objetivo = null;
            if (p instanceof Adulto) ((Adulto) p).evacuacionIndividual = false;
            p.estres = 0;
            p.bitacora.reiniciar();
            p.historial.reiniciar();
            p.acum = 0;
            p.esperando = 0;
            p.hist.clear();
            p.hist.add("t=0 inicio en (" + p.f + "," + p.c + ")");
            p.ruta.clear();
        }
        cruces.clear();
        exploradosTotal = 0;
        actualizarOcupacion();
        for (Persona p : mapa.personas) {
            p.bitacora.registrar(0, p.f, p.c, ControlCapacidad.estado(mapa.n[p.f][p.c], params.capacidad));
        }
        desastre.iniciar(this);
        for (Persona p : mapa.personas) asignarRuta(p);
        log.accept("Emergencia iniciada con " + mapa.personas.size() + " personas ("
                + (params.astar ? "A*" : "Dijkstra") + ", semilla " + params.semilla + ").");
    }

    public void asignarRuta(Persona p) {
        if (p.evacuada) return;
        if (p.salud == EstadoSalud.INCAPACITADO && !p.asistida && !p.ayudaFamiliar) {
            p.objetivo = null;
            p.ruta.clear();
            return;
        }
        p.objetivo = p.elegirObjetivo(mapa.personas);
        List<int[]> metas = mapa.salidas();
        ResultadoRuta r = null;
        if (p.objetivo != null) {
            List<int[]> rescate = metasRescate(p, p.objetivo);
            r = buscar(p, rescate);
            if (r.ruta != null) metas = rescate;
            else if (p.tipo == TipoPersona.ADULTO) {
                p.objetivo.ayudaFamiliar = false;
                p.ruta.clear();
                return;
            } else p.objetivo = null;
        }
        if (p.objetivo == null) {
            if (desastre instanceof Inundacion && ((Inundacion) desastre).pisoAfectado >= mapa.piso(p.f)) {
                List<int[]> altas = new ArrayList<>(metas);
                altas.removeIf(q -> mapa.piso(q[0]) <= ((Inundacion) desastre).pisoAfectado);
                if (!altas.isEmpty()) {
                    r = buscar(p, altas);
                    if (r.ruta != null) metas = altas;
                }
            }
            if (r == null || r.ruta == null) r = buscar(p, metas);
        }
        if (r.ruta == null && p.ruta.isEmpty()) return;
        if (r.ruta != null && rutaEquivalente(p, metas, r.costo)) return;
        p.ruta = r.ruta == null ? new ArrayList<>() : new ArrayList<>(r.ruta);
        if (tick > 0) {
            p.historial.recalculos++;
            p.hist.add("t=" + tick + (p.ruta.isEmpty() ? " espera sin ruta viable" : " cambia de ruta"));
        }
    }

    private boolean sinEspacio(Persona persona, int id) {
        Nodo n = mapa.n[id / mapa.cols][id % mapa.cols];
        return n.ocupacion + persona.tipo.ocupacion() > n.capacidadEfectiva(params.capacidad);
    }

    private ResultadoRuta buscar(Persona p, List<int[]> metas) {
        ResultadoRuta r = evaluarRuta(p, metas, params.astar);
        exploradosTotal += r.explorados;
        return r;
    }

    public ResultadoRuta evaluarRuta(Persona p, List<int[]> metas, boolean astar) {
        return (astar ? estrella : dijkstra).buscar(mapa, grafo, p.f, p.c, metas, id -> sinEspacio(p, id), this::sinCupo);
    }

    private boolean rutaEquivalente(Persona p, List<int[]> metas, double optimo) {
        int id = p.f * mapa.cols + p.c;
        double costo = 0;
        for (int[] q : p.ruta) {
            int destino = q[0] * mapa.cols + q[1];
            Grafo.Arista a = grafo.arista(id, destino);
            if (a == null || a.bloqueada() || !mapa.caminable(q[0], q[1]) || sinEspacio(p, destino)
                    || sinCupo(id, destino)) return false;
            costo += a.costoEntrada(mapa.n[q[0]][q[1]]);
            id = destino;
        }
        if (costo > optimo + 4 * Math.ulp(optimo)) return false;
        for (int[] q : metas) if (id == q[0] * mapa.cols + q[1]) return true;
        return false;
    }

    private List<int[]> metasRescate(Persona p, Persona objetivo) {
        int destino = objetivo.f * mapa.cols + objetivo.c;
        List<int[]> metas = new ArrayList<>();
        metas.add(new int[]{objetivo.f, objetivo.c});
        // Con un paciente en la última plaza, la asistencia se presta desde el nodo contiguo.
        if (sinEspacio(p, destino) && mapa.caminable(objetivo.f, objetivo.c))
            for (Grafo.Arista a : grafo.ady[destino])
                if (!a.bloqueada()) metas.add(new int[]{a.destino / mapa.cols, a.destino % mapa.cols});
        return metas;
    }

    public void recalcularTodas() {
        for (Persona p : mapa.personas) asignarRuta(p);
    }

    private void repararBloqueo(Persona p) {
        if (p.tipo != TipoPersona.RESCATE || p.salud == EstadoSalud.INCAPACITADO) return;
        for (Grafo.Arista a : grafo.ady[p.f * mapa.cols + p.c]) {
            Nodo n = mapa.n[a.destino / mapa.cols][a.destino % mapa.cols];
            if (a.bloqueada() || !n.bloqueado || !n.bloqueoReparable) continue;
            mapa.bloquear(a.destino / mapa.cols, a.destino % mapa.cols, false);
            p.hist.add("t=" + tick + " repara el bloqueo en " + mapa.idDe(a.destino / mapa.cols, a.destino % mapa.cols));
            break;
        }
    }

    public void generarEvento() {
        desastre.evento(this);
    }

    public void alBloquear(int f, int c) {
        contarBloqueo(f, c);
        recalcularTodas();
    }

    private void contarBloqueo(int f, int c) {
        for (Persona p : mapa.personas) {
            if (p.evacuada) continue;
            for (int[] q : p.ruta)
                if (q[0] == f && q[1] == c) {
                    p.historial.bloqueosSufridos++;
                    break;
                }
        }
    }

    private boolean cercaDePeligro(Persona p) {
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] x : d) {
            int nf = p.f + x[0], nc = p.c + x[1];
            if (mapa.dentro(nf, nc) && mapa.piso(nf) == mapa.piso(p.f) && mapa.n[nf][nc].tipo != TipoNodo.VACIO && mapa.n[nf][nc].bloqueado) return true;
        }
        return false;
    }

    private void chequearRescate(Persona p) {
        Persona o = p.objetivo;
        if (o == null || o.evacuada) return;
        if (p.tipo == TipoPersona.ADULTO) o.ayudaFamiliar = false;
        if (!mapa.caminable(o.f, o.c) || !mapa.caminable(p.f, p.c)) return;
        for (int[] q : metasRescate(p, o)) {
            if (q[0] != p.f || q[1] != p.c) continue;
            if (p.tipo == TipoPersona.ADULTO) {
                o.ayudaFamiliar = !o.asistida;
                return;
            }
            if (o.asistida) return;
            o.ayudaFamiliar = false;
            o.asistida = true;
            o.hist.add("t=" + tick + " asistida por P" + p.id);
            p.hist.add("t=" + tick + " alcanza a P" + o.id + " y lo asiste");
            log.accept("t=" + tick + " P" + p.id + " (rescate) asiste a P" + o.id + ".");
            asignarRuta(o);
            asignarRuta(p);
            return;
        }
    }

    public boolean paso() {
        if (terminado) return false;
        tick++;
        cruces.clear();
        actualizarOcupacion();
        desastre.paso(this);
        for (Persona p : mapa.personas) {
            if (p.evacuada) continue;
            repararBloqueo(p);
            asignarRuta(p);
            chequearRescate(p);
            if (p.objetivo == null && (p.salud != EstadoSalud.INCAPACITADO || p.asistida || p.ayudaFamiliar)
                    && mapa.caminable(p.f, p.c) && mapa.n[p.f][p.c].tipo == TipoNodo.SALIDA) {
                evacuar(p);
                continue;
            }
            double v = p.tipo.vel * p.salud.vel;
            if (p.salud == EstadoSalud.INCAPACITADO) v = p.asistida ? 0.5 : (p.ayudaFamiliar ? 0.25 : 0);
            v *= desastre.velocidad(this, p);
            if (mapa.n[p.f][p.c].peligroso) {
                p.aumentarEstres(params.intensidad);
                v *= 0.65;
            }
            if (NivelEstres.supera(p.estres, params.umbral)) v *= 0.6;
            if (cercaDePeligro(p)) p.aumentarEstres(0.1 * params.intensidad);
            if (p instanceof Adulto && ((Adulto) p).evacuacionIndividual) asignarRuta(p);
            if (v <= 0) {
                p.aumentarEstres(0.3);
                continue;
            }
            p.acum += v;
            boolean movio = false;
            while (p.acum >= 1 && !p.evacuada) {
                p.acum -= 1;
                if (p.ruta.isEmpty() && p.tipo == TipoPersona.ADULTO && p.objetivo != null
                        && (p.objetivo.ayudaFamiliar || p.objetivo.asistida)) {
                    Persona familiar = p.objetivo;
                    ResultadoRuta salida = (params.astar ? estrella : dijkstra).buscar(mapa, grafo, familiar.f, familiar.c, mapa.salidas());
                    if (salida.ruta == null || salida.ruta.isEmpty()) break;
                    int[] siguiente = salida.ruta.get(0);
                    if (siguiente[0] != p.f || siguiente[1] != p.c) break;
                    if (mapa.n[p.f][p.c].tipo == TipoNodo.SALIDA) {
                        evacuar(p);
                        break;
                    }
                    ResultadoRuta despejar = buscar(p, mapa.salidas());
                    if (despejar.ruta == null || despejar.ruta.isEmpty()) break;
                    p.ruta = new ArrayList<>(despejar.ruta);
                }
                if (p.ruta.isEmpty()) {
                    p.aumentarEstres(0.5);
                    p.esperando++;
                    registrarEspera(p);
                    break;
                }
                int[] sig = p.ruta.get(0);
                Nodo ns = mapa.n[sig[0]][sig[1]];
                int origen = p.f * mapa.cols + p.c, destino = sig[0] * mapa.cols + sig[1];
                Grafo.Arista arista = grafo.arista(origen, destino);
                if (!mapa.caminable(sig[0], sig[1]) || arista == null || arista.bloqueada()) {
                    p.hist.add("t=" + tick + " ruta bloqueada en (" + sig[0] + "," + sig[1] + "), recalcula");
                    p.historial.recalculos++;
                    p.historial.bloqueosSufridos++;
                    asignarRuta(p);
                    break;
                }
                long enlace = enlace(origen, destino);
                int usados = cruces.getOrDefault(enlace, 0);
                cruces.put(enlace, usados + 1);
                if (arista.peligrosa()) p.aumentarEstres(params.intensidad);
                mapa.n[p.f][p.c].ocupacion -= p.tipo.ocupacion();
                p.f = sig[0];
                p.c = sig[1];
                ns.ocupacion += p.tipo.ocupacion();
                p.ruta.remove(0);
                p.pasos++;
                p.esperando = 0;
                movio = true;
                p.hist.add("t=" + tick + " -> (" + p.f + "," + p.c + ") " + ns.tipo.nombre);
                p.bitacora.registrar(tick, p.f, p.c, ControlCapacidad.estado(ns, params.capacidad));
                if (ns.tipo == TipoNodo.SALIDA && p.objetivo == null) {
                    evacuar(p);
                } else {
                    chequearRescate(p);
                    if (p.acum >= 1) asignarRuta(p);
                }
            }
            if (movio) p.reducirEstres(0.2);
        }
        boolean todas = true;
        for (Persona p : mapa.personas) {
            if (!p.evacuada || p.tEvac == tick) mapa.n[p.f][p.c].visitas++;
            if (!p.evacuada) todas = false;
        }
        if (todas || tick >= params.limite) {
            terminado = true;
            for (Persona p : mapa.personas)
                if (!p.evacuada) p.hist.add("t=" + tick + " NO EVACUADA (tiempo limite)");
            if (alTerminar != null) alTerminar.run();
            log.accept("Simulacion terminada en t=" + tick + ": " + evacuadas() + "/" + mapa.personas.size() + " evacuadas.");
        }
        return !terminado;
    }

    private void registrarEspera(Persona p) {
        // Distingue un cuello de botella temporal de una zona aislada por bloqueos.
        List<int[]> metas = p.objetivo == null ? mapa.salidas() : metasRescate(p, p.objetivo);
        ResultadoRuta libre = (params.astar ? estrella : dijkstra).buscar(mapa, grafo, p.f, p.c, metas);
        exploradosTotal += libre.explorados;
        if (libre.ruta == null) return;
        int origen = p.f * mapa.cols + p.c;
        for (int[] q : libre.ruta) {
            int destino = q[0] * mapa.cols + q[1];
            if (sinEspacio(p, destino) || sinCupo(origen, destino)) {
                mapa.n[q[0]][q[1]].esperas++;
                p.historial.esperaCuelloBotella++;
                return;
            }
            origen = destino;
        }
    }

    private void evacuar(Persona p) {
        if (p instanceof Adulto && p.objetivo != null) p.objetivo.ayudaFamiliar = false;
        p.objetivo = null;
        p.evacuada = true;
        p.tEvac = tick;
        mapa.n[p.f][p.c].ocupacion -= p.tipo.ocupacion();
        p.hist.add("t=" + tick + " EVACUADA");
        salidas.encolar(new RegistroSalida(salidas.tamano() + 1, p, tick));
    }

    public int evacuadas() {
        int k = 0;
        for (Persona p : mapa.personas) if (p.evacuada) k++;
        return k;
    }

    public double promedioEvac() {
        double s = 0;
        int k = 0;
        for (Persona p : mapa.personas)
            if (p.evacuada) {
                s += p.tEvac;
                k++;
            }
        return k == 0 ? 0 : s / k;
    }

    public double promedioEstres() {
        if (mapa.personas.isEmpty()) return 0;
        double s = 0;
        for (Persona p : mapa.personas) s += p.estres;
        return s / mapa.personas.size();
    }

}
