package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import evacua.rutas.ResultadoRuta;
import java.util.*;

/** Una fuente, una reserva de excedente y la topología compartida del edificio. */
public final class FugaGas {
    private final MotorSimulacion m;
    final int origen, emision;
    final TipoGas tipo;
    private final Nodo[] nodos;
    private final double[] gas;
    private final BitSet yaLlenados = new BitSet();
    private boolean reparada;
    private int reparacion;
    private int tickExposicion = -1, tickReparacion = -1;
    private double pendiente, generado, ventilado;
    private boolean cambio;
    private String situacionAnterior;

    FugaGas(MotorSimulacion m, int origen) {
        this.m = m;
        this.origen = origen;
        tipo = Objects.requireNonNull(m.params.gas, "Seleccionar el compuesto");
        emision = 10 + 5 * m.params.nivel.ordinal();
        nodos = new Nodo[m.grafo.total()];
        gas = new double[nodos.length];
        for (int i = 0; i < nodos.length; i++) {
            nodos[i] = m.mapa.n[i / m.mapa.cols][i % m.mapa.cols];
            nodos[i].fijarGas(0);
        }
    }

    public boolean reparada() { return reparada; }
    public int progreso() { return reparacion; }
    public double pendiente() { return pendiente; }
    public double generado() { return generado; }
    public double ventilado() { return ventilado; }

    private boolean abierto(int i) { return nodos[i].tipo != TipoNodo.VACIO && !nodos[i].bloqueado; }
    private boolean permite(int desde, int hacia) {
        return nodos[hacia].tipo != TipoNodo.PUERTA || nodos[hacia].danado || gas[desde] >= 100;
    }

    void ambiente() {
        for (int i = 0; i < gas.length; i++) gas[i] = nodos[i].concentracionGas();
        if (!reparada) { pendiente += emision; generado += emision; }
        distribuir();
        Difusion.aplicar(m.grafo, gas, this::abierto, this::permite);
        cambio = false;
        for (int i = 0; i < gas.length; i++) {
            double restante = Difusion.ventilar(nodos[i], gas[i]);
            ventilado += gas[i] - restante;
            cambio |= Math.abs(restante - nodos[i].concentracionGas()) > 1e-9;
            nodos[i].fijarGas(restante);
        }
    }

    private void distribuir() {
        if (pendiente <= 0 || !abierto(origen)) return;
        // Cada capa recibe el excedente en proporción a su espacio; solo una capa llena abre la siguiente.
        List<Integer> capa = Collections.singletonList(origen);
        BitSet visitados = new BitSet(gas.length);
        visitados.set(origen);
        while (pendiente > 0 && !capa.isEmpty()) {
            double libre = 0;
            for (int id : capa) libre += 100 - gas[id];
            double reparto = Math.min(libre, pendiente);
            boolean estreno = false;
            for (int id : capa) {
                if (libre > 0) gas[id] = reparto == libre ? 100 : gas[id] + (100 - gas[id]) * (reparto / libre);
                if (gas[id] == 100) {
                    estreno |= id != origen && !yaLlenados.get(id) && libre > 0;
                    yaLlenados.set(id);
                }
            }
            pendiente -= reparto;
            if (pendiente <= 0) break;
            // El excedente acumulado puede atravesar capas ya llenas, pero solo llena
            // una capa nueva más allá de la fuente por tick.
            if (estreno) break;
            List<Integer> siguiente = new ArrayList<>();
            for (int id : capa) for (Grafo.Arista a : m.grafo.ady[id]) {
                int j = a.destino;
                if (a.bloqueada() || visitados.get(j) || !abierto(j) || !permite(id,j)) continue;
                visitados.set(j);
                siguiente.add(j);
            }
            capa = siguiente;
        }
    }

    void personas() {
        if (tickExposicion == m.tick) return;
        tickExposicion = m.tick;
        Set<Integer> avisos = new HashSet<>();
        for (Persona p : m.mapa.personas) {
            if (!p.activa()) continue;
            double valor = m.mapa.n[p.f][p.c].concentracionGas();
            if (p instanceof Hazmat) {
                if (valor > 0) m.emergencia().solicitarRefuerzos(m);
                continue;
            }
            int bandas = valor >= 95 ? 4 : valor >= 75 ? 3 : valor >= 50 ? 2 : valor > 10 ? 1 : 0;
            int[] t = p.exposicionGas;
            for (int j = 0; j < t.length; j++) t[j] = j < bandas ? t[j] + 1 : 0;
            if (t[3] >= tipo.muerte) {
                m.marcarFallecimiento(p, tipo.toString());
                continue;
            }
            if (TipoGas.alcanza(t,tipo.herida) && p.salud != EstadoSalud.INCAPACITADO && p.salud != EstadoSalud.HERIDO) {
                p.salud = EstadoSalud.HERIDO;
                p.hist.add("t=" + m.tick + " HERIDO por gas");
            }
            p.confusion = p.salud != EstadoSalud.HERIDO && p.salud != EstadoSalud.INCAPACITADO
                    && TipoGas.alcanza(t,tipo.confusion) ? 1 : 0;
            if (!TipoGas.alcanza(t,tipo.aviso)) continue;
            p.aumentarEstres(tipo.estresBase + m.params.nivel.ordinal());
            if (!p.alertada) p.hist.add("t=" + m.tick + " detecta la fuga");
            p.alertada = true;
            if (p.departamentoInicial > 0) avisos.add(p.departamentoInicial);
            m.emergencia().solicitarRefuerzos(m);
        }
        Desastre.alertarDepartamentos(m.mapa.personas, avisos, m.tick);
        for (Persona p : m.mapa.personas) avisarDesde(p);
    }

