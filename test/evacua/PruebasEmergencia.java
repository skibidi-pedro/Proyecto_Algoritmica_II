package evacua;

import evacua.edificio.*;
import evacua.personas.*;
import evacua.reportes.ReporteSimulacion;
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

    static void configuracion() {
        String plano = "HPS\n#PERSONAS\n0 0 ADULTO INCAPACITADO";
        for (TipoDesastre tipo : TipoDesastre.values()) {
            for (Intensidad nivel : Intensidad.values()) {
                MotorSimulacion m = iniciar(plano, tipo);
                m.params.nivel = nivel;
                m.iniciar();
                int origen = m.emergencia().origen();
                exigir(origen == iniciar(plano, tipo).emergencia().origen(), "Origen reproducible");
                exigir(tipo.requiereOrigen() == (origen >= 0), "Origen según tipo");
                int llegada = tipo == TipoDesastre.TERREMOTO ? new int[]{12,14,15}[nivel.ordinal()] : -1;
                exigir(m.emergencia().tickLlegada() == llegada, "Activación inicial");
                if (tipo.requiereOrigen()) continue; // Cada escenario prueba sus detectores y refuerzos.
                m.mapa.n[0][0].peligroso = true;
                m.paso();
                exigir(m.emergencia().tickLlegada() == llegada, "Estrés ambiental independiente");
                Persona persona = m.mapa.personas.get(0);
                m.estresPorDesastre(persona, 1);
                m.paso();
                m.estresPorDesastre(persona, 1);
                exigir(m.emergencia().tickLlegada() == llegada, "La cuenta empieza una vez");

            }
        }
        MotorSimulacion manual = iniciar("H.S", TipoDesastre.INCENDIO);
        manual.params.origenManual = true;
        manual.params.pisoOrigen = manual.params.filaOrigen = 0;
        manual.params.colOrigen = 2;
        manual.iniciar();
        exigir(manual.emergencia().origen() == 2, "Origen manual");
        manual.params.colOrigen = 1;
        try {
            manual.iniciar();
            throw new AssertionError("Origen vacío aceptado");
        } catch (IllegalArgumentException esperado) { }

    }

    static void mortalidad() {
        MotorSimulacion m = iniciar("HPPS\n#PERSONAS\n0 0 NINO INCAPACITADO\n0 2 RESCATE SANO", TipoDesastre.NINGUNO);
        m.params.prob = 0;
        Persona p = m.mapa.personas.get(0), r = m.mapa.personas.get(1);
        Nodo n = m.mapa.n[0][0];
        n.peligroso = n.fuegoActivo = true;
        n.fijarGas(61);
        m.paso();
        exigir(p.ticksFuego == 1 && p.ticksGas == 1, "Exposiciones simultáneas");
        n.fuegoActivo = false;
        n.fijarGas(60);
        m.paso();
        exigir(p.ticksFuego == 0 && p.ticksGas == 0, "Fin de exposición");
        int f = p.f, c = p.c, t = m.tick;
        double previo = p.estres;
        m.registrarFallecimiento(p, "prueba");
        m.paso();
        exigir(p.tickMuerte == t && !p.evacuada && p.estres == previo && p.f == f && p.c == c, "Estado terminal");
        exigir(r.objetivo != p && p.hist.contains("t=" + t + " FALLECIDA: prueba"), "Rescate e historial");
        exigir(ReporteSimulacion.general(m).contains("Fallecidas: 1") && m.evacuadas() <= 1, "Estadísticas");
    }

    static void compatibilidad() {
        String plano = "HPPPS\nPPPPP\n#PERSONAS\n0 0 ADULTO INCAPACITADO";
        for (TipoDesastre tipo : TipoDesastre.values()) {
            if (tipo == TipoDesastre.TERREMOTO) continue;
            MotorSimulacion sim = iniciar(plano, tipo);
            sim.paso();
            int bloqueados = 0;
            for (Nodo[] fila : sim.mapa.n) for (Nodo n : fila) if (n.bloqueado) bloqueados++;
            exigir(bloqueados == (tipo == TipoDesastre.NINGUNO ? 1 : 0), "Eventos genéricos: " + tipo);
        }
        MotorSimulacion m = iniciar("HS\n#PERSONAS\n0 0 ADULTO SANO\n0 0 ADULTO SANO", TipoDesastre.NINGUNO);
        m.mapa.conectar(0, 0, 0, 1, 1, 1);
        m.grafo = Grafo.desde(m.mapa);
        m.paso();
        exigir(m.evacuadas() == 1, "RN-03: solo una persona cruza por tick");
        m.paso();
        exigir(m.evacuadas() == 2, "El cupo se libera en el siguiente tick");

        Edificio e = GestorEscenarios.deTexto("HPS");
        Conexion k = e.conectar(0, 0, 0, 1, 1, 1);
        k.condicion = Condicion.DANADO;
        for (boolean astar : new boolean[]{true, false}) {
            exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), astar).ruta == null, "RN-05: conexión dañada");
        }
        k.condicion = Condicion.PELIGROSO;
        exigir(BuscadorRutas.buscar(e, 0, 0, e.salidas(), true).costo == 3, "RN-16: coste por peligro");
    }

    // Fuerza un tick sin alteraciones para aislar pruebas de movimiento y capacidad.
    static void pasoTranquilo(MotorSimulacion m) {
        m.rnd.setSeed(0); // Los primeros seis sorteos superan el 22 %.
        m.paso();
    }

    static void intensidades() {
        for (Intensidad nivel : Intensidad.values()) {
            int i = nivel.ordinal(), duracion = new int[]{3, 5, 7}[i];
            double derrumbe = new double[]{0.5, 1.5, 3}[i];
            double dano = new int[]{4, 8, 12}[i], peligro = new int[]{2, 4, 7}[i];
            int estres = new int[]{3, 5, 8}[i];
            // El observador está aislado del conjunto de nodos para medir solo estrés sísmico.
            String plano = "H." + "P".repeat(80) + "S" + "\n#PERSONAS\n0 0 ADULTO INCAPACITADO";
            MotorSimulacion m = iniciar(plano, TipoDesastre.TERREMOTO);
            m.params.nivel = nivel;
            m.iniciar();
            java.util.Random patron = new java.util.Random(m.params.semilla);
            patron.nextInt(4);
            int[] marcas = new int[m.mapa.cols];
            Persona civil = m.mapa.personas.get(0);
            double acumulado = 0;
            for (int t = 1; t <= duracion + 2; t++) {
                if (t <= duracion) for (int c = 0; c < marcas.length; c++) {
                    if (c == 1 || (marcas[c] & 4) != 0) continue;
                    double x = patron.nextDouble() * 100;
                    if (x < derrumbe) marcas[c] |= 4;
                    else if (x < derrumbe + dano) marcas[c] |= 1;
                    else if (x < derrumbe + dano + peligro) marcas[c] |= 2;
                }
                m.paso();
                for (int c = 0; c < marcas.length; c++) {
                    Nodo n = m.mapa.n[0][c];
                    exigir(marcas[c] == ((n.bloqueado ? 4 : 0) | (n.danado ? 1 : 0) | (n.peligroso ? 2 : 0)),
                            "Sorteo y persistencia en " + nivel + ", tick " + t + ", columna " + c);
                }
                if (civil.activa()) acumulado += (t <= duracion ? estres : 0)
                        + ((marcas[0] & 1) != 0 ? 1 : 0) + ((marcas[0] & 2) != 0 ? 4 : 0);
                exigir(civil.estres == Math.min(100, acumulado), "Estrés por intensidad");
                exigir(m.emergencia().ticksTemblorRestantes(t) == Math.max(0, duracion - t), "Duración");
            }
        }
    }

    static void confusionYDerrumbe() {
        MotorSimulacion m = iniciar("HPPPS\n#PERSONAS\n0 0 NINO INCAPACITADO\n0 0 RESCATE SANO\n0 1 ADULTO SANO", TipoDesastre.TERREMOTO);
        Persona paciente = m.mapa.personas.get(0), r = m.mapa.personas.get(1), civil = m.mapa.personas.get(2);
        exigir(paciente.confusion == 3 && civil.confusion == 1 && r.confusion == 0, "Sorteos individuales con semilla 42");
        pasoTranquilo(m);
        exigir(paciente.c == 1 && paciente.asistida && paciente.confusion == 2 && paciente.confundida() && !civil.confundida() && civil.c == 1 && civil.confusion == 0, "Carga e inmovilidad voluntaria");
        exigir(Math.abs(r.estres - 0.9) < 1e-8, "Resistencia al estrés sísmico");
        long semilla = 0;
        while (true) {
            java.util.Random prueba = new java.util.Random(semilla);
            prueba.nextDouble();
            if (prueba.nextDouble() < 0.005) break;
            semilla++;
        }
        m.rnd.setSeed(semilla);
        m.paso();
        for (Persona p : m.mapa.personas)
            exigir(p.tickMuerte == 2 && !p.confundida() && !p.evacuada && p.hist.contains("t=2 FALLECIDA: derrumbe"), "Derrumbe antes del movimiento");
    }

    static void refugiosYRescate() {
        for (boolean astar : new boolean[]{false, true}) {
            MotorSimulacion m = iniciar("HPPS\n#PERSONAS\n0 0 ADULTO SANO", TipoDesastre.TERREMOTO);
            m.params.astar = astar;
            Persona p = m.mapa.personas.get(0);
            m.mapa.bloquear(0, 3, true);
            m.mapa.n[0][0].danado = m.mapa.n[0][1].danado = true;
            int demora = p.confusion;
            for (int t = 0; t < demora; t++) {
                pasoTranquilo(m);
                exigir(p.c == 0, "Ticks completos de confusión");
            }
            for (int t = 0; t < 4; t++) pasoTranquilo(m);
            exigir(p.c == 2 && !p.evacuada && p.pasos == 2, "Refugio seguro estable");
            m.mapa.bloquear(0, 3, false);
            m.paso();
            exigir(p.evacuada, "Retoma evacuación");
            m = iniciar("HE\n#PISO 2\nSE\n#PERSONAS\n0 0 NINO INCAPACITADO\n0 0 RESCATE SANO", TipoDesastre.TERREMOTO);
            m.params.astar = astar;
            m.mapa.bloquear(0, 1, true);
            m.mapa.bloquear(1, 1, true);
            m.mapa.bloquear(1, 0, true);
            exigir(m.evaluarRuta(m.mapa.personas.get(0), m.mapa.salidas(true), astar).ruta == null, "Civil sin acceso");
            pasoTranquilo(m);
            pasoTranquilo(m);
            pasoTranquilo(m);
            exigir(m.evacuadas() == 1 && m.mapa.personas.get(0).f == 1 && m.mapa.n[0][1].bloqueado
                    && m.mapa.n[1][1].bloqueado && m.mapa.n[1][0].bloqueado, "Carga entre pisos por accesos bloqueados");
        }
    }

    static void relevoTerremoto() {
        MotorSimulacion m = iniciar("SH.H\n#PERSONAS\n0 0 RESCATE SANO\n0 3 ADULTO INCAPACITADO", TipoDesastre.TERREMOTO);
        Persona inicial = m.mapa.personas.get(0);
        inicial.aumentarEstres(400);
        pasoTranquilo(m);
        exigir(inicial.fuera() && !inicial.evacuada, "Retiro del rescatista por estrés");
        for (int i = 0; i < 3; i++) pasoTranquilo(m);
        exigir(m.mapa.personas.size() == 2, "Espera cuatro ticks desde el retiro");
        pasoTranquilo(m);
        exigir(m.mapa.personas.size() == 3 && m.mapa.personas.get(2).limiteEstres() == 150,
                "Relevo antes del contingente inicial con mayor resistencia");
    }

    static void refuerzos() {
        for (Intensidad nivel : Intensidad.values()) {
            int i = nivel.ordinal(), llegada = new int[]{12,14,15}[i], cantidad = new int[]{2,4,7}[i];
            MotorSimulacion m = iniciar("H.S\n#PERSONAS\n0 0 NINO INCAPACITADO", TipoDesastre.TERREMOTO);
            m.params.nivel = nivel;
            m.params.capacidad = 1;
            m.params.limite = 9;
            m.iniciar();
            m.mapa.bloquear(0, 2, true);
            for (int t = 1; t < llegada; t++) pasoTranquilo(m);
            exigir(m.mapa.personas.size() == 1 && !m.terminado, "Espera del contingente");
            int admitidos = 0;
            while (admitidos < cantidad) {
                pasoTranquilo(m);
                admitidos = Math.min(cantidad, admitidos + 2);
                exigir(m.mapa.personas.size() == 1 + admitidos && m.emergencia().refuerzosPendientes() == cantidad - admitidos,
                        "Ingreso por capacidad y pendientes en t=" + m.tick);
            }
            pasoTranquilo(m);
            exigir(m.mapa.personas.size() == cantidad + 1, "Sin duplicación");
        }
    }

    public static void main(String[] args) {
        intensidades();
        confusionYDerrumbe();
        refugiosYRescate();
        refuerzos();
        relevoTerremoto();
        configuracion();
        mortalidad();
        compatibilidad();
        System.out.println("Emergencias: TODAS LAS PRUEBAS OK");
    }
}
