package evacua;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.Persona;
import evacua.rutas.BuscadorRutas;
import evacua.simulacion.*;

public class PruebasRutasDinamicas {
    private static void exigir(boolean ok, String mensaje) {
        if (!ok) throw new AssertionError(mensaje);
    }

    private static MotorSimulacion motor(String plano, boolean astar) {
        ParametrosSimulacion params = new ParametrosSimulacion();
        params.prob = 0;
        params.astar = astar;
        params.capacidad = 1;
        MotorSimulacion m = new MotorSimulacion(GestorEscenarios.deTexto(plano), params, s -> {});
        m.iniciar();
        return m;
    }

    private static void costos(boolean astar) {
        Edificio e = GestorEscenarios.deTexto("HPS\nPPP");
        Nodo n = e.n[0][1];
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).costo == 2, "Ruta libre");
        n.danado = true;
        exigir(n.capacidadEfectiva(3) == 2 && n.capacidadEfectiva(1) == 1, "Capacidad dañada");
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).costo == 3, "Ruta dañada");
        n.peligroso = true;
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).costo == 4, "Desvío seguro");
        e.bloquear(1, 1, true);
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).costo == 11, "Daño y peligro");
        e.bloquear(0, 1, true);
        exigir(n.capacidadEfectiva(3) == 0 && BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).ruta == null, "Bloqueo");
        e.bloquear(0, 1, false);
        n.ocupacion = 2;
        exigir(ControlCapacidad.estado(n, 3) == EstadoNodo.CONGESTIONADO, "Lleno con daño previo");
        n.ocupacion--;
        exigir(ControlCapacidad.estado(n, 3) == EstadoNodo.PELIGROSO && n.pesoEntrada() == 10, "Se libera una plaza");
        e.conectar(0, 0, 0, 2, 0.125, 1);
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).costo == 0.125, "Atajo explícito fraccionario");
    }

    private static void reevaluacion(boolean astar) {
        MotorSimulacion m = motor("HPS\nPPP\n#PERSONAS\n0 0 MOVILIDAD SANO", astar);
        Persona p = m.mapa.personas.get(0);
        m.mapa.n[0][1].peligroso = true;
        m.paso();
        exigir(p.f == 0 && p.c == 0 && p.ruta.get(0)[0] == 1, "Reevalúa incluso antes de poder moverse");
        m.mapa.n[0][1].peligroso = false;
        m.paso();
        exigir(p.f == 0 && p.c == 1, "Aprovecha la mejora en el siguiente tick");

        m = motor("SPHPS\n#PERSONAS\n0 2 MOVILIDAD SANO", astar);
        p = m.mapa.personas.get(0);
        int otro = p.ruta.get(0)[1] == 1 ? 3 : 1;
        p.ruta = new java.util.ArrayList<>(java.util.Arrays.asList(new int[]{0, otro}, new int[]{0, otro == 1 ? 0 : 4}));
        java.util.List<int[]> anterior = p.ruta;
        m.paso();
        exigir(p.ruta == anterior && p.historial.recalculos == 0, "Mantiene la ruta óptima asignada al empatar");
    }

    private static void capacidadSimultanea(boolean astar) {
        MotorSimulacion m = motor("HPS\n.P.\n#PERSONAS\n0 0 ADULTO SANO\n1 1 ADULTO SANO", astar);
        m.paso();
        exigir(m.mapa.n[0][1].ocupacion == 1 && m.mapa.personas.get(1).pasos == 0, "Una sola entrada a la última plaza");
        m.paso();
        exigir(m.evacuadas() == 1 && m.mapa.personas.get(1).pasos == 1, "Se usa la plaza liberada en el mismo tick");
        m.paso();
        exigir(m.evacuadas() == 2, "Se reanuda el recorrido");
    }

    private static void rescate(boolean astar) {
        MotorSimulacion m = motor("HpS\n#CONDICIONES\n1:0:1 DANADO PELIGROSO REPARABLE\n#PERSONAS\n0 0 RESCATE SANO", astar);
        m.paso();
        m.paso();
        exigir(m.mapa.n[0][1].bloqueado, "El simulacro no repara bloqueos");
        m.params.desastre = TipoDesastre.TERREMOTO;
        m.iniciar();
        m.tick = m.emergencia().ticksTemblorRestantes(0);
        Nodo n = m.mapa.n[0][1];
        n.bloqueoReparable = false;
        m.paso();
        exigir(n.bloqueado, "Un tick de reparación no basta");
        m.paso();
        exigir(!n.bloqueado && n.danado && n.peligroso && n.capacidadEfectiva(2) == 1, "Repara solo el bloqueo");
        m = motor("HpS\n#PERSONAS\n0 0 RESCATE SANO", astar);
        m.paso();
        exigir(m.mapa.n[0][1].bloqueado, "Un bloqueo sin marca reparable se conserva");
    }

    public static void main(String[] args) {
        for (boolean astar : new boolean[]{false, true}) {
            costos(astar);
            reevaluacion(astar);
            rescate(astar);
            capacidadSimultanea(astar);
        }
        System.out.println("Rutas dinámicas: TODAS LAS PRUEBAS OK");
    }
}
