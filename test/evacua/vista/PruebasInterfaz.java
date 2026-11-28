package evacua.vista;

import evacua.edificio.Nodo;
import evacua.simulacion.TipoGas;
import java.awt.image.BufferedImage;
import java.util.HashSet;
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

    private static void indicadoresGas() {
        HashSet<Integer> tonos = new HashSet<>();
        for (TipoGas gas : TipoGas.values()) {
            Color baja = PanelEdificio.colorGas(gas, 25);
            Color alta = PanelEdificio.colorGas(gas, 100);
            if (PanelEdificio.colorGas(gas, 0).getAlpha() != 0 || baja.getAlpha() <= 0
                    || alta.getAlpha() <= baja.getAlpha() || !tonos.add(alta.getRGB() & 0xffffff))
                throw new AssertionError("Color o intensidad de " + gas);
        }
        int lado = PanelEdificio.CELL;
        BufferedImage celda = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = celda.createGraphics();
        try {
            PanelEdificio.dibujarFuente(g, 0, 0, false);
            if (celda.getRGB(lado - 8, 8) != 0xffef7815) throw new AssertionError("Fuente activa");
            PanelEdificio.dibujarFuente(g, 0, 0, true);
            if (celda.getRGB(lado - 8, 8) != 0xff15803d) throw new AssertionError("Fuente reparada");
        } finally {
            g.dispose();
        }
    }

    public static void main(String[] args) throws Exception {
        indicadoresGas();
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
            System.out.println("Calor e indicadores de gas: OK. La prueba de botones requiere una pantalla.");
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal v = new VentanaPrincipal();
            try {
                Map<String, AbstractButton> botones = new HashMap<>();
                botones(v, botones);
                for (String etiqueta : new String[]{"Habitacion", "Pasillo", "Escalera", "Salida", "Borrar nodo", "Puerta", "Mostrar / ocultar departamentos"})
                    if (!botones.containsKey(etiqueta)) throw new AssertionError(etiqueta);
                if (!botones.containsKey("Bloquear / desbloquear zonas")) throw new AssertionError("Falta bloqueo");
                if (botones.containsKey("Editar conexión")) throw new AssertionError("Editor de conexiones visible");
                v.mapa = evacua.persistencia.GestorEscenarios.deTexto("H.PS");
                for (String etiqueta : new String[]{"Puerta", "Borrar nodo"}) {
                    botones.get(etiqueta).doClick();
                    v.grid.dispatchEvent(new java.awt.event.MouseEvent(v.grid, java.awt.event.MouseEvent.MOUSE_PRESSED,
                            0, 0, PanelEdificio.CELL + 2, 2, 1, false));
                    int esperado = etiqueta.equals("Puerta") ? 1 : 0;
                    if (v.mapa.n[0][0].departamento != esperado) throw new AssertionError(etiqueta);
                    botones.get("Mostrar / ocultar departamentos").doClick();
                    if (v.verDepartamentos != (esperado == 1) || v.mapa.n[0][0].departamento != esperado)
                        throw new AssertionError("Alternar departamentos");
                }
                botones.get("Ver mapa de calor").doClick();
                if (!v.verCalor) throw new AssertionError("Activación del mapa de calor");
            } finally {
                v.dispose();
            }
        });
        System.out.println("Interfaz y calor: TODAS LAS PRUEBAS OK");
    }
}
