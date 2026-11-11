package evacua;

import evacua.edificio.Edificio;
import evacua.estructuras.Cola;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.Persona;
import evacua.simulacion.*;

public class PruebasCongestion {
    static int fallos = 0;

    static void chk(boolean ok, String msg) {
        if (!ok) {
            fallos++;
            System.out.println("FALLO: " + msg);
        }
    }

    public static void main(String[] args) {

        String plano = ".PPP.\nHP.PS\n.P.P.\n.PPP.\n#PERSONAS\n1 0 ADULTO SANO\n0 2 ADULTO INCAPACITADO\n";
        Edificio e = GestorEscenarios.deTexto(plano);
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.capacidad = 1;
        p.prob = 0;
        MotorSimulacion m = new MotorSimulacion(e, p, s -> { });
        m.iniciar();
        Persona camina = e.personas.get(0);
        boolean ocupadaEnPrimerTick = false;
        boolean libreDespues = false;
        while (m.paso()) {
            if (m.tick == 1) ocupadaEnPrimerTick = e.n[1][0].ocupacion + e.n[1][1].ocupacion >= 1;
            if (camina.f != 1 || camina.c != 0) libreDespues |= e.n[1][0].ocupacion == 0;
        }
        chk(camina.evacuada, "la persona rodea la congestion y evacua");
        chk(camina.historial.recalculos >= 1, "se cuenta el recalculo por congestion: " + camina.historial.recalculos);
        chk(camina.historial.esperaCuelloBotella >= p.esperaRecalculo, "espera antes de rodear: " + camina.historial.esperaCuelloBotella);
        boolean dice = false;
        for (String h : camina.hist) dice |= h.contains("congestion") && h.contains("otro camino");
        chk(dice, "la linea de tiempo registra el desvio por congestion");
        chk(!e.personas.get(1).evacuada, "la persona incapacitada sin asistencia no sale");
        chk(m.evacuadas() == 1 && m.salidas.tamano() == 1, "una sola salida en la cola");

        Edificio e2 = GestorEscenarios.deTexto("HPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 2 ADULTO INCAPACITADO\n");
        ParametrosSimulacion p2 = new ParametrosSimulacion();
        p2.capacidad = 1;
        p2.prob = 0;
        p2.limite = 40;
        MotorSimulacion m2 = new MotorSimulacion(e2, p2, s -> { });
        m2.iniciar();
        while (m2.paso()) { }
        chk(!e2.personas.get(0).evacuada && e2.personas.get(0).historial.recalculos == 0, "sin camino alterno: espera hasta el limite, sin recalcular");

        chk(ocupadaEnPrimerTick && libreDespues, "la ocupacion del nodo inicial queda en 0 cuando la persona se va");

        Cola<Integer> c = new Cola<>();
        c.encolar(1);
        c.encolar(2);
        int suma = 0;
        for (int x : c) suma = suma * 10 + x;
        chk(suma == 12 && c.tamano() == 2, "la cola se recorre sin sacar nada");

        System.out.println(fallos == 0 ? "Congestion: TODAS LAS PRUEBAS OK" : "Congestion: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
