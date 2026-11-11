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
        if (v.herramienta == Herramienta.BLOQUEO) {
            if (nd.tipo == TipoNodo.VACIO) return;
            if (valorArrastre == null) valorArrastre = !nd.bloqueado;
            if (nd.bloqueado != valorArrastre) {
                nd.bloqueado = valorArrastre;
                v.log((valorArrastre ? "Zona bloqueada" : "Zona desbloqueada") + " en (" + f + "," + c + ").");
                if (v.motor != null) {
                    if (valorArrastre) v.motor.alBloquear(f, c);
                    else v.motor.recalcularTodas();
                }
            }
        } else if (v.motor != null) {
            if (click) v.log("Para editar el edificio vuelva a edicion (boton 'Volver a edicion').");
        } else {
            switch (v.herramienta) {
                case HABITACION: nd.tipo = TipoNodo.HABITACION; break;
                case PASILLO: nd.tipo = TipoNodo.PASILLO; break;
                case ESCALERA: nd.tipo = TipoNodo.ESCALERA; break;
                case SALIDA: nd.tipo = TipoNodo.SALIDA; break;
                case BORRAR:
                    nd.tipo = TipoNodo.VACIO;
                    nd.bloqueado = false;
                    m.personas.removeIf(p -> p.f == f && p.c == c);
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
                float calor = enVivo ? (nd.ocupacion > 0 ? 0.25f + 0.6f * Math.min(1f, nd.ocupacion / (float) v.motor.params.capacidad) : 0f)
                        : (nd.visitas > 0 ? 0.12f + 0.73f * nd.visitas / maxV : 0f);
                if (v.verCalor && calor > 0) {
                    float a = Math.min(0.85f, calor);
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
