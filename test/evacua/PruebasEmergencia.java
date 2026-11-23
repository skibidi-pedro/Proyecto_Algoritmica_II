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
                m.mapa.n[0][0].peligroso = true;
                m.paso();
                exigir(m.emergencia().tickLlegada() == llegada, "Estrés ambiental independiente");
                Persona persona = m.mapa.personas.get(0);
                m.estresPorDesastre(persona, 1);
                if (tipo.requiereOrigen()) llegada = 1 + new int[]{10,11,12}[nivel.ordinal()];
                m.paso();
                m.estresPorDesastre(persona, 1);
                exigir(m.emergencia().tickLlegada() == llegada, "La cuenta empieza una vez");
                if (llegada >= 0) {
                    while (m.tick < llegada) m.paso();
                    exigir(m.mapa.personas.size() == 1, "Sin generación automática");
                    PersonalRescate r = m.incorporarRefuerzo(0, 2, id -> new PersonalRescate(id, EstadoSalud.SANO, 0, 0));
                    exigir(r.c == 2 && m.mapa.personas.contains(r), "Ingreso por la salida");
                }
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

        MotorSimulacion m = iniciar("HPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 1 ADULTO SANO\n0 2 RESCATE SANO", TipoDesastre.TERREMOTO);
        Persona a = m.mapa.personas.get(0), b = m.mapa.personas.get(1), r = m.mapa.personas.get(2);
        exigir(a.confusion == 0, "Confusión pendiente de activación");
        m.iniciarConfusion();
        int duracion = a.confusion, otra = b.confusion;
        exigir(duracion >= 3 && duracion <= 5 && otra >= 3 && otra <= 5 && r.confusion == 0, "Duraciones y excepción profesional");
        m.iniciarConfusion();
        m.mapa.n[0][0].danado = true;
        m.paso();
        exigir(a.confusion == duracion - 1 && b.confusion == otra - 1 && a.c == 0 && a.estres == 1, "Inmovilidad y estrés");
        for (int i = 1; i < duracion; i++) m.paso();
        exigir(a.confusion == 0 && a.c == 0, "Duración exacta");
        m.paso();
        exigir(a.c > 0, "Fin de la confusión");
    }

    static void mortalidad() {
        MotorSimulacion m = iniciar("HPPS\n#PERSONAS\n0 0 NINO INCAPACITADO\n0 2 RESCATE SANO", TipoDesastre.INCENDIO);
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

    public static void main(String[] args) {
        configuracion();
        mortalidad();
        compatibilidad();
        System.out.println("Emergencias: TODAS LAS PRUEBAS OK");
    }
}
