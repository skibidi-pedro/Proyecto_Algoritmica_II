package evacua.vista;

import evacua.edificio.Nodo;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PruebasInterfaz {
    private static void botones(Container raiz, Map<String, AbstractButton> encontrados) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                encontrados.put(b.getText(), b);
            }
            if (c instanceof Container) botones((Container) c, encontrados);
        }
    }

    public static void main(String[] args) throws Exception {
        Nodo n = new Nodo();
        n.danado = true;
        n.ocupacion = 1;
        float lleno = PanelEdificio.intensidadCalor(n, true, 2, 5);
        n.danado = false;
        if (lleno <= PanelEdificio.intensidadCalor(n, true, 2, 5)) throw new AssertionError("Capacidad efectiva en vivo");
        n.ocupacion = 0;
        n.visitas = 5;
        if (PanelEdificio.intensidadCalor(n, true, 2, 5) != 0 || PanelEdificio.intensidadCalor(n, false, 2, 5) <= 0)
            throw new AssertionError("Ocupación actual e histórica");
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Calor: OK. La prueba de botones requiere una pantalla.");
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal v = new VentanaPrincipal();
            try {
                Map<String, AbstractButton> botones = new HashMap<>();
                botones(v, botones);
                for (String etiqueta : new String[]{"Habitacion", "Pasillo", "Escalera", "Salida", "Borrar nodo", "Editar conexión"})
                    if (botones.containsKey(etiqueta)) throw new AssertionError(etiqueta);
                if (!botones.containsKey("Bloquear / desbloquear zonas")) throw new AssertionError("Falta bloqueo");
                botones.get("Ver mapa de calor").doClick();
                if (!v.verCalor) throw new AssertionError("Activación del mapa de calor");
            } finally {
                v.dispose();
            }
        });
        System.out.println("Interfaz y calor: TODAS LAS PRUEBAS OK");
    }
}
