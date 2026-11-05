import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * Evacua - Prototipo de interfaz (Java Swing).
 *
 * Cada nodo del edificio se dibuja como un cuadro de la cuadricula.
 * Los casos de uso que se pueden representar visualmente son botones del panel izquierdo.
 *
 * Archivo unico, sin dependencias. Para ejecutar en NetBeans: crear un proyecto Java,
 * pegar este archivo en el paquete por defecto y ejecutar (Shift+F6).
 *
 * Formato de escenario/plano (.txt):
 *   '.' = sin nodo   H = habitacion   P = pasillo   E = escalera   S = salida
 *   letra en minuscula = nodo bloqueado
 *   (opcional) linea "#PERSONAS" y luego lineas "fila columna TIPO SALUD"
 */
public class EvacuaPrototipo extends JFrame {

    // =====================================================================
    //  MODELO
    // =====================================================================

    enum Tipo {
        VACIO('.', "Vacio", Color.WHITE),
        HABITACION('H', "Habitacion", new Color(173, 216, 230)),
        PASILLO('P', "Pasillo", new Color(240, 235, 190)),
        ESCALERA('E', "Escalera", new Color(221, 190, 235)),
        SALIDA('S', "Salida", new Color(144, 238, 144));

        final char letra;
        final String nombre;
        final Color color;

        Tipo(char letra, String nombre, Color color) {
            this.letra = letra;
            this.nombre = nombre;
            this.color = color;
        }

        static Tipo de(char ch) {
            char u = Character.toUpperCase(ch);
            for (Tipo t : values()) {
                if (t.letra == u) return t;
            }
            return null;
        }
    }

    enum TipoPersona {
        ADULTO("Adulto", 'A', 1.0, new Color(30, 100, 200)),
        NINO("Nino", 'N', 0.8, new Color(235, 140, 20)),
        MOVILIDAD("Mov. reducida", 'M', 0.5, new Color(140, 60, 160)),
        RESCATE("Rescate", 'R', 1.2, new Color(200, 30, 30));

        final String nombre;
        final char letra;
        final double vel;
        final Color color;

        TipoPersona(String nombre, char letra, double vel, Color color) {
            this.nombre = nombre;
            this.letra = letra;
            this.vel = vel;
            this.color = color;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    enum EstadoSalud {
        SANO(1.0), HERIDO(0.6), ENFERMO(0.8), INCAPACITADO(0.0);

        final double vel;

        EstadoSalud(double vel) {
            this.vel = vel;
        }
    }

    static class Nodo {
        Tipo tipo = Tipo.VACIO;
        boolean bloqueado;
        int ocupacion;   // personas en este instante
        int visitas;     // acumulado para el mapa de calor
        int esperas;     // acumulado de esperas (cuello de botella)
    }

    static class Persona {
        final int id;
        final TipoPersona tipo;
        final EstadoSalud salud;
        int f, c;
        double estres, acum;
        boolean evacuada, asistida;
        int tEvac = -1, pasos, recalculos;
        List<int[]> ruta = new ArrayList<>();
        Persona objetivo;
        final List<String> hist = new ArrayList<>();

        Persona(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
            this.id = id;
            this.tipo = tipo;
            this.salud = salud;
            this.f = f;
            this.c = c;
        }

        String etiqueta() {
            return "P" + id + " (" + tipo.nombre + ", " + salud + ")";
        }
    }

    static class Params {
        double prob = 0.03;   // probabilidad de evento aleatorio por tick
        long semilla = 42;
        int limite = 300;     // tiempo limite (ticks)
        double umbral = 8;    // umbral de estres
        int capacidad = 2;    // personas por nodo
        boolean astar = true;

        Params copia() {
            Params p = new Params();
            p.prob = prob;
            p.semilla = semilla;
            p.limite = limite;
            p.umbral = umbral;
            p.capacidad = capacidad;
            p.astar = astar;
            return p;
        }
    }

    static class Mapa {
        final int filas, cols;
        final Nodo[][] n;
        final List<Persona> personas = new ArrayList<>();
        int sigId = 1;

        Mapa(int filas, int cols) {
            this.filas = filas;
            this.cols = cols;
            n = new Nodo[filas][cols];
            for (int i = 0; i < filas; i++)
                for (int j = 0; j < cols; j++) n[i][j] = new Nodo();
        }

        boolean dentro(int f, int c) {
            return f >= 0 && c >= 0 && f < filas && c < cols;
        }

        boolean caminable(int f, int c) {
            return dentro(f, c) && n[f][c].tipo != Tipo.VACIO && !n[f][c].bloqueado;
        }

        void rect(Tipo t, int f0, int c0, int f1, int c1) {
            for (int i = f0; i <= f1; i++)
                for (int j = c0; j <= c1; j++) n[i][j].tipo = t;
        }

        List<int[]> salidas() {
            List<int[]> r = new ArrayList<>();
            for (int i = 0; i < filas; i++)
                for (int j = 0; j < cols; j++)
                    if (n[i][j].tipo == Tipo.SALIDA && !n[i][j].bloqueado) r.add(new int[]{i, j});
            return r;
        }

        Persona agregar(TipoPersona t, EstadoSalud s, int f, int c) {
            Persona p = new Persona(sigId++, t, s, f, c);
            personas.add(p);
            return p;
        }

        String aTexto(boolean conPersonas) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < filas; i++) {
                for (int j = 0; j < cols; j++) {
                    Nodo nd = n[i][j];
                    char ch = nd.tipo.letra;
                    if (nd.tipo != Tipo.VACIO && nd.bloqueado) ch = Character.toLowerCase(ch);
                    sb.append(ch);
                }
                sb.append('\n');
            }
            if (conPersonas && !personas.isEmpty()) {
                sb.append("#PERSONAS\n");
                for (Persona p : personas)
                    sb.append(p.f).append(' ').append(p.c).append(' ')
                            .append(p.tipo.name()).append(' ').append(p.salud.name()).append('\n');
            }
            return sb.toString();
        }

