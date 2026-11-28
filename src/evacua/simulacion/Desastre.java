package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Estado de una emergencia, desde el tick cero hasta el fin de una ejecución. */
public class Desastre {
    private final TipoDesastre tipo;
    private final Intensidad intensidad;
    private final int origen;
    final Incendio incendio;
    public final FugaGas fuga;
    private int hazmatPendientes;
    private final java.util.ArrayDeque<int[]> reemplazos = new java.util.ArrayDeque<>();
    private int llegada = -1;
    private boolean confusionAsignada;
    private int pendientes, siguienteAcceso;

    Desastre(MotorSimulacion m) {
        ParametrosSimulacion p = m.params;
        tipo = p.desastre;
        intensidad = p.nivel;
        if (tipo == null || intensidad == null) throw new IllegalArgumentException("Seleccione desastre e intensidad.");
        if (!tipo.requiereOrigen()) origen = -1;
        else if (p.origenManual) {
            if (p.pisoOrigen < 0 || p.pisoOrigen >= m.mapa.pisos || p.filaOrigen < 0
                    || p.filaOrigen >= m.mapa.filasPorPiso || p.colOrigen < 0 || p.colOrigen >= m.mapa.cols)
                throw new IllegalArgumentException("Seleccione un origen válido en el plano.");
            int f = p.pisoOrigen * m.mapa.filasPorPiso + p.filaOrigen;
            if (!origenValido(m.mapa.n[f][p.colOrigen])) throw new IllegalArgumentException("El nodo seleccionado no admite este desastre.");
            origen = f * m.mapa.cols + p.colOrigen;
        } else {
            List<Integer> nodos = new ArrayList<>();
            for (int f = 0; f < m.mapa.filas; f++) for (int c = 0; c < m.mapa.cols; c++)
                if (origenValido(m.mapa.n[f][c])) nodos.add(f * m.mapa.cols + c);
            if (nodos.isEmpty()) throw new IllegalArgumentException("El plano no tiene un origen válido.");
            origen = nodos.get(m.rnd.nextInt(nodos.size()));
        }
        if (tipo == TipoDesastre.FUGA_DE_GAS && p.gas == null) throw new IllegalArgumentException("Seleccione el tipo de gas.");
        fuga = tipo == TipoDesastre.FUGA_DE_GAS ? new FugaGas(m, origen) : null;
        incendio = tipo == TipoDesastre.INCENDIO ? new Incendio(m, origen, intensidad) : null;
        if (tipo == TipoDesastre.TERREMOTO) {
            llegada = intensidad.demora(tipo);
            pendientes = intensidad.refuerzos;
        }
    }

    private boolean origenValido(Nodo n) {
        if (tipo == TipoDesastre.FUGA_DE_GAS) return n.tipo != TipoNodo.VACIO && !n.bloqueado;
        return tipo == TipoDesastre.INCENDIO ? n.combustible() : n.tipo != TipoNodo.VACIO;
    }

    void solicitarRefuerzos(MotorSimulacion m) {
        if (llegada >= 0) return;
        llegada = m.tick + intensidad.demora(tipo);
        pendientes = intensidad.refuerzos;
        if (fuga != null) {
            hazmatPendientes = fuga.tipo == TipoGas.GAS_NATURAL ? 1 : 2 + 2 * intensidad.ordinal();
            pendientes = hazmatPendientes + (fuga.tipo == TipoGas.GAS_NATURAL ? intensidad.refuerzos : 0);
            for (Persona p : m.mapa.personas) if (p instanceof PersonalRescate && p.activa()) p.alertada = true;
        }
        m.log.accept("t=" + m.tick + " primera detección; refuerzos solicitados para t=" + llegada);
    }

    void reemplazar(PersonalRescate b, int tick) {
        if (b.retirado) reemplazos.add(new int[]{tick + 4, Math.min(500, (int)b.limiteEstres() + 50)});
    }

    boolean reemplazoEnCamino(int tick) { return !reemplazos.isEmpty() && reemplazos.peek()[0] > tick; }

    boolean haySolicitudes() { return pendientes > 0 || !reemplazos.isEmpty(); }

    void despachar(MotorSimulacion m) {
        boolean tareas = fuga != null ? fuga.requierePersonal() : incendio.focos > 0 || m.mapa.personas.stream().anyMatch(p -> p.activa()
                && !(p instanceof Bombero) && (p.salud == EstadoSalud.HERIDO || p.salud == EstadoSalud.INCAPACITADO
                || m.mapa.n[p.f][p.c].humo >= 95));
        if (!tareas) {
            pendientes = hazmatPendientes = 0;
            reemplazos.clear();
        } else if (llegada >= 0 && m.tick >= llegada && haySolicitudes()) ingresarRefuerzos(m);
    }

    public int origen() { return origen; }
    public int tickLlegada() { return llegada; }
    public int restantes(int tick) { return llegada < 0 ? -1 : Math.max(0, llegada - tick); }

