package evacua;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.rutas.BuscadorRutas;
import evacua.simulacion.*;

public class PruebasEmergencia {
    static void exigir(boolean cumple, String mensaje) {
        if (!cumple) throw new AssertionError(mensaje);
    }

    static MotorSimulacion iniciar(String plano, TipoDesastre tipo) {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.desastre = tipo;
        p.prob = 1;
        MotorSimulacion motor = new MotorSimulacion(GestorEscenarios.deTexto(plano), p, s -> {});
        motor.iniciar();
        return motor;
    }

    public static void main(String[] args) {
        MotorSimulacion m = iniciar("HS\n#PERSONAS\n0 0 ADULTO SANO\n0 0 ADULTO SANO", TipoDesastre.CLASICO);
        m.mapa.conectar(0, 0, 0, 1, 1, 1);
        m.grafo = Grafo.desde(m.mapa);
        m.paso();
        exigir(m.evacuadas() == 1, "RN-03: solo una persona cruza por tick");
        m.paso();
        exigir(m.evacuadas() == 2, "El cupo se libera en el siguiente tick");

        for (TipoDesastre tipo : TipoDesastre.values()) {
            String plano = "HPPPS\nPPPPP\n#PERSONAS\n0 0 ADULTO INCAPACITADO";
            MotorSimulacion a = iniciar(plano, tipo), b = iniciar(plano, tipo);
            for (int i = 0; i < 3; i++) {
                a.paso();
                b.paso();
                int afectados = 0;
                for (int f = 0; f < a.mapa.filas; f++) for (int c = 0; c < a.mapa.cols; c++) {
                    Nodo x = a.mapa.n[f][c], y = b.mapa.n[f][c];
                    exigir(x.bloqueado == y.bloqueado && x.condicion == y.condicion, "RN-18: " + tipo);
                    if (x.bloqueado || x.condicion == Condicion.PELIGROSO) afectados++;
                    if (tipo == TipoDesastre.FUGA_DE_GAS) exigir(!x.bloqueado, "Gas transitable");
                }
                exigir(afectados == i + 1, "RN-20: evolución de " + tipo);
            }
        }
        Edificio e = GestorEscenarios.deTexto("HPS");
        Conexion k = e.conectar(0, 0, 0, 1, 1, 1);
        k.condicion = Condicion.DANADO;
        for (boolean astar : new boolean[]{true, false}) {
            exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).ruta == null, "RN-05: conexión dañada");
        }
        k.condicion = Condicion.PELIGROSO;
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), true).costo == 3, "RN-16: coste por peligro");
        System.out.println("Emergencias: TODAS LAS PRUEBAS OK");
    }
}