    void avisarDesde(Persona r) {
        if (!(r instanceof PersonalRescate) || !r.activa() || !r.alertada || r.confundida()) return;
        int dep = m.mapa.n[r.f][r.c].departamento;
        Set<Integer> avisos = new HashSet<>();
        if (dep > 0) avisos.add(dep);
        for (Persona p : m.mapa.personas) if (p.activa() && !p.alertada && p.f == r.f && p.c == r.c) {
            p.alertada = true;
            p.hist.add("t=" + m.tick + " aviso directo de P" + r.id);
            if (p.departamentoInicial > 0) avisos.add(p.departamentoInicial);
        }
        if (!avisos.isEmpty()) Desastre.alertarDepartamentos(m.mapa.personas, avisos, m.tick);
    }

    List<int[]> metasAviso() {
        Set<Integer> deps = new HashSet<>();
        List<int[]> metas = new ArrayList<>();
        for (Persona p : m.mapa.personas) if (p.activa() && !p.alertada && !(p instanceof PersonalRescate)) {
            if (p.departamentoInicial > 0) deps.add(p.departamentoInicial);
            metas.add(new int[]{p.f,p.c});
        }
        if (!deps.isEmpty()) for (int i = 0; i < nodos.length; i++) if (deps.contains(nodos[i].departamento))
            metas.add(new int[]{i / m.mapa.cols, i % m.mapa.cols});
        return metas;
    }

    boolean encargado(Persona p) { return p instanceof Hazmat && m.intervenciones.get(origen) == p && !((Hazmat)p).retirado && !reparada; }
    List<int[]> fuente() { return Collections.singletonList(new int[]{origen / m.mapa.cols, origen % m.mapa.cols}); }

    void asignarReparacion() {
        if (reparada) return;
        PersonalRescate anterior = m.intervenciones.remove(origen);
        Hazmat encargado = null;
        double mejor = Double.POSITIVE_INFINITY;
        for (Persona p : m.mapa.personas) {
            if (!(p instanceof Hazmat) || !p.activa() || p.asistida) continue;
            Hazmat h = (Hazmat)p;
            if (h.retirado || h.transportando || h.salud == EstadoSalud.INCAPACITADO) continue;
            ResultadoRuta r = m.evaluarRuta(h, fuente(), m.params.astar);
            if (r.ruta == null) continue;
            if (h == anterior) { encargado = h; break; }
            if (r.costo < mejor) { mejor = r.costo; encargado = h; }
        }
        if (encargado != null) {
            m.intervenciones.put(origen, encargado);
            encargado.objetivo = null;
        }
    }

    boolean reparar(Persona p) {
        if (!encargado(p) || p.f * m.mapa.cols + p.c != origen) return false;
        if (tickReparacion == m.tick) return true;
        tickReparacion = m.tick;
        if (++reparacion == 2) {
            reparada = true;
            m.intervenciones.remove(origen);
            m.log.accept("t=" + m.tick + " fuente reparada por P" + p.id);
        }
        return true;
    }

    boolean requierePersonal() {
        if (!reparada) return true;
        for (Persona p : m.mapa.personas) if (p.activa() && !(p instanceof PersonalRescate)
                && (!p.alertada || p.confundida() || p.salud != EstadoSalud.SANO || p.tipo != TipoPersona.ADULTO)) return true;
        return false;
    }

    String resultado(boolean todosFuera) {
        if (reparada && todosFuera) return "Fuente reparada; todos los supervivientes evacuaron.";
        if (m.emergencia().tickLlegada() < 0 && !deteccionPosible()) return "Sin progreso: nadie puede detectar la fuga.";
        if (cambio || m.emergencia().restantes(m.tick) > 0 || m.emergencia().reemplazoEnCamino(m.tick)) {
            situacionAnterior = null;
            return "";
        }
        // Con concentraciones estacionarias, comprobar también acciones, síntomas en curso y arribos.
        StringBuilder s = new StringBuilder().append(reparacion).append('/').append(m.emergencia().refuerzosPendientes());
        for (Persona p : m.mapa.personas) if (p.activa()) {
            s.append('|').append(p.id).append('/').append(p.f).append('/').append(p.c).append('/')
                    .append(p.estres).append('/').append(p.alertada).append('/').append(p.salud).append('/')
                    .append(p.confusion).append('/').append(p.asistida).append('/').append(p.objetivo == null ? 0 : p.objetivo.id);
            for (int t : p.exposicionGas) s.append('/').append(Math.min(20,t));
            if (!p.ruta.isEmpty()) s.append('/').append(p.acum).append('/').append(p.ruta.size());
        }
        String actual = s.toString();
        boolean igual = actual.equals(situacionAnterior);
        situacionAnterior = actual;
        return igual ? "Sin progreso: fuente o supervivientes pendientes sin acción alcanzable." : "";
    }

    private boolean deteccionPosible() {
        BitSet vistos = new BitSet(nodos.length);
        ArrayDeque<Integer> cola = new ArrayDeque<>();
        cola.add(origen);
        vistos.set(origen);
        while (!cola.isEmpty()) {
            int id = cola.remove();
            for (Grafo.Arista a : m.grafo.ady[id]) if (!a.bloqueada() && abierto(a.destino) && !vistos.get(a.destino)) {
                vistos.set(a.destino);
                cola.add(a.destino);
            }
        }
        for (Persona p : m.mapa.personas) if (p.activa() && vistos.get(p.f*m.mapa.cols+p.c)) return true;
        return false;
    }
}