    void estres(MotorSimulacion m, Persona p, double incremento) {
        if (tipo == TipoDesastre.NINGUNO || !p.activa() || !Double.isFinite(incremento) || incremento <= 0) return;
        p.aumentarEstres(incremento);
    }

    void iniciarConfusion(MotorSimulacion m) {
        if (tipo != TipoDesastre.TERREMOTO || m.tick != 0 || confusionAsignada) return;
        confusionAsignada = true;
        for (Persona p : m.mapa.personas)
            if (p.activa() && !(p instanceof PersonalRescate)) p.confusion = 1 + m.rnd.nextInt(4);
    }

    void paso(MotorSimulacion m) {
        if (fuga != null) {
            fuga.ambiente();
            fuga.personas();
            return;
        }
        if (incendio != null) {
            incendio.ambiente();
            incendio.personas();
            return;
        }
        if (tipo == TipoDesastre.NINGUNO && m.rnd.nextDouble() < m.params.prob) evento(m);
        if (tipo != TipoDesastre.TERREMOTO) return;
        if (m.tick <= intensidad.duracion) {
            sacudir(m);
            if (m.tick == intensidad.duracion) m.log.accept("t=" + m.tick + " finaliza el terremoto.");
        }
        if (m.tick >= llegada && pendientes > 0) ingresarRefuerzos(m);
    }

    public int ticksTemblorRestantes(int tick) {
        return tipo == TipoDesastre.TERREMOTO ? Math.max(0, intensidad.duracion - tick) : 0;
    }

    public int refuerzosPendientes() { return pendientes; }

    private void sacudir(MotorSimulacion m) {
        boolean huboDerrumbe = false;
        boolean[][] derrumbes = new boolean[m.mapa.filas][m.mapa.cols];
        for (int f = 0; f < m.mapa.filas; f++) for (int c = 0; c < m.mapa.cols; c++) {
            Nodo n = m.mapa.n[f][c];
            if (n.tipo == TipoNodo.VACIO || n.bloqueado) continue;
            double sorteo = m.rnd.nextDouble() * 100;
            if (sorteo < intensidad.derrumbe) {
                m.mapa.bloquear(f, c, true);
                derrumbes[f][c] = true;
                huboDerrumbe = true;
                m.log.accept("t=" + m.tick + " derrumbe en " + m.mapa.idDe(f, c));
            } else if (sorteo < intensidad.derrumbe + intensidad.dano) n.danado = true;
            else if (sorteo < intensidad.derrumbe + intensidad.dano + intensidad.peligro) n.peligroso = true;
        }
        for (Persona p : m.mapa.personas) {
            if (!p.activa()) continue;
            if (derrumbes[p.f][p.c]) m.marcarFallecimiento(p, "derrumbe");
            else estres(m, p, intensidad.estres);
        }
        if (huboDerrumbe) m.actualizarTrasDerrumbe();
    }

    private void ingresarRefuerzos(MotorSimulacion m) {
        List<int[]> accesos = m.mapa.salidas(true);
        int sinPlaza = 0;
        while ((pendientes > 0 || !reemplazos.isEmpty() && reemplazos.peek()[0] <= m.tick) && sinPlaza < accesos.size()) {
            int[] q = accesos.get(siguienteAcceso % accesos.size());
            siguienteAcceso = (siguienteAcceso + 1) % accesos.size();
            if (!m.admiteRefuerzo(q[0], q[1])) {
                sinPlaza++;
                continue;
            }
            int limite = pendientes > 0 ? 100 : reemplazos.remove()[1];
            boolean hazmat = fuga != null && (hazmatPendientes > 0 || pendientes == 0);
            PersonalRescate r = m.incorporarRefuerzo(q[0], q[1], id -> {
                if (hazmat) return new Hazmat(id, q[0], q[1], limite);
                if (incendio != null) return new Bombero(id, q[0], q[1], limite);
                return new PersonalRescate(id, EstadoSalud.SANO, q[0], q[1]);
            });
            if (hazmatPendientes > 0) hazmatPendientes--;
            m.log.accept("t=" + m.tick + " llega refuerzo P" + r.id + " por " + m.mapa.idDe(q[0], q[1]));
            if (pendientes > 0) pendientes--;
            sinPlaza = 0;
        }
    }

    void evento(MotorSimulacion m) {
        if (tipo != TipoDesastre.NINGUNO) return;
        int[] celda = EventoAleatorio.colapsarPasillo(m.mapa, m.rnd);
        if (celda != null) m.alBloquear(celda[0], celda[1]);
    }

    static void alertarDepartamentos(List<Persona> personas, Set<Integer> departamentos, int tick) {
        for (Persona p : personas) if (p.activa() && !p.alertada && departamentos.contains(p.departamentoInicial)) {
            p.alertada = true;
            p.hist.add("t=" + tick + " recibe alerta de su departamento");
        }
    }
}
