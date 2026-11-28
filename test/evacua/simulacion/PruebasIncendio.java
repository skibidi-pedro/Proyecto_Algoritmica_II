package evacua.simulacion;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.*;
import java.util.*;

public class PruebasIncendio {
    static void verificar(boolean ok, String detalle) { if (!ok) throw new AssertionError(detalle); }
    static void igual(double valor, double esperado) { verificar(Math.abs(valor - esperado) < 1e-8, valor + " != " + esperado); }

    static MotorSimulacion motor(String plano, Intensidad nivel, int columna) {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.desastre = TipoDesastre.INCENDIO;
        p.nivel = nivel;
        p.origenManual = true;
        p.filaOrigen = p.pisoOrigen = 0;
        p.colOrigen = columna;
        MotorSimulacion m = new MotorSimulacion(GestorEscenarios.deTexto(plano), p, s -> {});
        m.iniciar();
        return m;
    }

    static void propagacion() {
        for (Intensidad nivel : Intensidad.values()) {
            int ciclo = 4 - nivel.ordinal();
            for (long semilla = 0; semilla < 32; semilla++) {
                MotorSimulacion m = motor("HE.\n#PISO 2\nPEs", nivel, 1);
                m.rnd.setSeed(semilla);
                Random esperada = new Random(semilla);
                for (int t = 1; t <= ciclo; t++) {
                    esperada.nextInt(5);
                    m.tick = t;
                    m.emergencia().incendio.ambiente();
                    if (t < ciclo) verificar(m.emergencia().incendio.focos == 1, "frecuencia");
                }
                double prob = new double[]{.4,.5,.6}[nivel.ordinal()];
                verificar(m.mapa.n[0][0].fuegoActivo == (esperada.nextDouble() < prob), "sorteo horizontal");
                verificar(m.mapa.n[1][1].fuegoActivo == (esperada.nextDouble() < prob), "sorteo entre pisos");
                verificar(!m.mapa.n[1][0].fuegoActivo && !m.mapa.n[0][2].fuegoActivo, "sin cadena ni vacío");
            }
        }
    }

    static void puertasYExtincion() {
        try { motor("HDS", Intensidad.BAJA, 1); throw new AssertionError("origen"); }
        catch (IllegalArgumentException esperado) {}
        for (Intensidad nivel : Intensidad.values()) {
            MotorSimulacion m = motor("HDH.S", nivel, 0);
            Incendio incendio = m.emergencia().incendio;
            Nodo puerta = incendio.nodos[1], fuente = incendio.nodos[0];
            incendio.nodos[2].fuegoActivo = true;
            incendio.focos++;
            int limite = nivel == Intensidad.ALTA ? 4 : 6;
            for (int t = 1; t <= limite; t++) {
                m.tick = t;
                incendio.ambiente();
                verificar(puerta.calorPuerta == t && puerta.danado == (t == limite), "calor de puerta");
                igual(puerta.humo, 0);
                if (t < limite) verificar(!puerta.fuegoActivo, "puerta intacta");
            }
            m.tick++;
            incendio.ambiente();
            verificar(puerta.humo > 0 && puerta.tipo == TipoNodo.PUERTA, "puerta dañada permeable");
            m.mapa.bloquear(0, 1, true);
            fuente.humo = 99.5;
            incendio.ambiente();
            verificar(!fuente.fuegoActivo && !fuente.protegido && fuente.humo >= 99.5, "saturación aislada");
            fuente.humo = 0;
            puerta.fuegoActivo = true;
            for (int t = 0; t < 120 && !fuente.fuegoActivo; t++) { m.tick++; incendio.ambiente(); }
            verificar(fuente.fuegoActivo, "reincendio natural");
            incendio.apagar(0, true);
            for (int t = 0; t < 40; t++) { m.tick++; incendio.ambiente(); }
            verificar(fuente.protegido && !fuente.fuegoActivo, "protección tras extinción");
        }
    }

