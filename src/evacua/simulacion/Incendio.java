package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import java.util.*;

/** Condiciones por tick sobre la misma topología que utilizan los agentes. */
final class Incendio {
    private final MotorSimulacion m;
    private final int ciclo, dano, puerta, estresFuego, estresVecino, estresHumo;
    private final double prob;
    final Nodo[] nodos;
    private final double[] humo;
    private final int[] ticksHumoCompleto;
    int focos;
    boolean cambioAmbiental;
    private String ultimaSituacion;

    Incendio(MotorSimulacion m, int origen, Intensidad nivel) {
        this.m = m;
        int i = nivel.ordinal();
        ciclo = 4 - i;
        prob = (4 + i) / 10.0;
        dano = 8 - 2 * i;
        puerta = 40;
        estresFuego = 4 + 2 * i;
        estresVecino = i == 0 ? 2 : 4;
        estresHumo = i == 2 ? 3 : 2;
        nodos = new Nodo[m.grafo.total()];
        humo = new double[nodos.length];
        ticksHumoCompleto = new int[nodos.length];
        for (int id = 0; id < nodos.length; id++) {
            Nodo n = nodos[id] = m.mapa.n[id / m.mapa.cols][id % m.mapa.cols];
            n.fuegoActivo = n.protegido = false;
            n.humo = n.extincion = n.ticksArdiendo = n.calorPuerta = 0;
        }
        nodos[origen].fuegoActivo = true;
        focos = 1;
    }

    boolean fuegoVecino(int id, boolean deteccion) {
        if (deteccion && !nodos[id].permeable()) return false;
        for (Grafo.Arista a : m.grafo.ady[id])
            if (!a.bloqueada() && nodos[a.destino].fuegoActivo && (!deteccion || nodos[a.destino].permeable())) return true;
        return false;
    }

    private boolean saturado(int id) {
        if (nodos[id].humo < 99.5) return false;
        if (!nodos[id].permeable()) return true;
        for (Grafo.Arista a : m.grafo.ady[id])
            if (!a.bloqueada() && nodos[a.destino].permeable() && nodos[a.destino].humo < 99.5) return false;
        return true;
    }

    void ambiente() {
        cambioAmbiental = false;
        for (int i = 0; i < nodos.length; i++) {
            Nodo n = nodos[i];
            humo[i] = n.fuegoActivo ? Math.min(100, n.humo + 1 + m.rnd.nextInt(5)) : n.humo;
        }
        Difusion.aplicar(m.grafo, humo, i -> nodos[i].tipo != TipoNodo.VACIO && !nodos[i].bloqueado,
                (desde, hacia) -> Difusion.permiteEntrada(nodos[hacia], humo[desde]));
        for (int i = 0; i < nodos.length; i++) {
            Nodo n = nodos[i];
            double nuevo = Difusion.ventilar(n, humo[i]);
            cambioAmbiental |= Math.abs(nuevo - n.humo) > 1e-9;
            n.humo = nuevo;
            if (n.fuegoActivo && ++n.ticksArdiendo >= dano) n.danado = true;
            if (n.tipo == TipoNodo.PUERTA && !n.danado) {
                n.calorPuerta = fuegoVecino(i, false) ? n.calorPuerta + 1 : 0;
                if (n.calorPuerta >= puerta) n.danado = true;
            }
        }
        for (int i = 0; i < nodos.length; i++) {
            ticksHumoCompleto[i] = nodos[i].fuegoActivo && nodos[i].humo == 100 ? ticksHumoCompleto[i] + 1 : 0;
            if (ticksHumoCompleto[i] == 20) apagar(i, false);
        }
        if (m.tick % ciclo != 0) return;
        BitSet nuevos = new BitSet(nodos.length);
        for (int i = 0; i < nodos.length; i++) {
            Nodo n = nodos[i];
            if (!n.fuegoActivo && !n.protegido && n.combustible() && fuegoVecino(i, false)
                    && !saturado(i) && m.rnd.nextDouble() < prob) nuevos.set(i);
        }
        for (int i = nuevos.nextSetBit(0); i >= 0; i = nuevos.nextSetBit(i + 1)) {
            nodos[i].fuegoActivo = true;
            focos++;
        }
    }

