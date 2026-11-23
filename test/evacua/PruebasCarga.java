package evacua;

import evacua.persistencia.GestorEscenarios;
import evacua.personas.*;
import evacua.simulacion.*;

public class PruebasCarga {
    static MotorSimulacion motor(String plano, String personas) {
        ParametrosSimulacion params = new ParametrosSimulacion();
        params.prob = 0;
        params.capacidad = 2;
        MotorSimulacion m = new MotorSimulacion(GestorEscenarios.deTexto(plano + "\n#PERSONAS\n" + personas), params, s -> {});
        m.iniciar();
        return m;
    }

    static final String RESCATE = "0 0 RESCATE SANO\n";

    static void exigir(boolean ok, String mensaje) {
        if (!ok) throw new AssertionError(mensaje);
    }

    static void ninos() {
        MotorSimulacion m = motor("HPPS", RESCATE + "0 0 NINO INCAPACITADO");
        Persona r = m.mapa.personas.get(0), n = m.mapa.personas.get(1);
        m.paso();
        exigir(n.asistida && n.c == 1 && r.c == n.c && r.ruta == n.ruta, "Movimiento y ruta comunes");
        exigir(m.mapa.n[0][1].ocupacion == 0.5 && m.mapa.n[0][0].ocupacion == 0, "Capacidad del equipo");
        while (m.paso()) {}
        exigir(n.tEvac == r.tEvac && n.evacuada && n.pasos == r.pasos && m.mapa.n[0][3].ocupacion == 0, "Entrega y registro");
    }

    static void movilidad() {
        MotorSimulacion m = motor("HPS", RESCATE + "0 0 MOVILIDAD INCAPACITADO");
        m.paso();
        exigir(!m.mapa.personas.get(1).asistida, "Un rescatista es insuficiente");
        m = motor("HPS", RESCATE + RESCATE + "0 0 MOVILIDAD INCAPACITADO");
        m.paso();
        Persona lider = m.mapa.personas.get(0);
        exigir(lider.c == 0 && lider.acum == 0.6 && m.mapa.n[0][0].ocupacion == 1, "Media velocidad y una plaza");
        m.paso();
        for (Persona p : m.mapa.personas) exigir(p.c == 1 && p.pasos == 1, "Traslado conjunto");
        while (m.paso()) {}
        exigir(m.evacuadas() == 3, "Entrega coordinada");

        m = motor("HPS", RESCATE + RESCATE + "0 0 MOVILIDAD INCAPACITADO\n0 1 NINO INCAPACITADO");
        m.params.capacidad = 1;
        m.paso();
        m.paso();
        exigir(m.mapa.n[0][0].ocupacion == 1 && m.mapa.n[0][1].ocupacion == 0.5,
                "Media plaza libre no admite al equipo completo");
    }

    static void estres() {
        MotorSimulacion m = motor("HPPS", RESCATE + "0 0 NINO INCAPACITADO\n0 3 NINO INCAPACITADO");
        PersonalRescate r = (PersonalRescate) m.mapa.personas.get(0);
        m.mapa.n[0][0].peligroso = true;
        m.paso();
        exigir(Math.abs(r.estres - 1.2) < 1e-9 && m.mapa.personas.get(1).estres == 4, "Estrés fraccionario individual");
        r.aumentarEstres(400);
        while (m.paso()) {}
        exigir(r.retirado && r.estres == 100 && r.evacuada && m.mapa.personas.get(1).evacuada
                && !m.mapa.personas.get(2).asistida, "Termina la carga y se retira sin otra misión");
    }

    static void rutas() {
        MotorSimulacion m = motor("HPS\nPPP", RESCATE + "0 0 NINO INCAPACITADO");
        m.mapa.n[0][1].peligroso = true;
        Persona r = m.mapa.personas.get(0);
        for (boolean astar : new boolean[]{false,true})
            exigir(m.evaluarRuta(r,m.mapa.salidas(),astar).costo == 2, "Costo de peligro para rescatista");
        m.paso();
        m.mapa.bloquear(0,2,true);
        m.paso();
        exigir(!r.evacuada && r.ruta.isEmpty(), "El equipo espera ante un bloqueo");
        m.mapa.bloquear(0,2,false);
        while (m.paso()) {}
        exigir(m.evacuadas() == 2, "Reevalúa al liberar el destino");
    }

    static void permanencia() {
        for (boolean astar : new boolean[]{true, false}) {
            MotorSimulacion m = motor("H.SPPPH", "0 2 RESCATE SANO\n0 0 NINO INCAPACITADO\n"
                    + "0 5 NINO INCAPACITADO\n0 6 NINO INCAPACITADO");
            m.params.astar = astar;
            Persona r = m.mapa.personas.get(0), primero = m.mapa.personas.get(2), segundo = m.mapa.personas.get(3);
            m.paso();
            exigir(!r.evacuada && r.objetivo == primero, "Sale a buscar al objetivo alcanzable desde la salida");
            while (!primero.evacuada && m.paso()) {}
            exigir(primero.evacuada && !r.evacuada && r.objetivo == segundo, "Después de entregar vuelve a buscar");
            while (m.paso()) {}
            exigir(segundo.evacuada && r.evacuada && !m.mapa.personas.get(1).asistida
                    && r.tEvac == segundo.tEvac && r.tEvac < m.params.limite, "Sale al quedar solo el objetivo aislado");
        }
        MotorSimulacion m = motor("H.SP.H", "0 2 RESCATE SANO\n0 3 MOVILIDAD INCAPACITADO\n0 5 RESCATE SANO");
        m.paso();
        exigir(m.mapa.personas.get(0).evacuada, "Un segundo portador aislado no hace viable el rescate");
        m = motor("SPPH", RESCATE + "0 2 NINO INCAPACITADO");
        PersonalRescate retirado = (PersonalRescate) m.mapa.personas.get(0);
        retirado.aumentarEstres(400);
        m.paso();
        exigir(retirado.evacuada && !m.mapa.personas.get(1).asistida, "Con estrés 100 sale aunque haya un objetivo");
    }

    public static void main(String[] args) {
        ninos();
        movilidad();
        estres();
        rutas();
        permanencia();
        System.out.println("Carga y permanencia: TODAS LAS PRUEBAS OK");
    }
}
