package evacua;

import evacua.persistencia.GestorEscenarios;
import evacua.personas.Persona;
import evacua.simulacion.*;

public class PruebasEstres {
    static MotorSimulacion iniciar(String plano) {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.capacidad = 1;
        p.prob = 0;
        MotorSimulacion m = new MotorSimulacion(GestorEscenarios.deTexto(plano), p, s -> {});
        m.iniciar();
        return m;
    }

    static void igual(double actual, double esperado) {
        if (actual != esperado) throw new AssertionError("Esperado " + esperado + ", obtenido " + actual);
    }

    static void estados() {
        MotorSimulacion m = iniciar("HPS\n#PERSONAS\n0 0 ADULTO INCAPACITADO");
        Persona p = m.mapa.personas.get(0);
        m.paso();
        igual(p.estres, 0);
        m.mapa.n[0][0].danado = true;
        m.paso();
        igual(p.estres, 1);
        m.mapa.n[0][0].danado = false;
        m.mapa.n[0][0].peligroso = true;
        m.paso();
        igual(p.estres, 5);
        m.mapa.n[0][0].danado = true;
        m.mapa.n[0][1].peligroso = true;
        m.paso();
        igual(p.estres, 10);
    }

    static void congestion() {
        MotorSimulacion m = iniciar("HPS\n#PERSONAS\n0 0 ADULTO SANO\n0 1 ADULTO INCAPACITADO");
        Persona p = m.mapa.personas.get(0);
        m.paso();
        igual(p.estres, 2);
        igual(m.mapa.personas.get(1).estres, 0);
        m.mapa.personas.get(1).evacuada = true;
        m.paso();
        igual(p.c, 1);
        igual(p.estres, 2);
        m = iniciar("HPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 2 ADULTO INCAPACITADO");
        m.paso();
        igual(m.mapa.personas.get(0).estres, 0);
    }

    static void bloqueos() {
        MotorSimulacion m = iniciar("HPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 2 ADULTO HERIDO");
        // La segunda persona empieza en el tramo final y tiene una ruta independiente.
        m.params.capacidad = 2;
        m.recalcularTodas();
        m.mapa.bloquear(0,1,true);
        m.alBloquear(0,1);
        m.recalcularTodas();
        m.paso();
        igual(m.mapa.personas.get(0).estres, 3);
        igual(m.mapa.personas.get(1).estres, 0);
        m.mapa.bloquear(0,1,false);
        m.recalcularTodas();
        m.mapa.bloquear(0,1,true);
        m.alBloquear(0,1);
        igual(m.mapa.personas.get(0).estres, 6);
    }

    static void acumulacion() {
        MotorSimulacion m = iniciar("#PISO 1\nHPP\nPPS\n#PISO 2\nPPP\nPPS\n#PERSONAS\n0 0 ADULTO INCAPACITADO");
        Persona p = m.mapa.personas.get(0);
        m.mapa.n[2][0].peligroso = m.mapa.n[1][1].peligroso = true;
        m.paso();
        igual(p.estres, 0);
        m.mapa.n[0][1].peligroso = m.mapa.n[1][0].peligroso = true;
        m.paso();
        igual(p.estres, 1);
        m = iniciar("HPS\n#PERSONAS\n0 0 ADULTO SANO\n0 1 ADULTO INCAPACITADO");
        p = m.mapa.personas.get(0);
        m.mapa.n[0][0].danado = m.mapa.n[0][0].peligroso = true;
        p.acum = 1;
        m.paso();
        igual(p.estres, 7);
        p.estres = 98;
        m.paso();
        igual(p.estres, 100);
        m.mapa.n[0][0].danado = m.mapa.n[0][0].peligroso = false;
        m.mapa.personas.get(1).evacuada = true;
        while (m.paso()) {}
        igual(p.estres, 100);
        if (!p.evacuada) throw new AssertionError("Debe evacuar");

        m = iniciar("HPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 0 ADULTO INCAPACITADO");
        p = m.mapa.personas.get(0);
        p.acum = 3;
        m.mapa.n[0][0].danado = true;
        m.mapa.n[0][1].peligroso = true;
        m.paso();
        if (!p.evacuada) throw new AssertionError("Debe completar varios movimientos");
        igual(p.estres, 2);
        m.mapa.n[0][3].peligroso = true;
        m.paso();
        igual(p.estres, 2);
    }

    public static void main(String[] args) {
        estados();
        congestion();
        bloqueos();
        acumulacion();
        System.out.println("Estrés ambiental: cuatro escenarios OK");
    }
}
