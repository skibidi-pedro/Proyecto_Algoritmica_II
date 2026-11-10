package evacua.vista;

import evacua.edificio.Edificio;
import evacua.edificio.HistorialEdicion;
import evacua.edificio.TipoNodo;
import evacua.persistencia.BaseDatos;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;
import evacua.reportes.ComparadorEscenarios;
import evacua.reportes.ExportadorIndividual;
import evacua.reportes.ExportadorSalidas;
import evacua.reportes.ReporteSimulacion;
import evacua.rutas.BuscadorRutas;
import evacua.rutas.ResultadoRuta;
import evacua.simulacion.MotorSimulacion;
import evacua.simulacion.ParametrosSimulacion;

import javax.swing.*;
import javax.swing.border.*;
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
    final BaseDatos bd = new BaseDatos("datos");
    final HistorialEdicion historial = new HistorialEdicion();
    JButton btnDeshacer;
    int pisoActual;            // 0 = piso 1
    JPanel panelPisos;
    int pisosMostrados = -1;
    boolean selectorEnEdicion;

    Edificio vista() {
        return motor != null ? motor.mapa : mapa;
    }

    public VentanaPrincipal() {
        super("EVACUA");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        mapa = crearEdificioInicial();
        grid = new PanelEdificio(this);
        timer = new javax.swing.Timer(250, e -> avanzar());

        setLayout(new BorderLayout(6, 6));
        add(crearPanelIzquierdo(), BorderLayout.WEST);
        add(new JScrollPane(grid), BorderLayout.CENTER);
        panelPisos = new JPanel();
        panelPisos.setLayout(new BoxLayout(panelPisos, BoxLayout.Y_AXIS));
        panelPisos.setBorder(new CompoundBorder(new TitledBorder("Pisos"), new EmptyBorder(2, 4, 4, 4)));
        JPanel este = new JPanel(new BorderLayout());
        este.add(panelPisos, BorderLayout.NORTH);
        add(este, BorderLayout.EAST);

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel sur = new JPanel(new BorderLayout());
        estado = new JLabel(" ");
        estado.setBorder(new EmptyBorder(2, 6, 2, 6));
        sur.add(estado, BorderLayout.NORTH);
        sur.add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Z"), "deshacer");
        getRootPane().getActionMap().put("deshacer", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deshacer();
            }
        });
        grid.ajustarTamano();
        actualizarEstado();
        log("Listo. Elija una herramienta o pulse 'Iniciar emergencia y simular evacuacion'.");
        setSize(1280, 840);
        setLocationRelativeTo(null);
    }

    Edificio crearEdificioInicial() {
        Edificio m = new Edificio(12, 20, 3);
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
        // pisos 2 y 3: mismo plano, escalera en la misma posicion y sin salidas (se evacua por el piso 1)
        for (int piso = 1; piso < 3; piso++) {
            int b = piso * 12;
            m.rect(TipoNodo.PASILLO, b + 5, 1, b + 6, 18);
            for (int[] r : new int[][]{{1, 1, 3, 5}, {1, 7, 3, 11}, {1, 13, 3, 17}, {8, 1, 10, 5}, {8, 7, 10, 11}, {8, 13, 10, 17}})
                m.rect(TipoNodo.HABITACION, b + r[0], r[1], b + r[2], r[3]);
            for (int c : new int[]{3, 9, 15}) {
                m.n[b + 4][c].tipo = TipoNodo.PASILLO;
                m.n[b + 7][c].tipo = TipoNodo.PASILLO;
            }
            m.n[b + 5][10].tipo = TipoNodo.ESCALERA;
        }
        m.agregar(TipoPersona.ADULTO, EstadoSalud.SANO, 12 + 2, 3);
        m.agregar(TipoPersona.NINO, EstadoSalud.SANO, 12 + 9, 15);
        m.agregar(TipoPersona.ADULTO, EstadoSalud.HERIDO, 24 + 2, 9);
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
                boton("Importar plano del edificio", "Lee y valida un plano .json, .xml o .txt", e -> importarPlano()),
                boton("Guardar en base de datos", "Guarda el escenario (RN-10) en la base de datos embebida", e -> guardarEnBD()),
                boton("Cargar de base de datos", "Carga un escenario guardado", e -> cargarDeBD()),
                boton("Comparar escenarios guardados", "Simula escenarios de la base de datos y compara resultados", e -> compararGuardados()),
                boton("Comparar escenarios", "Simula dos escenarios y compara resultados", e -> compararEscenarios())));

        // Gestionar edificio (nodos y conexiones)
        btnDeshacer = boton("Deshacer (Ctrl+Z)", "Deshace el ultimo cambio al edificio (solo en modo edicion)", e -> deshacer());
        p.add(grupo("Gestionar edificio (nodos y conexiones)",
                btnDeshacer,
                tool("Sin herramienta", Herramienta.NINGUNA, herramientas, true, "Solo observar"),
                tool("Habitacion", Herramienta.HABITACION, herramientas, false, "Pinta nodos de tipo habitacion"),
                tool("Pasillo", Herramienta.PASILLO, herramientas, false, "Pinta nodos de tipo pasillo"),
                tool("Escalera", Herramienta.ESCALERA, herramientas, false, "Pinta escaleras. Conectan con la escalera de la misma posicion en el piso de arriba y abajo"),
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
                boton("Exportar reporte individual por persona", "Linea de tiempo de una persona a .txt", e -> exportarIndividual()),
                boton("Exportar reportes de todas las personas", "Un archivo por persona en una carpeta", e -> exportarTodos()),
                boton("Exportar salidas (CSV)", "Tiempo y estado de quienes salieron, en orden de salida", e -> exportarSalidas())));

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

    /** Selector de pisos (a la derecha): un boton por piso y, en modo edicion, un boton para agregar piso. */
    void reconstruirSelector() {
        Edificio m = vista();
        if (pisoActual >= m.pisos) pisoActual = m.pisos - 1;
        panelPisos.removeAll();
        ButtonGroup grupo = new ButtonGroup();
        for (int k = m.pisos - 1; k >= 0; k--) {   // el piso mas alto arriba, como en un edificio
            final int piso = k;
            JToggleButton b = new JToggleButton("Piso " + (k + 1), k == pisoActual);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            b.addActionListener(e -> {
                pisoActual = piso;
                grid.repaint();
                actualizarEstado();
            });
            grupo.add(b);
            panelPisos.add(b);
        }
        selectorEnEdicion = motor == null;
        JButton mas = new JButton("+ Agregar piso");
        mas.setEnabled(selectorEnEdicion);
        mas.setToolTipText("Agrega un piso vacio arriba del ultimo (solo en modo edicion)");
        mas.setMaximumSize(new Dimension(Integer.MAX_VALUE, mas.getPreferredSize().height));
        mas.addActionListener(e -> agregarPiso());
        panelPisos.add(Box.createVerticalStrut(6));
        panelPisos.add(mas);
        pisosMostrados = m.pisos;
        panelPisos.revalidate();
        panelPisos.repaint();
    }

    /** Guarda el edificio de edicion en la pila antes de cambiarlo. */
    void guardarParaDeshacer() {
        historial.guardar(mapa);
        btnDeshacer.setEnabled(motor == null);
    }

    void deshacer() {
        if (motor != null) {
            log("Para deshacer vuelva primero a edicion.");
            return;
        }
        Edificio anterior = historial.deshacer();
        if (anterior == null) {
            log("No hay nada que deshacer.");
            return;
        }
        mapa = anterior;
        grid.ajustarTamano();
        actualizarEstado();
        log("Cambio deshecho (" + historial.tamano() + " mas en el historial).");
    }

    void agregarPiso() {
        if (motor != null) return;
        guardarParaDeshacer();
        mapa = mapa.copiaConPisos(mapa.pisos + 1);
        pisoActual = mapa.pisos - 1;
        grid.repaint();
        actualizarEstado();
        log("Piso " + mapa.pisos + " agregado. Pinte sus nodos; las escaleras se conectan con las del piso de abajo.");
    }

    void actualizarEstado() {
        Edificio m = vista();
        btnDeshacer.setEnabled(motor == null && historial.puedeDeshacer());
        if (m.pisos != pisosMostrados || selectorEnEdicion != (motor == null)) reconstruirSelector();
        if (motor == null) {
            estado.setText("Modo edicion  |  Piso " + (pisoActual + 1) + "/" + m.pisos + "  |  Personas: " + m.personas.size() + "  |  Algoritmo: " + (params.astar ? "A*" : "Dijkstra"));
        } else {
            estado.setText("Simulando  |  Piso " + (pisoActual + 1) + "/" + m.pisos + "  |  t=" + motor.tick + "  |  Evacuadas: " + motor.evacuadas() + "/" + m.personas.size()
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
        motor.alTerminar = this::guardarSalidasCsv;
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

    /** Al terminar la simulacion guarda datos/salidas.csv con el tiempo y estado de quienes salieron. */
    void guardarSalidasCsv() {
        try {
            File f = new File("datos", "salidas.csv");
            int n = ExportadorSalidas.exportar(motor, f);
            log("Salidas guardadas en " + f.getPath() + " (" + n + " personas).");
        } catch (java.io.IOException ex) {
            log("No se pudo guardar el CSV de salidas: " + ex.getMessage());
        }
    }

    void exportarSalidas() {
        if (!requiereMotor()) return;
        JFileChooser fc = new JFileChooser(ultimoDir);
        fc.setSelectedFile(new File("salidas.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            int n = ExportadorSalidas.exportar(motor, fc.getSelectedFile());
            log("Salidas exportadas a " + fc.getSelectedFile().getName() + " (" + n + " personas).");
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
            guardarParaDeshacer();
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
            List<String> errores = GestorEscenarios.validarParaGuardar(mapa);
            if (!errores.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El escenario no es valido: " + String.join(" ", errores), "Validar escenario", JOptionPane.ERROR_MESSAGE);
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
            guardarParaDeshacer();
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
            List<ComparadorEscenarios.Fila> filas = new java.util.ArrayList<>();
            for (File f : new File[]{a, b}) filas.add(ComparadorEscenarios.simular(f.getName(), GestorEscenarios.leer(f), params));
            mostrarTexto("Comparar escenarios", ComparadorEscenarios.aTexto(filas));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void guardarEnBD() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del escenario:", "Guardar en base de datos", JOptionPane.QUESTION_MESSAGE);
        if (nombre == null) return;
        try {
            bd.guardar(nombre.trim(), mapa);
            log("Escenario '" + nombre.trim() + "' guardado en la base de datos.");
        } catch (IllegalArgumentException | java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
        }
    }

    void cargarDeBD() {
        try {
            List<String> nombres = bd.listar();
            if (nombres.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay escenarios guardados.", "Base de datos", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<String> cb = new JComboBox<>(nombres.toArray(new String[0]));
            if (JOptionPane.showConfirmDialog(this, cb, "Escenario a cargar", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            Edificio m = bd.cargar((String) cb.getSelectedItem());
            guardarParaDeshacer();
            timer.stop();
            motor = null;
            mapa = m;
            grid.ajustarTamano();
            actualizarEstado();
            log("Escenario cargado de la base de datos: " + cb.getSelectedItem() + ".");
        } catch (IllegalArgumentException | java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cargar", JOptionPane.ERROR_MESSAGE);
        }
    }

    void compararGuardados() {
        try {
            List<String> nombres = bd.listar();
            if (nombres.size() < 2) {
                JOptionPane.showMessageDialog(this, "Guarde al menos dos escenarios en la base de datos.", "Comparar escenarios", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JList<String> lista = new JList<>(nombres.toArray(new String[0]));
            if (JOptionPane.showConfirmDialog(this, new JScrollPane(lista), "Seleccione dos o mas escenarios", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            List<String> sel = lista.getSelectedValuesList();
            if (sel.size() < 2) {
                JOptionPane.showMessageDialog(this, "Seleccione al menos dos escenarios.");
                return;
            }
            mostrarTexto("Comparar escenarios guardados", ComparadorEscenarios.aTexto(ComparadorEscenarios.compararGuardados(bd, sel, params)));
        } catch (IllegalArgumentException | java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void exportarTodos() {
        if (!requiereMotor()) return;
        JFileChooser fc = new JFileChooser(ultimoDir);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            int k = ExportadorIndividual.exportarTodos(motor, fc.getSelectedFile());
            log("Exportados " + k + " reportes individuales en " + fc.getSelectedFile().getName() + ".");
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