        /** Valida y construye. Lanza IllegalArgumentException con el motivo del error. */
        static Mapa deTexto(String txt) {
            String[] lineas = txt.split("\\R");
            List<String> grid = new ArrayList<>();
            int i = 0;
            while (i < lineas.length && lineas[i].trim().isEmpty()) i++;
            while (i < lineas.length && !lineas[i].trim().isEmpty() && !lineas[i].startsWith("#")) {
                grid.add(lineas[i].trim());
                i++;
            }
            if (grid.isEmpty()) throw new IllegalArgumentException("El archivo no contiene un plano.");
            int cols = grid.get(0).length();
            Mapa m = new Mapa(grid.size(), cols);
            boolean haySalida = false;
            for (int f = 0; f < grid.size(); f++) {
                String ln = grid.get(f);
                if (ln.length() != cols)
                    throw new IllegalArgumentException("La fila " + (f + 1) + " tiene " + ln.length()
                            + " columnas y se esperaban " + cols + ".");
                for (int c = 0; c < cols; c++) {
                    char ch = ln.charAt(c);
                    Tipo t = ch == '.' ? Tipo.VACIO : Tipo.de(ch);
                    if (t == null)
                        throw new IllegalArgumentException("Caracter invalido '" + ch + "' en fila "
                                + (f + 1) + ", columna " + (c + 1) + ".");
                    m.n[f][c].tipo = t;
                    m.n[f][c].bloqueado = t != Tipo.VACIO && Character.isLowerCase(ch);
                    if (t == Tipo.SALIDA) haySalida = true;
                }
            }
            if (!haySalida) throw new IllegalArgumentException("El plano debe tener al menos una salida (S).");
            for (; i < lineas.length; i++) {
                String ln = lineas[i].trim();
                if (ln.isEmpty() || ln.startsWith("#")) continue;
                String[] tk = ln.split("\\s+");
                int f, c;
                TipoPersona tp;
                EstadoSalud es;
                try {
                    f = Integer.parseInt(tk[0]);
                    c = Integer.parseInt(tk[1]);
                    tp = TipoPersona.valueOf(tk[2]);
                    es = EstadoSalud.valueOf(tk[3]);
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Linea de persona invalida: \"" + ln + "\".");
                }
                if (!m.caminable(f, c))
                    throw new IllegalArgumentException("La persona en (" + f + "," + c + ") no esta sobre un nodo libre.");
                m.agregar(tp, es, f, c);
            }
            return m;
        }
    }

    // =====================================================================
    //  RUTAS: Dijkstra y A*
    // =====================================================================

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    static class Resultado {
        List<int[]> ruta;   // null si no hay camino
        int explorados;
        long nanos;
        double costo;
    }

    static double heuristica(int f, int c, List<int[]> metas) {
        double best = Double.MAX_VALUE;
        for (int[] g : metas) best = Math.min(best, Math.abs(g[0] - f) + Math.abs(g[1] - c));
        return best == Double.MAX_VALUE ? 0 : best;
    }

    static Resultado buscar(Mapa m, int sf, int sc, List<int[]> metas, boolean astar) {
        long t0 = System.nanoTime();
        int C = m.cols;
        int total = m.filas * m.cols;
        double[] dist = new double[total];
        Arrays.fill(dist, Double.MAX_VALUE);
        int[] prev = new int[total];
        Arrays.fill(prev, -1);
        boolean[] cerrado = new boolean[total];
        boolean[] esMeta = new boolean[total];
        List<int[]> validas = new ArrayList<>();
        for (int[] g : metas)
            if (m.caminable(g[0], g[1])) {
                esMeta[g[0] * C + g[1]] = true;
                validas.add(g);
            }
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        int ini = sf * C + sc;
        dist[ini] = 0;
        pq.add(new double[]{astar ? heuristica(sf, sc, validas) : 0, ini});
        int explorados = 0, fin = -1;
        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int id = (int) cur[1];
            if (cerrado[id]) continue;
            cerrado[id] = true;
            explorados++;
            if (esMeta[id]) {
                fin = id;
                break;
            }
            int f = id / C, c = id % C;
            for (int[] d : DIRS) {
                int nf = f + d[0], nc = c + d[1];
                if (!m.caminable(nf, nc)) continue;
                int nid = nf * C + nc;
                double costo = m.n[nf][nc].tipo == Tipo.ESCALERA ? 3 : 1;
                double nd = dist[id] + costo;
                if (nd < dist[nid]) {
                    dist[nid] = nd;
                    prev[nid] = id;
                    pq.add(new double[]{nd + (astar ? heuristica(nf, nc, validas) : 0), nid});
                }
            }
        }
        Resultado r = new Resultado();
        r.explorados = explorados;
        if (fin >= 0) {
            LinkedList<int[]> path = new LinkedList<>();
            for (int x = fin; x != ini; x = prev[x]) path.addFirst(new int[]{x / C, x % C});
            r.ruta = path;
            r.costo = dist[fin];
        }
        r.nanos = System.nanoTime() - t0;
        return r;
    }

    // =====================================================================
    //  MOTOR DE SIMULACION
    // =====================================================================

    static class Motor {
        final Mapa mapa;
        final Params params;
        final Random rnd;
        final java.util.function.Consumer<String> log;
        int tick;
        boolean terminado;
        int exploradosTotal;

        Motor(Mapa mapa, Params params, java.util.function.Consumer<String> log) {
            this.mapa = mapa;
            this.params = params;
            this.rnd = new Random(params.semilla);
            this.log = log;
        }

        void iniciar() {
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

        Persona masCercanoIncapacitado(Persona r) {
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

        void asignarRuta(Persona p) {
            if (p.evacuada) return;
            p.objetivo = p.tipo == TipoPersona.RESCATE ? masCercanoIncapacitado(p) : null;
            if (p.salud == EstadoSalud.INCAPACITADO && !p.asistida) {
                p.ruta = new ArrayList<>();
                return;
            }
            Resultado r;
            if (p.objetivo != null) {
                r = buscar(mapa, p.f, p.c, Collections.singletonList(new int[]{p.objetivo.f, p.objetivo.c}), params.astar);
                if (r.ruta == null) p.objetivo = null;
                else exploradosTotal += r.explorados;
            } else {
                r = null;
            }
            if (p.objetivo == null) {
                r = buscar(mapa, p.f, p.c, mapa.salidas(), params.astar);
                exploradosTotal += r.explorados;
            }
            p.ruta = r.ruta == null ? new ArrayList<>() : new ArrayList<>(r.ruta);
        }

        void recalcularTodas() {
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

        void generarEvento() {
            List<int[]> cand = new ArrayList<>();
            for (int i = 0; i < mapa.filas; i++)
                for (int j = 0; j < mapa.cols; j++)
                    if (mapa.n[i][j].tipo == Tipo.PASILLO && !mapa.n[i][j].bloqueado) cand.add(new int[]{i, j});
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

        boolean paso() {
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
                    if (ns.tipo != Tipo.SALIDA && ns.ocupacion >= params.capacidad) {
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
                    if (ns.tipo == Tipo.SALIDA && p.objetivo == null) {
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

        int evacuadas() {
            int k = 0;
            for (Persona p : mapa.personas) if (p.evacuada) k++;
            return k;
        }

        double promedioEvac() {
            double s = 0;
            int k = 0;
            for (Persona p : mapa.personas)
                if (p.evacuada) {
                    s += p.tEvac;
                    k++;
                }
            return k == 0 ? 0 : s / k;
        }

        double promedioEstres() {
            if (mapa.personas.isEmpty()) return 0;
            double s = 0;
            for (Persona p : mapa.personas) s += p.estres;
            return s / mapa.personas.size();
        }

        String reporte() {
            StringBuilder sb = new StringBuilder();
            sb.append("REPORTE DE SIMULACION\n=====================\n");
            sb.append("Algoritmo: ").append(params.astar ? "A*" : "Dijkstra").append("   Semilla: ").append(params.semilla).append('\n');
            sb.append("Ticks simulados: ").append(tick).append('\n');
            sb.append("Personas: ").append(mapa.personas.size()).append("   Evacuadas: ").append(evacuadas())
                    .append("   No evacuadas: ").append(mapa.personas.size() - evacuadas()).append('\n');
            sb.append(String.format("Tiempo promedio de evacuacion: %.1f ticks%n", promedioEvac()));
            sb.append(String.format("Estres promedio final: %.2f%n", promedioEstres()));
            sb.append("Nodos explorados por los algoritmos: ").append(exploradosTotal).append('\n');
            List<int[]> cuellos = new ArrayList<>();
            for (int i = 0; i < mapa.filas; i++)
                for (int j = 0; j < mapa.cols; j++)
                    if (mapa.n[i][j].esperas > 0) cuellos.add(new int[]{i, j, mapa.n[i][j].esperas});
            cuellos.sort((a, b) -> Integer.compare(b[2], a[2]));
            sb.append("\nCuellos de botella (nodos con mas esperas):\n");
            if (cuellos.isEmpty()) sb.append("  ninguno\n");
            for (int k = 0; k < Math.min(3, cuellos.size()); k++)
                sb.append("  (").append(cuellos.get(k)[0]).append(',').append(cuellos.get(k)[1])
                        .append(") -> ").append(cuellos.get(k)[2]).append(" esperas\n");
            sb.append("\nPor persona:\n");
            for (Persona p : mapa.personas)
                sb.append(String.format("  %-34s %s  pasos=%d  recalculos=%d  estres=%.1f%n", p.etiqueta(),
                        p.evacuada ? "EVACUADA t=" + p.tEvac : "NO EVACUADA", p.pasos, p.recalculos, p.estres));
            return sb.toString();
        }

        String reporteIndividual(Persona p) {
            StringBuilder sb = new StringBuilder();
            sb.append("REPORTE INDIVIDUAL - ").append(p.etiqueta()).append('\n');
            sb.append("Estado final: ").append(p.evacuada ? "evacuada en t=" + p.tEvac : "no evacuada").append('\n');
            sb.append("Pasos: ").append(p.pasos).append("  Recalculos: ").append(p.recalculos)
                    .append(String.format("  Estres final: %.1f%n", p.estres));
            sb.append("\nLinea de tiempo:\n");
            for (String h : p.hist) sb.append("  ").append(h).append('\n');
            return sb.toString();
        }
    }

    // =====================================================================
    //  PANEL DE LA CUADRICULA (cada nodo = un cuadro)
    // =====================================================================

    enum Herramienta { NINGUNA, HABITACION, PASILLO, ESCALERA, SALIDA, BORRAR, PERSONA, BLOQUEO }

    static final int CELL = 38;

    class GridPanel extends JPanel {
        private Boolean valorArrastre;

        GridPanel() {
            setBackground(Color.WHITE);
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    valorArrastre = null;
                    aplicar(e, true);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    aplicar(e, false);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void ajustarTamano() {
            Mapa m = vista();
            setPreferredSize(new Dimension(m.cols * CELL + 1, m.filas * CELL + 1));
            revalidate();
            repaint();
        }

        private void aplicar(MouseEvent e, boolean click) {
            Mapa m = vista();
            int c = e.getX() / CELL, f = e.getY() / CELL;
            if (!m.dentro(f, c)) return;
            Nodo nd = m.n[f][c];
            if (herramienta == Herramienta.BLOQUEO) {
                if (nd.tipo == Tipo.VACIO) return;
                if (valorArrastre == null) valorArrastre = !nd.bloqueado;
                if (nd.bloqueado != valorArrastre) {
                    nd.bloqueado = valorArrastre;
                    log((valorArrastre ? "Zona bloqueada" : "Zona desbloqueada") + " en (" + f + "," + c + ").");
                    if (motor != null) motor.recalcularTodas();
                }
            } else if (motor != null) {
                if (click) log("Para editar el edificio vuelva a edicion (boton 'Volver a edicion').");
            } else {
                switch (herramienta) {
                    case HABITACION: nd.tipo = Tipo.HABITACION; break;
                    case PASILLO: nd.tipo = Tipo.PASILLO; break;
                    case ESCALERA: nd.tipo = Tipo.ESCALERA; break;
                    case SALIDA: nd.tipo = Tipo.SALIDA; break;
                    case BORRAR:
                        nd.tipo = Tipo.VACIO;
                        nd.bloqueado = false;
                        m.personas.removeIf(p -> p.f == f && p.c == c);
                        break;
                    case PERSONA:
                        if (!click) break;
                        if (!m.caminable(f, c)) {
                            log("Solo se puede colocar personas sobre un nodo libre.");
                            break;
                        }
                        boolean habia = m.personas.removeIf(p -> p.f == f && p.c == c);
                        if (!habia) {
                            Persona p = m.agregar((TipoPersona) cmbTipo.getSelectedItem(), (EstadoSalud) cmbSalud.getSelectedItem(), f, c);
                            log("Registrada " + p.etiqueta() + " en (" + f + "," + c + ").");
                        }
                        break;
                    default:
                        break;
                }
            }
            repaint();
            actualizarEstado();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Mapa m = vista();
            int maxV = 1;
            for (Nodo[] fila : m.n) for (Nodo nd : fila) maxV = Math.max(maxV, nd.visitas);

            // cuadricula de fondo
            g.setColor(new Color(205, 205, 205));
            for (int i = 0; i <= m.filas; i++) g.drawLine(0, i * CELL, m.cols * CELL, i * CELL);
            for (int j = 0; j <= m.cols; j++) g.drawLine(j * CELL, 0, j * CELL, m.filas * CELL);

            // nodos como cuadros
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            for (int i = 0; i < m.filas; i++) {
                for (int j = 0; j < m.cols; j++) {
                    Nodo nd = m.n[i][j];
                    if (nd.tipo == Tipo.VACIO) continue;
                    int x = j * CELL, y = i * CELL;
                    g.setColor(nd.tipo.color);
                    g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);
                    if (verCalor && nd.visitas > 0) {
                        float a = Math.min(0.85f, 0.12f + 0.73f * nd.visitas / maxV);
                        g.setColor(new Color(1f, 0.1f, 0.0f, a));
                        g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);
                    }
                    g.setColor(new Color(90, 90, 90));
                    g.drawRect(x + 2, y + 2, CELL - 3, CELL - 3);
                    g.setColor(new Color(60, 60, 60, 160));
                    g.drawString(String.valueOf(nd.tipo.letra), x + CELL / 2 - 5, y + CELL / 2 + 5);
                    if (nd.bloqueado) {
                        g.setColor(new Color(40, 40, 40, 190));
                        g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);
                        g.setColor(Color.RED);
                        g.setStroke(new BasicStroke(3));
                        g.drawLine(x + 8, y + 8, x + CELL - 8, y + CELL - 8);
                        g.drawLine(x + CELL - 8, y + 8, x + 8, y + CELL - 8);
                        g.setStroke(new BasicStroke(1));
                    }
                }
            }

            // personas
            Map<Integer, Integer> contador = new HashMap<>();
            double[][] off = {{0.3, 0.3}, {0.7, 0.3}, {0.3, 0.7}, {0.7, 0.7}};
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            for (Persona p : m.personas) {
                if (p.evacuada) continue;
                int key = p.f * m.cols + p.c;
                int k = contador.merge(key, 1, Integer::sum) - 1;
                double[] o = off[Math.min(k, 3)];
                int cx = (int) (p.c * CELL + o[0] * CELL), cy = (int) (p.f * CELL + o[1] * CELL);
                int d = 15;
                g.setColor(p.tipo.color);
                g.fillOval(cx - d / 2, cy - d / 2, d, d);
                boolean estresado = motor != null && p.estres >= motor.params.umbral;
                g.setColor(estresado ? Color.RED : Color.WHITE);
                g.setStroke(new BasicStroke(estresado ? 2.5f : 1.2f));
                g.drawOval(cx - d / 2, cy - d / 2, d, d);
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.WHITE);
                g.drawString(String.valueOf(p.tipo.letra), cx - 3, cy + 4);
                if (p.salud != EstadoSalud.SANO) {
                    g.setColor(p.salud == EstadoSalud.INCAPACITADO ? Color.BLACK : new Color(180, 0, 0));
                    g.drawString("+", cx + 5, cy - 4);
                }
            }
        }
    }

    // =====================================================================
    //  VENTANA PRINCIPAL
    // =====================================================================

    private Mapa mapa;
    private Motor motor;
    private Params params = new Params();
    private Herramienta herramienta = Herramienta.NINGUNA;
    private boolean verCalor;
    private GridPanel grid;
    private JTextArea logArea;
    private JLabel estado;
    private javax.swing.Timer timer;
    private JComboBox<TipoPersona> cmbTipo;
    private JComboBox<EstadoSalud> cmbSalud;
    private JCheckBox chkTiempoReal;
    private JSlider velocidad;
    private File ultimoDir = new File(".");

    private Mapa vista() {
        return motor != null ? motor.mapa : mapa;
    }

    public EvacuaPrototipo() {
        super("Evacua - Prototipo de interfaz");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        mapa = crearDemo();
        grid = new GridPanel();
        timer = new javax.swing.Timer(250, e -> avanzar());

        setLayout(new BorderLayout(6, 6));
        add(crearPanelIzquierdo(), BorderLayout.WEST);
        add(new JScrollPane(grid), BorderLayout.CENTER);

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel sur = new JPanel(new BorderLayout());
        estado = new JLabel(" ");
        estado.setBorder(new EmptyBorder(2, 6, 2, 6));
        sur.add(estado, BorderLayout.NORTH);
        sur.add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        grid.ajustarTamano();
        actualizarEstado();
        log("Prototipo listo. Elija una herramienta o pulse 'Iniciar emergencia y simular evacuacion'.");
        setSize(1280, 840);
        setLocationRelativeTo(null);
    }

    private Mapa crearDemo() {
        Mapa m = new Mapa(12, 20);
        m.rect(Tipo.PASILLO, 5, 1, 6, 18);
        m.rect(Tipo.HABITACION, 1, 1, 3, 5);
        m.rect(Tipo.HABITACION, 1, 7, 3, 11);
        m.rect(Tipo.HABITACION, 1, 13, 3, 17);
        m.rect(Tipo.HABITACION, 8, 1, 10, 5);
        m.rect(Tipo.HABITACION, 8, 7, 10, 11);
        m.rect(Tipo.HABITACION, 8, 13, 10, 17);
        for (int c : new int[]{3, 9, 15}) {
            m.n[4][c].tipo = Tipo.PASILLO;
            m.n[7][c].tipo = Tipo.PASILLO;
        }
        m.n[5][10].tipo = Tipo.ESCALERA;
        m.n[5][0].tipo = Tipo.SALIDA;
        m.n[6][19].tipo = Tipo.SALIDA;
        m.agregar(TipoPersona.ADULTO, EstadoSalud.SANO, 2, 3);
        m.agregar(TipoPersona.NINO, EstadoSalud.SANO, 2, 9);
        m.agregar(TipoPersona.MOVILIDAD, EstadoSalud.SANO, 9, 3);
        m.agregar(TipoPersona.ADULTO, EstadoSalud.HERIDO, 9, 15);
        m.agregar(TipoPersona.ADULTO, EstadoSalud.INCAPACITADO, 9, 9);
        m.agregar(TipoPersona.RESCATE, EstadoSalud.SANO, 2, 15);
        return m;
    }

    // ---------- panel de casos de uso (botones) ----------

    private JComponent crearPanelIzquierdo() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(6, 6, 6, 0));
        ButtonGroup herramientas = new ButtonGroup();

        // Escenarios y persistencia
        p.add(grupo("Escenarios y persistencia",
                boton("Cargar escenario", "Lee un escenario .txt (plano + personas)", e -> cargarEscenario()),
                boton("Guardar escenario", "Guarda plano, bloqueos y personas en un .txt", e -> guardarEscenario()),
                boton("Importar plano del edificio", "Lee y valida un plano estructurado", e -> importarPlano()),
                boton("Comparar escenarios", "Simula dos escenarios y compara resultados", e -> compararEscenarios())));

        // Gestionar edificio (nodos y conexiones)
        p.add(grupo("Gestionar edificio (nodos y conexiones)",
                tool("Sin herramienta", Herramienta.NINGUNA, herramientas, true, "Solo observar"),
                tool("Habitacion", Herramienta.HABITACION, herramientas, false, "Pinta nodos de tipo habitacion"),
                tool("Pasillo", Herramienta.PASILLO, herramientas, false, "Pinta nodos de tipo pasillo"),
                tool("Escalera", Herramienta.ESCALERA, herramientas, false, "Pinta nodos de tipo escalera (costo 3)"),
                tool("Salida", Herramienta.SALIDA, herramientas, false, "Pinta nodos de tipo salida"),
                tool("Borrar nodo", Herramienta.BORRAR, herramientas, false, "Elimina el nodo y las personas sobre el")));

        // Registrar personas
        cmbTipo = new JComboBox<>(TipoPersona.values());
        cmbSalud = new JComboBox<>(EstadoSalud.values());
        p.add(grupo("Registrar personas",
                cmbTipo, cmbSalud,
                tool("Colocar / quitar persona", Herramienta.PERSONA, herramientas, false,
                        "Clic en un nodo libre para colocar; clic otra vez para quitar")));

        // Bloqueos
        p.add(grupo("Bloqueos",
                tool("Bloquear / desbloquear zonas", Herramienta.BLOQUEO, herramientas, false,
                        "Clic o arrastre sobre nodos. Funciona tambien durante la simulacion")));

        // Simulacion
        chkTiempoReal = new JCheckBox("Visualizar simulacion en tiempo real", true);
        velocidad = new JSlider(1, 10, 5);
        velocidad.setToolTipText("Velocidad de la animacion");
        velocidad.addChangeListener(e -> timer.setDelay(1100 - velocidad.getValue() * 100));
        timer.setDelay(1100 - velocidad.getValue() * 100);
        p.add(grupo("Simulacion",
                boton("Configurar parametros de simulacion", "Probabilidad, semilla, tiempo limite, estres, capacidad, algoritmo", e -> configurar()),
                boton("Iniciar emergencia y simular evacuacion", "Parte desde el escenario actual", e -> iniciar()),
                chkTiempoReal, velocidad,
                boton("Detener simulacion", "Pausa la animacion (conserva resultados)", e -> timer.stop()),
                boton("Generar evento aleatorio", "Colapsa un pasillo al azar", e -> eventoManual()),
                boton("Recalcular ruta", "Recalcula las rutas de todas las personas", e -> recalcularManual()),
                boton("Volver a edicion", "Descarta la simulacion y vuelve al escenario editable", e -> volverAEdicion()),
                boton("Comparar Dijkstra vs A*", "Compara nodos explorados y tiempo de calculo", e -> compararAlgoritmos())));

        // Analisis
        JToggleButton calor = new JToggleButton("Ver mapa de calor");
        calor.setToolTipText("Colorea los nodos segun su ocupacion acumulada");
        calor.addActionListener(e -> {
            verCalor = calor.isSelected();
            grid.repaint();
        });
        p.add(grupo("Analisis",
                calor,
                boton("Generar reporte de simulacion", "Estadisticas, tiempos y cuellos de botella", e -> reporte()),
                boton("Exportar reporte individual por persona", "Linea de tiempo de una persona a .txt", e -> exportarIndividual())));

        p.add(crearLeyenda());
        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(330, 700));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JComponent crearLeyenda() {
        JPanel l = new JPanel(new GridLayout(0, 3, 3, 3));
        l.setBorder(new TitledBorder("Leyenda"));
        for (Tipo t : Tipo.values()) {
            if (t == Tipo.VACIO) continue;
            JLabel x = new JLabel(t.letra + " " + t.nombre, SwingConstants.CENTER);
            x.setOpaque(true);
            x.setBackground(t.color);
            x.setFont(x.getFont().deriveFont(10f));
            x.setBorder(new LineBorder(Color.GRAY));
            l.add(x);
        }
        for (TipoPersona t : TipoPersona.values()) {
            JLabel x = new JLabel(t.letra + " " + t.nombre, SwingConstants.CENTER);
            x.setOpaque(true);
            x.setBackground(t.color);
            x.setForeground(Color.WHITE);
            x.setFont(x.getFont().deriveFont(10f));
            l.add(x);
        }
        return l;
    }

    private JPanel grupo(String titulo, Component... comps) {
        JPanel g = new JPanel(new GridLayout(0, 1, 3, 3));
        g.setBorder(new CompoundBorder(new TitledBorder(titulo), new EmptyBorder(2, 4, 4, 4)));
        for (Component c : comps) g.add(c);
        g.setMaximumSize(new Dimension(Integer.MAX_VALUE, g.getPreferredSize().height));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        return g;
    }

    private JButton boton(String texto, String tip, ActionListener a) {
        JButton b = new JButton(texto);
        b.setToolTipText(tip);
        b.addActionListener(a);
        return b;
    }

    private JToggleButton tool(String texto, Herramienta h, ButtonGroup g, boolean sel, String tip) {
        JToggleButton b = new JToggleButton(texto, sel);
        b.setToolTipText(tip);
        b.addActionListener(e -> herramienta = h);
        g.add(b);
        return b;
    }

    // ---------- acciones ----------

    private void log(String s) {
        if (logArea == null) return;
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void actualizarEstado() {
        Mapa m = vista();
        if (motor == null) {
            estado.setText("Modo edicion  |  Personas: " + m.personas.size() + "  |  Algoritmo: " + (params.astar ? "A*" : "Dijkstra"));
        } else {
            estado.setText("Simulando  |  t=" + motor.tick + "  |  Evacuadas: " + motor.evacuadas() + "/" + m.personas.size()
                    + "  |  Algoritmo: " + (params.astar ? "A*" : "Dijkstra") + (motor.terminado ? "  |  TERMINADA" : ""));
        }
    }

    private void iniciar() {
        if (mapa.personas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Registre al menos una persona antes de iniciar.", "Sin personas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        timer.stop();
        Mapa copia = Mapa.deTexto(mapa.aTexto(true));
        motor = new Motor(copia, params.copia(), this::log);
        motor.iniciar();
        grid.ajustarTamano();
        if (chkTiempoReal.isSelected()) {
            timer.start();
        } else {
            while (motor.paso()) { /* corre sin animar */ }
        }
        grid.repaint();
        actualizarEstado();
    }

    private void avanzar() {
        if (motor == null || !motor.paso()) timer.stop();
        grid.repaint();
        actualizarEstado();
    }

    private void volverAEdicion() {
        timer.stop();
        motor = null;
        grid.ajustarTamano();
        actualizarEstado();
        log("Vuelve al modo edicion.");
    }

    private boolean requiereMotor() {
        if (motor == null) {
            JOptionPane.showMessageDialog(this, "Primero inicie una emergencia.", "Sin simulacion", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    private void eventoManual() {
        if (!requiereMotor()) return;
        motor.generarEvento();
        grid.repaint();
    }

    private void recalcularManual() {
        if (!requiereMotor()) return;
        motor.recalcularTodas();
        grid.repaint();
    }

    private void configurar() {
        JTextField prob = new JTextField(String.valueOf(params.prob));
        JTextField sem = new JTextField(String.valueOf(params.semilla));
        JTextField lim = new JTextField(String.valueOf(params.limite));
        JTextField umb = new JTextField(String.valueOf(params.umbral));
        JTextField cap = new JTextField(String.valueOf(params.capacidad));
        JComboBox<String> alg = new JComboBox<>(new String[]{"A*", "Dijkstra"});
        alg.setSelectedIndex(params.astar ? 0 : 1);
        JPanel f = new JPanel(new GridLayout(0, 2, 6, 6));
        f.add(new JLabel("Probabilidad de evento por tick (0-1)"));
        f.add(prob);
        f.add(new JLabel("Semilla aleatoria"));
        f.add(sem);
        f.add(new JLabel("Tiempo limite (ticks)"));
        f.add(lim);
        f.add(new JLabel("Umbral de estres"));
        f.add(umb);
        f.add(new JLabel("Capacidad por nodo"));
        f.add(cap);
        f.add(new JLabel("Algoritmo de ruta"));
        f.add(alg);
        if (JOptionPane.showConfirmDialog(this, f, "Configurar parametros de simulacion",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double pr = Double.parseDouble(prob.getText().trim());
            long se = Long.parseLong(sem.getText().trim());
            int li = Integer.parseInt(lim.getText().trim());
            double um = Double.parseDouble(umb.getText().trim());
            int ca = Integer.parseInt(cap.getText().trim());
            if (pr < 0 || pr > 1 || li < 1 || um <= 0 || ca < 1) throw new NumberFormatException();
            params.prob = pr;
            params.semilla = se;
            params.limite = li;
            params.umbral = um;
            params.capacidad = ca;
            params.astar = alg.getSelectedIndex() == 0;
            log("Parametros actualizados.");
            actualizarEstado();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Algun valor no es valido. Los parametros no se modificaron.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File elegirArchivo(boolean abrir, String sugerido) {
        JFileChooser fc = new JFileChooser(ultimoDir);
        if (sugerido != null) fc.setSelectedFile(new File(ultimoDir, sugerido));
        int r = abrir ? fc.showOpenDialog(this) : fc.showSaveDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        ultimoDir = f.getParentFile() != null ? f.getParentFile() : ultimoDir;
        if (!abrir && !f.getName().contains(".")) f = new File(f.getPath() + ".txt");
        return f;
    }

    private Mapa leerMapa(File f) {
        try {
            return Mapa.deTexto(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    private void cargarEscenario() {
        File f = elegirArchivo(true, null);
        if (f == null) return;
        try {
            Mapa m = leerMapa(f);
            timer.stop();
            motor = null;
            mapa = m;
            grid.ajustarTamano();
            actualizarEstado();
            log("Escenario cargado: " + f.getName() + " (" + m.filas + "x" + m.cols + ", " + m.personas.size() + " personas).");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cargar el escenario", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarEscenario() {
        File f = elegirArchivo(false, "escenario.txt");
        if (f == null) return;
        try {
            if (mapa.salidas().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El escenario no es valido: necesita al menos una salida sin bloquear.", "Validar escenario", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Files.write(f.toPath(), mapa.aTexto(true).getBytes(StandardCharsets.UTF_8));
            log("Escenario guardado en " + f.getName() + ".");
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importarPlano() {
        File f = elegirArchivo(true, null);
        if (f == null) return;
        try {
            Mapa m = leerMapa(f);
            m.personas.clear();
            timer.stop();
            motor = null;
            mapa = m;
            grid.ajustarTamano();
            actualizarEstado();
            log("Plano importado y validado: " + f.getName() + " (" + m.filas + "x" + m.cols + "). Grafo construido.");
        } catch (IllegalArgumentException ex) {
            log("Plano rechazado: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Errores de validacion del plano", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compararEscenarios() {
        JOptionPane.showMessageDialog(this, "Seleccione el escenario A y luego el escenario B.");
        File a = elegirArchivo(true, null);
        if (a == null) return;
        File b = elegirArchivo(true, null);
        if (b == null) return;
        try {
            DefaultTableModel tm = new DefaultTableModel(
                    new Object[]{"Escenario", "Personas", "Evacuadas", "Ticks", "T. promedio", "Estres prom."}, 0);
            for (File f : new File[]{a, b}) {
                Motor mo = new Motor(leerMapa(f), params.copia(), s -> { });
                mo.iniciar();
                while (mo.paso()) { /* headless */ }
                tm.addRow(new Object[]{f.getName(), mo.mapa.personas.size(), mo.evacuadas(), mo.tick,
                        String.format("%.1f", mo.promedioEvac()), String.format("%.2f", mo.promedioEstres())});
            }
            JTable t = new JTable(tm);
            t.setEnabled(false);
            JPanel p = new JPanel(new BorderLayout());
            p.add(new JScrollPane(t), BorderLayout.CENTER);
            p.setPreferredSize(new Dimension(620, 110));
            JOptionPane.showMessageDialog(this, p, "Comparar escenarios", JOptionPane.PLAIN_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compararAlgoritmos() {
        Mapa m = vista();
        StringBuilder sb = new StringBuilder();
        int eD = 0, eA = 0;
        long tD = 0, tA = 0;
        int n = 0;
        sb.append(String.format("%-34s %-22s %-22s%n", "Persona", "Dijkstra (expl./costo)", "A* (expl./costo)"));
        for (Persona p : m.personas) {
            if (p.evacuada) continue;
            Resultado d = buscar(m, p.f, p.c, m.salidas(), false);
            Resultado a = buscar(m, p.f, p.c, m.salidas(), true);
            sb.append(String.format("%-34s %-22s %-22s%n", p.etiqueta(),
                    d.explorados + " / " + (d.ruta == null ? "sin ruta" : (int) d.costo),
                    a.explorados + " / " + (a.ruta == null ? "sin ruta" : (int) a.costo)));
            eD += d.explorados;
            eA += a.explorados;
            tD += d.nanos;
            tA += a.nanos;
            n++;
        }
        if (n == 0) {
            JOptionPane.showMessageDialog(this, "No hay personas para comparar.", "Comparar Dijkstra vs A*", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        sb.append(String.format("%nTOTAL nodos explorados: Dijkstra=%d  A*=%d%n", eD, eA));
        sb.append(String.format("TIEMPO de calculo: Dijkstra=%d us  A*=%d us%n", tD / 1000, tA / 1000));
        mostrarTexto("Comparar Dijkstra vs A*", sb.toString());
    }

    private void reporte() {
        if (!requiereMotor()) return;
        mostrarTexto("Reporte de simulacion", motor.reporte());
    }

    private void exportarIndividual() {
        if (!requiereMotor()) return;
        List<Persona> ps = motor.mapa.personas;
        JComboBox<String> cb = new JComboBox<>();
        for (Persona p : ps) cb.addItem(p.etiqueta());
        if (JOptionPane.showConfirmDialog(this, cb, "Persona a exportar", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Persona p = ps.get(cb.getSelectedIndex());
        File f = elegirArchivo(false, "reporte_P" + p.id + ".txt");
        if (f == null) return;
        try {
            Files.write(f.toPath(), motor.reporteIndividual(p).getBytes(StandardCharsets.UTF_8));
            log("Reporte individual exportado: " + f.getName());
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea ta = new JTextArea(texto, 20, 78);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), titulo, JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new EvacuaPrototipo().setVisible(true));
    }
}
