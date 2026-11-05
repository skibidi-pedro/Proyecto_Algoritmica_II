package evacua.vista;

import evacua.edificio.Edificio;
import evacua.edificio.TipoNodo;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;
import evacua.reportes.ReporteSimulacion;
import evacua.rutas.BuscadorRutas;
import evacua.rutas.ResultadoRuta;
import evacua.simulacion.MotorSimulacion;
import evacua.simulacion.ParametrosSimulacion;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Ventana principal (Swing): botones de casos de uso, cuadricula, estado y registro. */
public class VentanaPrincipal extends JFrame {

    Edificio mapa;
    MotorSimulacion motor;
    ParametrosSimulacion params = new ParametrosSimulacion();
    Herramienta herramienta = Herramienta.NINGUNA;
    boolean verCalor;
    PanelEdificio grid;
    JTextArea logArea;
    JLabel estado;
    javax.swing.Timer timer;
    JComboBox<TipoPersona> cmbTipo;
    JComboBox<EstadoSalud> cmbSalud;
    JCheckBox chkTiempoReal;
    JSlider velocidad;
    File ultimoDir = new File(".");

    Edificio vista() {
        return motor != null ? motor.mapa : mapa;
    }

    public VentanaPrincipal() {
        super("Evacua - Prototipo de interfaz");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        mapa = crearDemo();
        grid = new PanelEdificio(this);
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

    Edificio crearDemo() {
        Edificio m = new Edificio(12, 20);
        m.rect(TipoNodo.PASILLO, 5, 1, 6, 18);
        m.rect(TipoNodo.HABITACION, 1, 1, 3, 5);
        m.rect(TipoNodo.HABITACION, 1, 7, 3, 11);
        m.rect(TipoNodo.HABITACION, 1, 13, 3, 17);
        m.rect(TipoNodo.HABITACION, 8, 1, 10, 5);
        m.rect(TipoNodo.HABITACION, 8, 7, 10, 11);
        m.rect(TipoNodo.HABITACION, 8, 13, 10, 17);
        for (int c : new int[]{3, 9, 15}) {
            m.n[4][c].tipo = TipoNodo.PASILLO;
            m.n[7][c].tipo = TipoNodo.PASILLO;
        }
        m.n[5][10].tipo = TipoNodo.ESCALERA;
        m.n[5][0].tipo = TipoNodo.SALIDA;
        m.n[6][19].tipo = TipoNodo.SALIDA;
        m.agregar(TipoPersona.ADULTO, EstadoSalud.SANO, 2, 3);
        m.agregar(TipoPersona.NINO, EstadoSalud.SANO, 2, 9);
        m.agregar(TipoPersona.MOVILIDAD, EstadoSalud.SANO, 9, 3);
        m.agregar(TipoPersona.ADULTO, EstadoSalud.HERIDO, 9, 15);
        m.agregar(TipoPersona.ADULTO, EstadoSalud.INCAPACITADO, 9, 9);
        m.agregar(TipoPersona.RESCATE, EstadoSalud.SANO, 2, 15);
        return m;
    }

    // ---------- panel de casos de uso (botones) ----------

    JComponent crearPanelIzquierdo() {
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

    JComponent crearLeyenda() {
        JPanel l = new JPanel(new GridLayout(0, 3, 3, 3));
        l.setBorder(new TitledBorder("Leyenda"));
        for (TipoNodo t : TipoNodo.values()) {
            if (t == TipoNodo.VACIO) continue;
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

    JPanel grupo(String titulo, Component... comps) {
        JPanel g = new JPanel(new GridLayout(0, 1, 3, 3));
        g.setBorder(new CompoundBorder(new TitledBorder(titulo), new EmptyBorder(2, 4, 4, 4)));
        for (Component c : comps) g.add(c);
        g.setMaximumSize(new Dimension(Integer.MAX_VALUE, g.getPreferredSize().height));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        return g;
    }

    JButton boton(String texto, String tip, ActionListener a) {
        JButton b = new JButton(texto);
        b.setToolTipText(tip);
        b.addActionListener(a);
        return b;
    }

    JToggleButton tool(String texto, Herramienta h, ButtonGroup g, boolean sel, String tip) {
        JToggleButton b = new JToggleButton(texto, sel);
        b.setToolTipText(tip);
        b.addActionListener(e -> herramienta = h);
        g.add(b);
        return b;
    }

    // ---------- acciones ----------

    void log(String s) {
        if (logArea == null) return;
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    void actualizarEstado() {
        Edificio m = vista();
        if (motor == null) {
            estado.setText("Modo edicion  |  Personas: " + m.personas.size() + "  |  Algoritmo: " + (params.astar ? "A*" : "Dijkstra"));
        } else {
            estado.setText("Simulando  |  t=" + motor.tick + "  |  Evacuadas: " + motor.evacuadas() + "/" + m.personas.size()
                    + "  |  Algoritmo: " + (params.astar ? "A*" : "Dijkstra") + (motor.terminado ? "  |  TERMINADA" : ""));
        }
    }

    void iniciar() {
        if (mapa.personas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Registre al menos una persona antes de iniciar.", "Sin personas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        timer.stop();
        Edificio copia = mapa.copia();
        motor = new MotorSimulacion(copia, params.copia(), this::log);
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

    void avanzar() {
        if (motor == null || !motor.paso()) timer.stop();
        grid.repaint();
        actualizarEstado();
    }

    void volverAEdicion() {
        timer.stop();
        motor = null;
        grid.ajustarTamano();
        actualizarEstado();
        log("Vuelve al modo edicion.");
    }

    boolean requiereMotor() {
        if (motor == null) {
            JOptionPane.showMessageDialog(this, "Primero inicie una emergencia.", "Sin simulacion", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    void eventoManual() {
        if (!requiereMotor()) return;
        motor.generarEvento();
        grid.repaint();
    }

    void recalcularManual() {
        if (!requiereMotor()) return;
        motor.recalcularTodas();
        grid.repaint();
    }

    void configurar() {
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

    File elegirArchivo(boolean abrir, String sugerido) {
        JFileChooser fc = new JFileChooser(ultimoDir);
        if (sugerido != null) fc.setSelectedFile(new File(ultimoDir, sugerido));
        int r = abrir ? fc.showOpenDialog(this) : fc.showSaveDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        ultimoDir = f.getParentFile() != null ? f.getParentFile() : ultimoDir;
        if (!abrir && !f.getName().contains(".")) f = new File(f.getPath() + ".txt");
        return f;
    }


    void cargarEscenario() {
        File f = elegirArchivo(true, null);
        if (f == null) return;
        try {
            Edificio m = GestorEscenarios.leer(f);
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

    void guardarEscenario() {
        File f = elegirArchivo(false, "escenario.txt");
        if (f == null) return;
        try {
            if (mapa.salidas().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El escenario no es valido: necesita al menos una salida sin bloquear.", "Validar escenario", JOptionPane.ERROR_MESSAGE);
                return;
            }
            GestorEscenarios.guardar(f, mapa);
            log("Escenario guardado en " + f.getName() + ".");
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void importarPlano() {
        File f = elegirArchivo(true, null);
        if (f == null) return;
        try {
            Edificio m = GestorEscenarios.leer(f);
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

    void compararEscenarios() {
        JOptionPane.showMessageDialog(this, "Seleccione el escenario A y luego el escenario B.");
        File a = elegirArchivo(true, null);
        if (a == null) return;
        File b = elegirArchivo(true, null);
        if (b == null) return;
        try {
            DefaultTableModel tm = new DefaultTableModel(
                    new Object[]{"Escenario", "Personas", "Evacuadas", "Ticks", "T. promedio", "Estres prom."}, 0);
            for (File f : new File[]{a, b}) {
                MotorSimulacion mo = new MotorSimulacion(GestorEscenarios.leer(f), params.copia(), s -> { });
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

    void compararAlgoritmos() {
        Edificio m = vista();
        StringBuilder sb = new StringBuilder();
        int eD = 0, eA = 0;
        long tD = 0, tA = 0;
        int n = 0;
        sb.append(String.format("%-34s %-22s %-22s%n", "Persona", "Dijkstra (expl./costo)", "A* (expl./costo)"));
        for (Persona p : m.personas) {
            if (p.evacuada) continue;
            ResultadoRuta d = BuscadorRutas.buscar(m, p.f, p.c, m.salidas(), false);
            ResultadoRuta a = BuscadorRutas.buscar(m, p.f, p.c, m.salidas(), true);
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

    void reporte() {
        if (!requiereMotor()) return;
        mostrarTexto("Reporte de simulacion", ReporteSimulacion.general(motor));
    }

    void exportarIndividual() {
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
            Files.write(f.toPath(), ReporteSimulacion.individual(p).getBytes(StandardCharsets.UTF_8));
            log("Reporte individual exportado: " + f.getName());
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void mostrarTexto(String titulo, String texto) {
        JTextArea ta = new JTextArea(texto, 20, 78);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