    static void humo() {
        for (Intensidad nivel : Intensidad.values()) {
            MotorSimulacion m = motor("H.S", nivel, 0);
            Random esperada = new Random(m.params.semilla);
            double total = 0;
            for (int t = 1; t <= 8; t++) {
                total += 1 + esperada.nextInt(5);
                m.tick = t;
                m.emergencia().incendio.ambiente();
                igual(m.mapa.n[0][0].humo, total);
                verificar(m.mapa.n[0][0].danado == (t >= 8 - 2 * nivel.ordinal()), "daño estructural");
            }
            m.emergencia().incendio.apagar(0, true);
            m.mapa.n[0][2].humo = 22;
            m.emergencia().incendio.ambiente();
            igual(m.mapa.n[0][0].humo, total);
            igual(m.mapa.n[0][2].humo,17);
            m.mapa.bloquear(0,2,true);
            m.emergencia().incendio.ambiente();
            igual(m.mapa.n[0][2].humo,17);
        }
        Edificio e = GestorEscenarios.deTexto("HPPPPPS");
        e.conexionesExplicitas = true;
        for (int j = 1; j < 7; j++) e.conectar(0,0,0,j,1,1);
        Grafo g = Grafo.desde(e);
        double[] dos = {80,20,0,0,0,0,0};
        Difusion.aplicar(g,dos,i -> i <= 1);
        igual(dos[0],65); igual(dos[1],35);
        for (double centro : new double[]{0,100}) {
            double[] v = new double[7];
            Arrays.fill(v,100-centro);
            v[0] = centro;
            double masa = Arrays.stream(v).sum();
            Difusion.aplicar(g,v,i -> true);
            igual(Arrays.stream(v).sum(),masa);
            igual(v[0],100-centro);
            for (double x : v) verificar(x >= 0 && x <= 100, "capacidad");
        }
    }

    static void personasYAlerta() {
        MotorSimulacion m = motor("HPDHH.S\n#PERSONAS\n0 1 ADULTO SANO Dep-001\n0 3 ADULTO SANO Dep-001\n0 4 ADULTO SANO Dep-002", Intensidad.MEDIA, 0);
        Incendio i = m.emergencia().incendio;
        Persona a = m.mapa.personas.get(0), b = m.mapa.personas.get(1), otro = m.mapa.personas.get(2);
        i.nodos[1].humo = 60;
        i.nodos[4].humo = 10;
        m.tick = 1; i.personas();
        verificar(a.alertada && b.alertada && !otro.alertada && !a.confundida(), "alerta por departamento");
        igual(a.estres,4);
        int llegada = m.emergencia().tickLlegada();
        i.nodos[4].humo = 10.001;
        m.tick++; i.personas();
        verificar(otro.alertada && m.emergencia().tickLlegada() == llegada, "umbral y única solicitud");
        a.c = 0;
        i.personas();
        verificar(a.salud == EstadoSalud.HERIDO, "herida");
        a.c = 1;
        i.personas();
        verificar(a.ticksFuego == 0 && a.salud == EstadoSalud.HERIDO, "reinicio fuego");
        i.nodos[1].fuegoActivo = true;
        for (int t = 1; t <= 3; t++) { m.tick++; a.c = t % 2; i.personas(); }
        verificar(!a.activa() && a.ticksFuego == 3, "muerte por fuego continuo entre nodos");
        i.nodos[3].humo = 95;
        for (int t = 1; t <= 7; t++) { m.tick++; i.personas(); }
        verificar(b.salud == EstadoSalud.SANO, "7 ticks");
        i.nodos[3].humo = 94.99;
        i.personas();
        verificar(b.ticksHumo == 0, "reinicio humo");
        i.nodos[3].humo = 95;
        for (int t = 1; t <= 15; t++) {
            m.tick++; i.personas();
            if (t == 8) verificar(b.salud == EstadoSalud.HERIDO, "8 ticks");
        }
        verificar(!b.activa() && b.ticksHumo == 15, "15 ticks totales");
        m = motor("H.DHS\n#PERSONAS\n0 3 ADULTO SANO", Intensidad.ALTA,0);
        m.paso();
        verificar(!m.mapa.personas.get(0).alertada && m.mapa.personas.get(0).pasos == 0 && m.emergencia().tickLlegada() < 0, "sin alarma global");
    }