    String resultado(boolean todosFuera) {
        if (todosFuera && focos == 0) {
            if (m.evacuadas() == m.residentes()) return "Todos evacuaron y el incendio está extinguido.";
            return "Incendio extinguido con fallecidos. Evacuaron todos los supervivientes.";
        }
        if (m.emergencia().alarmaPendiente(m.tick)) return "";
        if (m.emergencia().haySolicitudes() && m.emergencia().restantes(m.tick) == 0 && accesosOcupadosPermanentemente()) {
            boolean enMarcha = m.mapa.personas.stream().anyMatch(p -> p.activa()
                    && (!p.ruta.isEmpty() || p.asistida || p.ticksFuego > 0 || p.ticksHumo > 0 || p instanceof Bombero));
            if (!enMarcha) {
                return "Sin progreso: ningún acceso tiene capacidad física para los refuerzos.";
            }
        }
        if (focos == 0) {
            if (m.emergencia().haySolicitudes() || cambioAmbiental) {
                ultimaSituacion = null;
                return "";
            }
            // Tras extinguirse el fuego no hay sorteos. Verificar un punto fijo del movimiento,
            // sin confundir una velocidad fraccionaria o un futuro daño por humo con un atasco.
            StringBuilder situacion = new StringBuilder();
            for (Persona p : m.mapa.personas) if (p.activa()) {
                if (nodos[p.f * m.mapa.cols + p.c].humo >= 95 && !(p instanceof Bombero)) return "";
                situacion.append(p.id).append('/').append(p.f).append('/').append(p.c).append('/')
                        .append(p.estres).append('/').append(p.alertada).append('/').append(p.salud)
                        .append('/').append(p.asistida).append('/').append(p.ayudaFamiliar)
                        .append('/').append(p.objetivo == null ? 0 : p.objetivo.id);
                if (!p.ruta.isEmpty()) situacion.append('/').append(p.acum).append('/').append(p.ruta.size());
                situacion.append(';');
            }
            String actual = situacion.toString();
            boolean inmovil = actual.equals(ultimaSituacion);
            ultimaSituacion = actual;
            if (!inmovil) return "";
            return "Sin progreso: quedan supervivientes sin evacuación posible.";
        }
        // Una salida en llamas siempre queda por debajo de saturación después de ventilar.
        // Sin unidades disponibles ni posibilidad de que alguien dé el aviso, no podrá apagarse.
        if (!m.emergencia().haySolicitudes() && m.mapa.personas.stream().noneMatch(p -> p.activa() && p instanceof Bombero)) {
            for (Nodo n : nodos) if (n.tipo == TipoNodo.SALIDA && !n.bloqueado && n.fuegoActivo
                    && (m.emergencia().tickLlegada() >= 0 || !avisoPosible())) {
                for (Persona p : m.mapa.personas) if (p.activa() && (p.asistida || !p.ruta.isEmpty()
                        || p.ticksHumo > 0 || p.ticksFuego > 0)) return "";
                return "Sin progreso: fuego ventilado sin intervención posible.";
            }
        }
        return "";
    }

    private boolean accesosOcupadosPermanentemente() {
        double[] cuerpos = new double[nodos.length];
        for (Persona p : m.mapa.personas) if (p.tickMuerte >= 0)
            cuerpos[p.f * m.mapa.cols + p.c] += p.tipo.ocupacion();
        for (int[] q : m.mapa.salidas(true)) {
            int i = q[0] * m.mapa.cols + q[1];
            if (cuerpos[i] + TipoPersona.RESCATE.ocupacion() <= nodos[i].capacidadEfectiva(m.params.capacidad, true)) return false;
        }
        return true;
    }

    private boolean avisoPosible() {
        BitSet visitados = new BitSet(nodos.length);
        ArrayDeque<Integer> cola = new ArrayDeque<>();
        for (int i = 0; i < nodos.length; i++) if (nodos[i].fuegoActivo || nodos[i].humo > 10) {
            visitados.set(i);
            cola.add(i);
        }
        while (!cola.isEmpty()) {
            for (Grafo.Arista a : m.grafo.ady[cola.remove()]) if (!a.bloqueada() && !visitados.get(a.destino)) {
                visitados.set(a.destino);
                cola.add(a.destino);
            }
        }
        for (Persona p : m.mapa.personas) if (p.activa() && visitados.get(p.f * m.mapa.cols + p.c)) return true;
        return false;
    }

    void apagar(int id, boolean profesional) {
        Nodo n = nodos[id];
        n.fuegoActivo = false;
        ticksHumoCompleto[id] = 0;
        n.ticksArdiendo = n.extincion = 0;
        n.protegido = profesional;
        focos--;
        m.log.accept("t=" + m.tick + (profesional ? " extinción por bombero en " : " extinción natural en ") + id);
    }

    void personas() {
        Set<Integer> departamentos = new HashSet<>();
        for (Persona p : m.mapa.personas) {
            if (!p.activa()) continue;
            int id = p.f * m.mapa.cols + p.c;
            Nodo n = nodos[id];
            boolean cerca = fuegoVecino(id, true);
            if (n.fuegoActivo || cerca || n.humo > 10) {
                m.emergencia().alertar(p, m.tick, "detecta el incendio");
                if (p.departamentoInicial > 0) departamentos.add(p.departamentoInicial);
                m.emergencia().solicitarRefuerzos(m);
            }
            if (p instanceof Bombero) continue;
            p.aumentarEstres(Math.max(n.fuegoActivo ? estresFuego : 0, Math.max(cerca ? estresVecino : 0, n.humo > 10 ? estresHumo : 0)));
            p.ticksFuego = n.fuegoActivo ? p.ticksFuego + 1 : 0;
            p.ticksHumo = n.humo >= 95 ? p.ticksHumo + 1 : 0;
            if (p.ticksFuego >= 3 || p.ticksHumo >= 15) {
                m.marcarFallecimiento(p, p.ticksFuego >= 3 ? "fuego" : "humo");
            } else if ((p.ticksFuego > 0 || p.ticksHumo >= 8) && p.salud != EstadoSalud.HERIDO && p.salud != EstadoSalud.INCAPACITADO) {
                p.salud = EstadoSalud.HERIDO;
                p.hist.add("t=" + m.tick + " HERIDO por exposición");
            }
        }
        m.emergencia().alertarDepartamentos(m.mapa.personas, departamentos, m.tick);
    }

}
