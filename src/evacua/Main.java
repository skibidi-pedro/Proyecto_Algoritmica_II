package evacua;

import evacua.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada del simulador Evacua. */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