    static void rutas() {
        MotorSimulacion m = motor("HPS\nPPP\n#PERSONAS\n0 0 ADULTO SANO", Intensidad.BAJA,0);
        Persona a = m.mapa.personas.get(0);
        Bombero b = new Bombero(44,0,0,100);
        for (boolean astar : new boolean[]{false,true}) {
            Nodo n = m.mapa.n[0][1];
            n.fuegoActivo = true;
            igual(m.evaluarRuta(a,m.mapa.salidas(),astar).costo,4);
            igual(m.evaluarRuta(b,m.mapa.salidas(),astar).costo,2);
            n.fuegoActivo = false;
            double[] niveles = {10,10.1,50,75,95}, costos = {1,1.5,2,3,5}, velocidades = {1,1,.9,.8,.7};
            for (int k = 0; k < niveles.length; k++) {
                n.humo = niveles[k];
                igual(n.costoHumo(),costos[k]); igual(n.velocidadHumo(),velocidades[k]);
                igual(m.evaluarRuta(a,m.mapa.salidas(),astar).costo,Math.min(4,costos[k]+1));
                igual(m.evaluarRuta(b,m.mapa.salidas(),astar).costo,2);
            }
        }
        m.params.umbral = 100;
        m.paso(); m.paso();
        verificar(a.activa() && a.pasos > 0 && a.salud == EstadoSalud.HERIDO, "sale del foco con velocidad fraccionaria");
    }

    static void brigada() {
        MotorSimulacion m = motor("HPPPS\n#PERSONAS\n0 3 ADULTO SANO", Intensidad.BAJA,0);
        Bombero b = new Bombero(m.mapa.sigId++,0,1,100);
        Bombero otro = new Bombero(m.mapa.sigId++,0,4,100);
        m.mapa.personas.add(b);
        m.mapa.personas.add(otro);
        Persona civil = m.mapa.personas.get(0);
        m.paso();
        verificar(m.mapa.n[0][0].extincion == 1 && otro.foco < 0, "reserva exclusiva");
        civil.c = 0;
        m.mapa.n[0][0].humo = 95;
        m.paso();
        verificar(civil.hist.stream().anyMatch(h -> h.contains("CARGAR")) && m.mapa.n[0][0].extincion == 1, "interrumpe para rescatar y conserva progreso");
        igual(b.estres,0);
        verificar(b.salud == EstadoSalud.SANO && civil.salud == EstadoSalud.HERIDO, "inmunidad individual");
        for (int t = 0; t < 200 && !m.terminado; t++) m.paso();
        verificar(m.terminado && civil.evacuada && m.mapa.n[0][0].protegido && b.evacuada, "entrega y extinción final");
    }

    static void refuerzos() {
        for (Intensidad nivel : Intensidad.values()) {
            MotorSimulacion m = motor("H.S\n#PERSONAS\n0 0 ADULTO INCAPACITADO", nivel,0);
            m.params.capacidad = 8;
            int llegada = 1 + 10 + nivel.ordinal();
            for (int t = 1; t < llegada; t++) m.paso();
            verificar(m.mapa.personas.size() == 1 && !m.terminado, "espera aun si muere quien detectó");
            m.paso();
            int cantidad = new int[]{2,4,7}[nivel.ordinal()];
            verificar(m.mapa.personas.size() == 1 + cantidad, "contingente " + nivel);
            Set<Integer> ids = new HashSet<>();
            for (int j = 1; j <= cantidad; j++) {
                Persona b = m.mapa.personas.get(j);
                verificar(b instanceof Bombero && b instanceof PersonalRescate && ids.add(b.id) && b.c == 2, "unidades individuales por salida");
            }
            Bombero retirado = (Bombero)m.mapa.personas.get(1);
            retirado.aumentarEstres(1000);
            m.paso();
            verificar(retirado.evacuada, "retirada");
            int tSalida = m.tick;
            while (m.tick < tSalida+4) m.paso();
            verificar(m.mapa.personas.size() == cantidad+2, "solo un reemplazo");
            Bombero reemplazo = (Bombero)m.mapa.personas.get(cantidad+1);
            igual(reemplazo.estres,0); igual(reemplazo.limiteEstres(),150);
            reemplazo.aumentarEstres(400);
            igual(reemplazo.estres,120);
            verificar(!reemplazo.retirado, "supera 100 sin retirarse");
            reemplazo.aumentarEstres(2000);
            igual(reemplazo.estres,150);
            igual(new Bombero(0,0,0,550).limiteEstres(),500);
        }
    }

