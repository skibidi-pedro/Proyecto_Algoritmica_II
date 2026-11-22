package evacua.vista;

import evacua.edificio.Edificio;
import evacua.edificio.Nodo;
import evacua.edificio.TipoNodo;
import evacua.personas.EstadoSalud;
import evacua.personas.Persona;
import evacua.personas.TipoPersona;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class PanelEdificio extends JPanel {
    static final int CELL = 38;

    static float intensidadCalor(Nodo n, boolean enVivo, int capacidad, int maxVisitas) {
        if (!enVivo) return n.visitas > 0 ? 0.12f + 0.73f * n.visitas / maxVisitas : 0;
        if (n.bloqueado || n.ocupacion == 0) return 0;
        return 0.25f + 0.6f * Math.min(1f, (float) n.ocupacion / n.capacidadEfectiva(capacidad));
    }

    private final VentanaPrincipal v;
    private Boolean valorArrastre;

    PanelEdificio(VentanaPrincipal v) {
        this.v = v;
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
        Edificio m = v.vista();
        setPreferredSize(new Dimension(m.cols * CELL + 1, m.filasPorPiso * CELL + 1));
        revalidate();
        repaint();
    }

    private void aplicar(MouseEvent e, boolean click) {
        Edificio m = v.vista();
        int piso = Math.min(v.pisoActual, m.pisos - 1);
        if (e.getY() / CELL >= m.filasPorPiso) return;
        int c = e.getX() / CELL, f = piso * m.filasPorPiso + e.getY() / CELL;
        if (!m.dentro(f, c)) return;
        Nodo nd = m.n[f][c];
        if (click && v.motor == null && v.herramienta != Herramienta.NINGUNA) v.guardarParaDeshacer();
        if (v.herramienta == Herramienta.ESTADO) {
            if (!click || nd.tipo == TipoNodo.VACIO) return;
            boolean corriendo = v.timer.isRunning();
            v.timer.stop();
            JCheckBox dano = new JCheckBox("Dañado", nd.danado);
            JCheckBox peligro = new JCheckBox("Peligroso", nd.peligroso);
            if (JOptionPane.showConfirmDialog(v, new Object[]{dano, peligro}, "Estado del nodo",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                nd.danado = dano.isSelected();
                nd.peligroso = peligro.isSelected();
                if (v.motor != null) v.motor.recalcularTodas();
            }
            if (corriendo) v.timer.start();
        } else if (v.herramienta == Herramienta.BLOQUEO) {
            if (nd.tipo == TipoNodo.VACIO) return;
            if (valorArrastre == null) valorArrastre = !nd.bloqueado;
            if (nd.bloqueado != valorArrastre) {
                nd.bloqueado = valorArrastre;
                nd.bloqueoReparable = valorArrastre;
                v.log((valorArrastre ? "Zona bloqueada" : "Zona desbloqueada") + " en (" + f + "," + c + ").");
                if (v.motor != null) {
                    if (valorArrastre) v.motor.alBloquear(f, c);
                    else v.motor.recalcularTodas();
                }
            }
        } else if (v.motor != null) {
            if (click) v.log("Para editar el edificio vuelva a edicion (boton 'Volver a edicion').");
        } else {
            TipoNodo anterior = nd.tipo;
            switch (v.herramienta) {
                case HABITACION: nd.tipo = TipoNodo.HABITACION; break;
                case PUERTA: nd.tipo = TipoNodo.PUERTA; break;
                case PASILLO: nd.tipo = TipoNodo.PASILLO; break;
                case ESCALERA: nd.tipo = TipoNodo.ESCALERA; break;
                case SALIDA: nd.tipo = TipoNodo.SALIDA; break;
                case BORRAR:
                    m.eliminarNodo(f, c);
                    break;
                case PERSONA:
                    if (!click) break;
                    if (!m.caminable(f, c)) {
                        v.log("Solo se puede colocar personas sobre un nodo libre.");
                        break;
                    }
                    boolean habia = m.personas.removeIf(p -> p.f == f && p.c == c);
                    if (!habia) {
                        Persona p = m.agregar((TipoPersona) v.cmbTipo.getSelectedItem(), (EstadoSalud) v.cmbSalud.getSelectedItem(), f, c);
                        v.log("Registrada " + p.etiqueta() + " en (" + f + "," + c + ").");
                    }
                    break;
                default:
                    break;
            }
            if (anterior != nd.tipo) evacua.edificio.Departamentos.detectar(m);
        }
        repaint();
        v.actualizarEstado();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Edificio m = v.vista();
        int maxV = 1;
        for (Nodo[] fila : m.n) for (Nodo nd : fila) maxV = Math.max(maxV, nd.visitas);

        g.setColor(new Color(205, 205, 205));
        int piso = Math.min(v.pisoActual, m.pisos - 1), base = piso * m.filasPorPiso;
        for (int i = 0; i <= m.filasPorPiso; i++) g.drawLine(0, i * CELL, m.cols * CELL, i * CELL);
        for (int j = 0; j <= m.cols; j++) g.drawLine(j * CELL, 0, j * CELL, m.filasPorPiso * CELL);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        for (int i = 0; i < m.filasPorPiso; i++) {
            for (int j = 0; j < m.cols; j++) {
                Nodo nd = m.n[base + i][j];
                if (nd.tipo == TipoNodo.VACIO) continue;
                int x = j * CELL, y = i * CELL;
                g.setColor(nd.tipo.color);
                g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);

                boolean enVivo = v.motor != null && !v.motor.terminado;
                float calor = intensidadCalor(nd, enVivo, enVivo ? v.motor.params.capacidad : v.params.capacidad, maxV);
                if (v.verCalor && calor > 0) {
                    float a = Math.min(0.85f, calor);
                    g.setColor(new Color(1f, 0.1f, 0.0f, a));
                    g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);
                }
                g.setColor(new Color(90, 90, 90));
                g.drawRect(x + 2, y + 2, CELL - 3, CELL - 3);
                g.setColor(new Color(60, 60, 60, 160));
                g.drawString(String.valueOf(nd.tipo.letra), x + CELL / 2 - 5, y + CELL / 2 + 5);
                if (nd.danado || nd.peligroso) {
                    g.setColor(Color.BLACK);
                    g.drawString((nd.danado ? "D" : "") + (nd.peligroso ? "!" : ""), x + 3, y + 14);
                }
                if (nd.bloqueado) {
                    g.setColor(new Color(40, 40, 40, 190));
                    g.fillRect(x + 2, y + 2, CELL - 3, CELL - 3);
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 8, y + 8, x + CELL - 8, y + CELL - 8);
                    g.drawLine(x + CELL - 8, y + 8, x + 8, y + CELL - 8);
                    g.setStroke(new BasicStroke(1));
                }
                if (v.verDepartamentos && nd.departamento > 0) {
                    g.setColor(Color.getHSBColor((nd.departamento * 0.618034f) % 1, 0.85f, 0.7f));
                    g.setStroke(new BasicStroke(nd.tipo == TipoNodo.PUERTA ? 4 : 2));
                    g.drawRect(x + 3, y + 3, CELL - 6, CELL - 6);
                    g.setStroke(new BasicStroke(1));
                    if (nd.tipo == TipoNodo.PUERTA) {
                        Font anterior = g.getFont();
                        g.setFont(anterior.deriveFont(8f));
                        g.setColor(Color.WHITE);
                        g.fillRect(x + 2, y + CELL - 12, CELL - 4, 10);
                        g.setColor(Color.BLACK);
                        g.drawString(evacua.edificio.Departamentos.identificador(nd.departamento), x + 2, y + CELL - 4);
                        g.setFont(anterior);
                    }
                }
            }
        }

        if (v.motor != null && !v.motor.terminado) {
            for (Persona p : m.personas) {
                if (p.evacuada) continue;
                g.setColor(p.tipo.color.darker());
                g.setStroke(new BasicStroke(2));
                int f = p.f, c = p.c;
                for (int[] siguiente : p.ruta) {
                    if (m.piso(f) == piso && m.piso(siguiente[0]) == piso)
                        g.drawLine(c * CELL + CELL / 2, (f - base) * CELL + CELL / 2,
                                siguiente[1] * CELL + CELL / 2, (siguiente[0] - base) * CELL + CELL / 2);
                    f = siguiente[0];
                    c = siguiente[1];
                }
            }
            g.setStroke(new BasicStroke(1));
        }

        Map<Integer, Integer> contador = new HashMap<>();
        double[][] off = {{0.3, 0.3}, {0.7, 0.3}, {0.3, 0.7}, {0.7, 0.7}};
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        for (Persona p : m.personas) {
            if (p.evacuada || m.piso(p.f) != piso) continue;
            int key = p.f * m.cols + p.c;
            int k = contador.merge(key, 1, Integer::sum) - 1;
            double[] o = off[Math.min(k, 3)];
            int cx = (int) (p.c * CELL + o[0] * CELL), cy = (int) ((p.f - base) * CELL + o[1] * CELL);
            int d = 15;
            g.setColor(p.tipo.color);
            g.fillOval(cx - d / 2, cy - d / 2, d, d);
            boolean estresado = v.motor != null && p.estres >= v.motor.params.umbral;
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
