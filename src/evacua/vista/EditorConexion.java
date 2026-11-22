package evacua.vista;

import evacua.edificio.*;
import javax.swing.*;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

final class EditorConexion {
    static void abrir(VentanaPrincipal ventana) {
        Edificio e = ventana.vista();
        List<Integer> ids = new ArrayList<>();
        JComboBox<String> origen = new JComboBox<>(), destino = new JComboBox<>();
        for (int f = 0; f < e.filas; f++) for (int c = 0; c < e.cols; c++) {
            if (e.n[f][c].tipo == TipoNodo.VACIO) continue;
            String nombre = "Piso " + (1 + e.piso(f)) + " · (" + (f % e.filasPorPiso) + ", " + c + ")";
            ids.add(f * e.cols + c);
            origen.addItem(nombre);
            destino.addItem(nombre);
        }
        if (ids.size() < 2) {
            JOptionPane.showMessageDialog(ventana, "Se necesitan dos nodos para editar una conexión.");
            return;
        }
        destino.setSelectedIndex(1);
        JTextField tiempo = new JTextField();
        JSpinner capacidad = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JComboBox<Condicion> condicion = new JComboBox<>(Condicion.values());
        JCheckBox bloqueo = new JCheckBox("Bloqueada");
        Grafo grafo = Grafo.desde(e);
        Runnable mostrar = () -> {
            int a = ids.get(origen.getSelectedIndex()), b = ids.get(destino.getSelectedIndex());
            Grafo.Arista ar = grafo.arista(a, b);
            tiempo.setText(String.valueOf(ar == null ? 1 : ar.tiempo));
            capacidad.setValue(ar == null ? Grafo.CAPACIDAD_DEFECTO : ar.capacidad);
            condicion.setSelectedItem(Condicion.NORMAL);
            bloqueo.setSelected(false);
            for (Conexion k : e.conexiones) {
                if ((k.origen == a && k.destino == b) || (k.origen == b && k.destino == a)) {
                    condicion.setSelectedItem(k.condicion);
                    bloqueo.setSelected(k.bloqueada);
                }
            }
        };
        origen.addActionListener(ev -> mostrar.run());
        destino.addActionListener(ev -> mostrar.run());
        mostrar.run();
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Desde")); panel.add(origen);
        panel.add(new JLabel("Hasta")); panel.add(destino);
        panel.add(new JLabel("Tiempo de recorrido")); panel.add(tiempo);
        panel.add(new JLabel("Capacidad por tick")); panel.add(capacidad);
        panel.add(new JLabel("Condición")); panel.add(condicion);
        panel.add(bloqueo);
        if (JOptionPane.showConfirmDialog(ventana, panel, "Crear o editar conexión bidireccional",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            int a = ids.get(origen.getSelectedIndex()), b = ids.get(destino.getSelectedIndex());
            double t = Double.parseDouble(tiempo.getText().trim());
            capacidad.commitEdit();
            int cap = (int) capacidad.getValue();
            if (a == b || !Double.isFinite(t) || t <= 0)
                throw new IllegalArgumentException("Elija nodos distintos y un tiempo positivo.");
            if (ventana.motor == null) ventana.guardarParaDeshacer();
            Conexion k = e.conectar(a / e.cols, a % e.cols, b / e.cols, b % e.cols, t, cap);
            k.bloqueada = bloqueo.isSelected();
            k.condicion = (Condicion) condicion.getSelectedItem();
            if (ventana.motor != null) {
                ventana.motor.grafo = Grafo.desde(e);
                ventana.motor.recalcularTodas();
            }
            ventana.grid.repaint();
            ventana.actualizarEstado();
        } catch (IllegalArgumentException | java.text.ParseException ex) {
            JOptionPane.showMessageDialog(ventana, "Conexión inválida: " + ex.getMessage());
        }
    }
}
