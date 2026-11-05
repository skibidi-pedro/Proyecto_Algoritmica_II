package evacua.simulacion;

import evacua.edificio.Edificio;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;
import evacua.rutas.BuscadorRutas;
import evacua.rutas.ResultadoRuta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Motor de simulacion paso a paso (eventos aleatorios, recalculo de rutas y congestion). */
public class MotorSimulacion {
    public final Edificio mapa;
    public final ParametrosSimulacion params;
    public final Random rnd;
    public final java.util.function.Consumer<String> log;
    public int tick;
    public boolean terminado;
    public int exploradosTotal;

    public MotorSimulacion(Edificio mapa, ParametrosSimulacion params, java.util.function.Consumer<String> log) {
        this.mapa = mapa;
        this.params = params;
        this.rnd = new Random(params.semilla);
        this.log = log;
    }

    public void iniciar() {
        tick = 0;
        terminado = false;
        for (Persona p : mapa.personas) {
            p.evacuada = false;
            p.asistida = false;
            p.estres = 0;
            p.acum = 0;
            p.hist.clear();
            p.hist.add("t=0 inicio en (" + p.f + "," + p.c + ")");
        }
        for (Persona p : mapa.personas) asignarRuta(p);
        log.accept("Emergencia iniciada con " + mapa.personas.size() + " personas ("
                + (params.astar ? "A*" : "Dijkstra") + ", semilla " + params.semilla + ").");
    }

    public Persona masCercanoIncapacitado(Persona r) {
        Persona best = null;
        int bd = Integer.MAX_VALUE;
        for (Persona q : mapa.personas) {
            if (q.evacuada || q.asistida || q.salud != EstadoSalud.INCAPACITADO) continue;
            int d = Math.abs(q.f - r.f) + Math.abs(q.c - r.c);
            if (d < bd) {
                bd = d;
                best = q;
            }
        }
        return best;
    }

    public void asignarRuta(Persona p) {
        if (p.evacuada) return;
        p.objetivo = p.tipo == TipoPersona.RESCATE ? masCercanoIncapacitado(p) : null;
        if (p.salud == EstadoSalud.INCAPACITADO && !p.asistida) {
            p.ruta = new ArrayList<>();
            return;
        }
        ResultadoRuta r;
        if (p.objetivo != null) {
            r = BuscadorRutas.buscar(mapa, p.f, p.c, Collections.singletonList(new int[]{p.objetivo.f, p.objetivo.c}), params.astar);
            if (r.ruta == null) p.objetivo = null;
            else exploradosTotal += r.explorados;
        } else {
            r = null;
        }
        if (p.objetivo == null) {
            r = BuscadorRutas.buscar(mapa, p.f, p.c, mapa.salidas(), params.astar);
            exploradosTotal += r.explorados;
        }
        p.ruta = r.ruta == null ? new ArrayList<>() : new ArrayList<>(r.ruta);
    }

    public void recalcularTodas() {
        int k = 0;
        for (Persona p : mapa.personas) {
            if (p.evacuada) continue;
            asignarRuta(p);
            p.recalculos++;
            p.hist.add("t=" + tick + " ruta recalculada (" + p.ruta.size() + " pasos)");
            k++;
        }
        log.accept("t=" + tick + " rutas recalculadas para " + k + " personas.");
    }

    public void generarEvento() {
        List<int[]> cand = new ArrayList<>();
        for (int i = 0; i < mapa.filas; i++)
            for (int j = 0; j < mapa.cols; j++)
                if (mapa.n[i][j].tipo == TipoNodo.PASILLO && !mapa.n[i][j].bloqueado) cand.add(new int[]{i, j});
        if (cand.isEmpty()) {
            log.accept("No hay pasillos disponibles para el evento aleatorio.");
            return;
        }
        int[] q = cand.get(rnd.nextInt(cand.size()));
        mapa.n[q[0]][q[1]].bloqueado = true;
        log.accept("t=" + tick + " EVENTO: colapso del pasillo (" + q[0] + "," + q[1] + ").");
        recalcularTodas();
    }

    private void chequearRescate(Persona p) {
        Persona o = p.objetivo;
        if (o != null && !o.evacuada && p.f == o.f && p.c == o.c) {
            o.asistida = true;
            o.hist.add("t=" + tick + " asistida por P" + p.id);
            p.hist.add("t=" + tick + " alcanza a P" + o.id + " y lo asiste");
            log.accept("t=" + tick + " P" + p.id + " (rescate) asiste a P" + o.id + ".");
            asignarRuta(o);
            asignarRuta(p);
        }
    }

    public boolean paso() {
        if (terminado) return false;
        tick++;
        if (rnd.nextDouble() < params.prob) generarEvento();
        for (Nodo[] fila : mapa.n) for (Nodo nd : fila) nd.ocupacion = 0;
        for (Persona p : mapa.personas) if (!p.evacuada) mapa.n[p.f][p.c].ocupacion++;

        for (Persona p : mapa.personas) {
            if (p.evacuada) continue;
            double v = p.tipo.vel * p.salud.vel;
            if (p.salud == EstadoSalud.INCAPACITADO) v = p.asistida ? 0.5 : 0;
            if (p.estres >= params.umbral) v *= 0.6;
            if (v <= 0) {
                p.estres += 0.3;
                continue;
            }
            p.acum += v;
            boolean movio = false;
            while (p.acum >= 1 && !p.evacuada) {
                p.acum -= 1;
                chequearRescate(p);
                if (p.ruta.isEmpty()) {
                    asignarRuta(p);
                    if (p.ruta.isEmpty()) {
                        p.estres += 0.5;
                        break;
                    }
                }
                int[] sig = p.ruta.get(0);
                Nodo ns = mapa.n[sig[0]][sig[1]];
                if (ns.bloqueado) {
                    p.hist.add("t=" + tick + " ruta bloqueada en (" + sig[0] + "," + sig[1] + "), recalcula");
                    p.recalculos++;
                    asignarRuta(p);
                    break;
                }
                if (ns.tipo != TipoNodo.SALIDA && ns.ocupacion >= params.capacidad) {
                    p.estres += 1;
                    ns.esperas++;
                    break;
                }
                mapa.n[p.f][p.c].ocupacion--;
                p.f = sig[0];
                p.c = sig[1];
                ns.ocupacion++;
                p.ruta.remove(0);
                p.pasos++;
                movio = true;
                p.hist.add("t=" + tick + " -> (" + p.f + "," + p.c + ") " + ns.tipo.nombre);
                if (ns.tipo == TipoNodo.SALIDA && p.objetivo == null) {
                    p.evacuada = true;
                    p.tEvac = tick;
                    ns.ocupacion--;
                    p.hist.add("t=" + tick + " EVACUADA");
                } else {
                    chequearRescate(p);
                }
            }
            if (movio) p.estres = Math.max(0, p.estres - 0.2);
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
            log.accept("Simulacion terminada en t=" + tick + ": " + evacuadas() + "/" + mapa.personas.size() + " evacuadas.");
        }
        return !terminado;
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