    static void prioridadesYReservas() {
        MotorSimulacion m = motor("HPPPPPS\n#PERSONAS\n0 0 ADULTO SANO\n0 2 ADULTO SANO\n0 3 ADULTO HERIDO", Intensidad.BAJA, 0);
        Persona fuego = m.mapa.personas.get(0), humo = m.mapa.personas.get(1), herido = m.mapa.personas.get(2);
        m.mapa.n[0][2].humo = 96;
        Bombero b = new Bombero(m.mapa.sigId++,0,4,100);
        m.mapa.personas.add(b);
        Brigada brigada = new Brigada(m);
        brigada.planificar();
        verificar(b.objetivo == fuego, "prioridad 1");
        m.registrarFallecimiento(fuego, "prueba");
        brigada.planificar();
        verificar(b.objetivo == humo, "prioridad 2 y liberación tras muerte");
        m.mapa.n[0][2].humo = 0;
        brigada.planificar();
        verificar(b.objetivo == null && b.foco == 0, "prioridad 3");
        m.emergencia().incendio.apagar(0, true);
        brigada.planificar();
        verificar(b.objetivo == herido && b.foco == -1, "prioridad 4");
        herido.evacuada = true;
        brigada.planificar();
        verificar(b.objetivo == null && b.foco == -1, "liberación tras evacuación");
    }

    static void puertaInterrumpidaYSaturacion() {
        MotorSimulacion m = motor("HDPS", Intensidad.ALTA,0);
        Incendio i = m.emergencia().incendio;
        m.tick = 1; i.ambiente();
        i.apagar(0,false);
        i.ambiente();
        verificar(i.nodos[1].calorPuerta == 0, "exposición térmica consecutiva");
        i.nodos[0].fuegoActivo = true;
        i.focos++;
        i.nodos[1].danado = true;
        i.nodos[0].humo = 100;
        i.nodos[1].humo = 0;
        i.ambiente();
        verificar(i.nodos[0].fuegoActivo, "un vecino no saturado conserva el foco");
        i.nodos[1].bloqueado = true;
        i.nodos[0].humo = 99.5;
        i.ambiente();
        verificar(!i.nodos[0].fuegoActivo && !i.nodos[0].protegido, "barrera excluida de saturación");
    }

    static void cancelacionYAtasco() {
        MotorSimulacion m = motor("S.H", Intensidad.BAJA,0);
        verificar(!m.paso() && !m.terminado && m.resultadoIncendio.contains("Sin progreso"), "atasco distinto de extinción");
        m = motor("H.S", Intensidad.BAJA,0);
        Bombero b = new Bombero(m.mapa.sigId++,0,2,450);
        m.mapa.personas.add(b);
        m.emergencia().solicitarRefuerzos(m);
        b.aumentarEstres(2000);
        m.paso();
        verificar(b.evacuada && b.retirado, "retiro individual");
        m.emergencia().incendio.apagar(0,true);
        m.paso();
        verificar(m.terminado && !m.emergencia().haySolicitudes(), "cancelación de refuerzos y reemplazos");
        m = motor("HPS",Intensidad.BAJA,0);
        b = new Bombero(m.mapa.sigId++,0,2,100);
        m.mapa.personas.add(b);
        m.emergencia().incendio.apagar(0,true);
        m.paso();
        verificar(b.evacuada && !m.emergencia().haySolicitudes(), "evacuación normal sin reemplazo");
    }

    static void finalizacion() {
        MotorSimulacion m = motor("H.S\n#PERSONAS\n0 2 ADULTO SANO", Intensidad.BAJA,0);
        m.mapa.n[0][0].humo = 99.5;
        m.mapa.n[0][2].humo = 25; // Humo residual tras la extinción previa al primer aviso.
        m.paso();
        verificar(m.terminado && m.emergencia().tickLlegada() < 0 && !m.mapa.personas.get(0).evacuada, "fin sin detección");
        m = motor("HPS\n#PERSONAS\n0 1 ADULTO SANO",Intensidad.BAJA,0);
        m.params.limite = 1;
        for (int t=0; t<150 && !m.terminado; t++) m.paso();
        verificar(m.terminado && m.emergencia().incendio.focos == 0 && m.evacuadas() == m.mapa.personas.size(), "continúa tras evacuación civil y límite clásico");
        verificar(m.mapa.n[0][0].humo > 0, "humo residual");
    }

    public static void main(String[] args) {
        propagacion(); puertasYExtincion(); humo(); personasYAlerta(); rutas(); brigada(); refuerzos(); prioridadesYReservas(); puertaInterrumpidaYSaturacion(); cancelacionYAtasco(); finalizacion();
        System.out.println("Incendio: propagación, puertas, humo, personas, alarma, rutas, brigada, extinción, refuerzos y finalización OK");
    }
}
