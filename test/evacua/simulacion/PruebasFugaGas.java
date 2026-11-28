package evacua.simulacion;

import evacua.edificio.*;
import evacua.personas.*;
import evacua.persistencia.GestorEscenarios;
import java.util.*;

public class PruebasFugaGas {
    static void exigir(boolean ok, String motivo) { if (!ok) throw new AssertionError(motivo); }
    static void igual(double a, double b) { exigir(Math.abs(a-b) < 1e-7, a + " != " + b); }
    static MotorSimulacion iniciar(String plano, TipoGas tipo, Intensidad nivel) {
        ParametrosSimulacion p = new ParametrosSimulacion();
        p.desastre = TipoDesastre.FUGA_DE_GAS;
        p.gas = tipo;
        p.nivel = nivel;
        p.origenManual = true;
        p.pisoOrigen = p.filaOrigen = p.colOrigen = 0;
        MotorSimulacion m = new MotorSimulacion(GestorEscenarios.deTexto(plano),p, x -> {});
        m.iniciar();
        return m;
    }
    static double masa(MotorSimulacion m) {
        double suma = m.emergencia().fuga.pendiente();
        for (Nodo[] fila : m.mapa.n) for (Nodo n : fila) suma += n.concentracionGas();
        return suma;
    }
    static void ambiente(MotorSimulacion m) { m.tick++; m.emergencia().fuga.ambiente(); }

    static void configuracionYPuertas() {
        for (TipoGas tipo : TipoGas.values()) for (Intensidad nivel : Intensidad.values()) {
            MotorSimulacion m = iniciar("HDP.S",tipo,nivel);
            int emision = 10 + 5*nivel.ordinal();
            Nodo a = m.mapa.n[0][0], d = m.mapa.n[0][1], p = m.mapa.n[0][2];
            igual(masa(m),0);
            a.fijarGas(59.99-emision);
            ambiente(m);
            igual(a.concentracionGas(),59.99); igual(d.concentracionGas(),0);
            a.fijarGas(60-emision);
            ambiente(m);
            igual(a.concentracionGas(),45); igual(d.concentracionGas(),15); igual(p.concentracionGas(),0);
            ambiente(m);
            igual(d.concentracionGas(),11.25 + (45 + emision >= 60 ? (45 + emision - 15)*.25 : 0)); igual(p.concentracionGas(),3.75);
            exigir(!d.danado && !d.bloqueado && m.mapa.caminable(0,1), "puerta intacta");
            a.fijarGas(100); d.fijarGas(0); p.fijarGas(0);
            ambiente(m);
            igual(d.concentracionGas(), emision + (100-emision)*.25-emision*.25);
            igual(p.concentracionGas(), emision*.25);
            d.danado = true; a.fijarGas(40-emision); d.fijarGas(0); p.fijarGas(0);
            ambiente(m); igual(d.concentracionGas(),10);
            d.bloqueado = true;
            d.fijarGas(0); a.fijarGas(100);
            ambiente(m); igual(d.concentracionGas(),0); igual(m.emergencia().fuga.pendiente(),emision);
            igual(m.emergencia().fuga.generado(),6*emision);
        }
        for (String invalido : new String[]{".PS","hPS"}) {
            try { iniciar(invalido,TipoGas.GAS_NATURAL,Intensidad.BAJA); throw new AssertionError("origen inválido"); }
            catch (IllegalArgumentException esperado) {}
        }
    }

    static void capasYBalance() {
        MotorSimulacion m = iniciar("HDP.s",TipoGas.GAS_NATURAL,Intensidad.BAJA);
        FugaGas g = m.emergencia().fuga;
        for (int i=0;i<60;i++) { ambiente(m); igual(masa(m),g.generado()-g.ventilado()); }
        igual(g.pendiente(),300);
        m.mapa.n[0][1].bloqueado = true;
        m.mapa.n[0][2].fijarGas(60);
        ambiente(m); igual(g.pendiente(),310); igual(m.mapa.n[0][2].concentracionGas(),60);
        m.mapa.n[0][1].bloqueado = false;
        ambiente(m); igual(g.pendiente(),280); igual(m.mapa.n[0][2].concentracionGas(),100);
        m = iniciar("HE.S\n#PISO 2\n.ESS",TipoGas.MONOXIDO_CARBONO,Intensidad.MEDIA);
        m.mapa.n[1][2].danado = true;
        for (int t=0;t<80;t++) {
            ambiente(m);
            igual(masa(m),m.emergencia().fuga.generado()-m.emergencia().fuga.ventilado());
            for (Nodo[] fila:m.mapa.n) for(Nodo n:fila) exigir(n.concentracionGas() >= 0 && n.concentracionGas() <= 100, "capacidad");
        }
        exigir(m.mapa.n[1][1].concentracionGas()>0 && m.emergencia().fuga.ventilado()>0,"escalera y salida");
        igual(m.mapa.n[0][2].concentracionGas(),0);
        m = iniciar("SHS",TipoGas.DIOXIDO_CARBONO,Intensidad.BAJA);
        m.mapa.n[0][0].danado = true; m.mapa.n[0][2].bloqueado = true;
        for (Nodo n:m.mapa.n[0]) n.fijarGas(50);
        ambiente(m);
        igual(m.mapa.n[0][0].concentracionGas(),52.5); igual(m.mapa.n[0][2].concentracionGas(),50);
        igual(m.emergencia().fuga.ventilado(),5);
        MotorSimulacion a = iniciar("HPP\nPDP\nPPS",TipoGas.GAS_NATURAL,Intensidad.ALTA);
        MotorSimulacion b = iniciar("HPP\nPDP\nPPS",TipoGas.GAS_NATURAL,Intensidad.ALTA);
        for (List<Grafo.Arista> vecinos:b.grafo.ady) Collections.reverse(vecinos);
        for (int t=0;t<100;t++) {
            ambiente(a); ambiente(b);
            for(int f=0;f<3;f++) for(int c=0;c<3;c++) igual(a.mapa.n[f][c].concentracionGas(), b.mapa.n[f][c].concentracionGas());
            igual(masa(a),a.emergencia().fuga.generado()-a.emergencia().fuga.ventilado());
        }
    }

    static void perfiles() {
        int[][] avisos = {{8,5,3,2},{1,1,1,1},{6,4,2,1},{7,5,3,2}};
        int[][] confusion = {{0,7,5,3},{0,0,0,0},{0,0,0,0},{0,8,5,3}};
        int[][] heridas = {{0,0,10,6},{0,0,14,10},{0,0,10,7},{0,0,12,8}};
        int[] muertes = {12,20,14,16};
        double[] bandas = {10.01,50,75,95};
        for (TipoGas tipo : TipoGas.values()) for (Intensidad nivel : Intensidad.values()) for (int j=0;j<4;j++) {
            int i=tipo.ordinal(), estres=new int[]{4,3,4,3}[i]+nivel.ordinal();
            MotorSimulacion m=iniciar("HPS\n#PERSONAS\n0 0 ADULTO SANO",tipo,nivel);
            Persona p=m.mapa.personas.get(0);
            m.mapa.n[0][0].fijarGas(10); m.emergencia().fuga.personas();
            exigir(!p.alertada && p.estres==0,"umbral inferior");
            for (int t=1;t<=21;t++) {
                m.tick++;
                p.c=t%2; m.mapa.n[0][p.c].fijarGas(bandas[j]);
                m.emergencia().fuga.personas();
                m.emergencia().fuga.personas();
                boolean muerto=j==3 && t>=muertes[i];
                boolean herido=heridas[i][j]>0 && t>=heridas[i][j];
                exigir(p.activa()!=muerto && p.alertada==(t>=avisos[i][j]),"muerte/aviso "+tipo+" t="+t);
                exigir(p.confundida()==(!herido && confusion[i][j]>0 && t>=confusion[i][j]),"confusión");
                exigir((p.salud==EstadoSalud.HERIDO)==herido,"herida");
                int fin=muerto?muertes[i]-1:t;
                igual(p.estres,Math.min(100,Math.max(0,fin-avisos[i][j]+1)*estres));
                if (muerto) break;
            }
        }
        MotorSimulacion m=iniciar("HPPPS\n#PERSONAS\n0 0 ADULTO SANO Dep-001\n0 2 ADULTO SANO Dep-001\n0 3 ADULTO SANO Dep-002",TipoGas.MONOXIDO_CARBONO,Intensidad.BAJA);
        Persona p=m.mapa.personas.get(0), familiar=m.mapa.personas.get(1), otro=m.mapa.personas.get(2);
        for(int t=0;t<3;t++){m.tick++;m.mapa.n[0][0].fijarGas(96);m.emergencia().fuga.personas();}
        exigir(p.confundida() && familiar.alertada && !otro.alertada && familiar.estres==0,"alerta local sin estrés");
        int llegada=m.emergencia().tickLlegada();
        m.mapa.n[0][0].fijarGas(54); m.tick++; m.emergencia().fuga.personas();
        exigir(Arrays.equals(p.exposicionGas,new int[]{4,4,0,0}) && !p.confundida(),"reinicio selectivo");
        m.mapa.n[0][0].fijarGas(96);
        for(int t=0;t<6;t++){ m.tick++;m.emergencia().fuga.personas(); }
        double previo=p.estres;
        m.mapa.n[0][0].fijarGas(0); m.tick++; m.emergencia().fuga.personas();
        exigir(p.alertada && p.salud==EstadoSalud.HERIDO && p.estres==previo && m.emergencia().tickLlegada()==llegada,"herida y llamada persistentes");
    }

    static void rutasYRescate() {
        double[][] velocidad={{.9,.8,.7},{.95,.9,.85},{.9,.8,.7},{.9,.85,.75}};
        for(TipoGas tipo:TipoGas.values()) {
            MotorSimulacion m=iniciar("HPS\n#PERSONAS\n0 0 ADULTO SANO",tipo,Intensidad.MEDIA);
            Persona p=m.mapa.personas.get(0);
            Nodo n=m.mapa.n[0][1]; n.danado=true;
            for(int j=0;j<3;j++) {
                double nivel=new double[]{50,75,95}[j]; n.fijarGas(nivel);
                igual(tipo.velocidad(nivel),velocidad[tipo.ordinal()][j]);
                for(boolean astar:new boolean[]{true,false}) {
                    double costo=2*Nodo.costoConcentracion(nivel)+1;
                    p.alertada=false; igual(m.evaluarRuta(p,m.mapa.salidas(),astar).costo,tipo==TipoGas.GAS_NATURAL?costo:3);
                    p.alertada=true; igual(m.evaluarRuta(p,m.mapa.salidas(),astar).costo,costo);
                    Hazmat h=new Hazmat(8,0,0,100);
                    igual(m.evaluarRuta(h,m.mapa.salidas(),astar).costo,3);
                }
            }
        }
        for(boolean especializado:new boolean[]{false,true}) {
            MotorSimulacion m=iniciar("PHPPS\n#PERSONAS\n0 2 ADULTO SANO",TipoGas.MONOXIDO_CARBONO,Intensidad.BAJA);
            Persona adulto=m.mapa.personas.get(0);
            PersonalRescate r=especializado?new Hazmat(88,0,2,100):new PersonalRescate(88,EstadoSalud.SANO,0,2);
            r.alertada=true; m.mapa.personas.add(r);
            if(especializado) m.mapa.personas.add(new Hazmat(89,0,0,100));
            adulto.exposicionGas[1]=7;
            for(Nodo n:m.mapa.n[0]) n.fijarGas(60);
            m.paso();
            exigir(adulto.confundida() && adulto.asistida && adulto.hist.stream().anyMatch(h->h.contains("CARGAR")), "carga de adulto confundido");
            exigir(r.transportando && adulto.c==r.c, "transporte heredado");
        }
    }

    static void personal() {
        for(TipoGas tipo:TipoGas.values()) for(Intensidad nivel:Intensidad.values()) {
            MotorSimulacion m=iniciar("HPS",tipo,nivel);
            m.params.capacidad=10;
            m.emergencia().solicitarRefuerzos(m);
            int llegada=10+nivel.ordinal(); m.tick=llegada-1; m.emergencia().despachar(m);
            exigir(m.mapa.personas.isEmpty(),"llegada exacta");
            m.tick++; m.emergencia().despachar(m); m.emergencia().solicitarRefuerzos(m);
            m.emergencia().despachar(m);
            long hazmat=m.mapa.personas.stream().filter(q->q instanceof Hazmat).count();
            long normales=m.mapa.personas.stream().filter(q->q.getClass()==PersonalRescate.class).count();
            igual(hazmat,tipo==TipoGas.GAS_NATURAL?1:2+2*nivel.ordinal());
            igual(normales,tipo==TipoGas.GAS_NATURAL?new int[]{2,4,7}[nivel.ordinal()]:0);
            igual(m.mapa.personas.size(),normales+hazmat);
            for(Persona q:m.mapa.personas) exigir(q.c==2 && q.alertada,"ingreso por salida");
        }
        MotorSimulacion m=iniciar("HPS",TipoGas.GAS_NATURAL,Intensidad.BAJA);
        Hazmat a=new Hazmat(21,0,0,100), b=new Hazmat(22,0,1,100);
        m.mapa.personas.add(a);m.mapa.personas.add(b);
        FugaGas g=m.emergencia().fuga;
        m.emergencia().solicitarRefuerzos(m);
        g.asignarReparacion();
        exigir(g.encargado(a) && !g.reparar(b),"responsable exclusivo");
        exigir(g.reparar(a) && g.reparar(a) && g.progreso()==1,"un tick de trabajo");
        a.aumentarEstres(400);
        g.asignarReparacion();
        exigir(g.encargado(b) && !g.reparar(b),"reasignación sin reparación remota");
        b.c=0; m.tick++; g.reparar(b);
        exigir(g.reparada() && g.progreso()==2 && !g.reparar(b),"cierre permanente");
        m.mapa.n[0][0].fijarGas(80);
        ambiente(m);
        igual(g.generado(),0); igual(m.mapa.n[0][1].concentracionGas(),20);
        exigir(!m.terminado,"continúa tras el cierre");
        m.mapa.n[0][0].fijarGas(100);
        for(int t=0;t<16;t++){m.tick++;g.personas();}
        exigir(b.activa() && !b.confundida() && b.salud==EstadoSalud.SANO && b.estres==0,"inmunidad HAZMAT");
    }

    static void reemplazosYFin() {
        MotorSimulacion m=iniciar("H.S",TipoGas.MONOXIDO_CARBONO,Intensidad.BAJA);
        m.emergencia().solicitarRefuerzos(m);m.tick=10;m.emergencia().despachar(m);
        Hazmat a=(Hazmat)m.mapa.personas.get(0);
        a.aumentarEstres(400); m.paso(); exigir(a.fuera() && a.retirado,"retiro por estrés");
        int salida=m.tick;
        while(m.tick<salida+3) m.paso();
        igual(m.mapa.personas.size(),2);
        m.paso(); igual(m.mapa.personas.size(),3);
        Hazmat b=(Hazmat)m.mapa.personas.get(2);
        igual(b.estres,0); igual(b.limiteEstres(),150); b.aumentarEstres(400); igual(b.estres,120);
        igual(new Hazmat(90,0,0,550).limiteEstres(),500);
        m=iniciar("HHPS\n#PERSONAS\n0 0 ADULTO SANO Dep-001\n0 1 ADULTO SANO Dep-001",TipoGas.GAS_NATURAL,Intensidad.BAJA);
        int tope=500;
        while(m.tick<tope && m.paso()) {}
        exigir(m.terminado && m.emergencia().fuga.reparada() && m.evacuadas()>0 && m.tick<tope,"simulación completa");
        exigir(m.mapa.personas.stream().noneMatch(Persona::activa),"salida de los supervivientes");
        exigir(masa(m)>0,"gas residual permitido");
        m=iniciar("H.S\n#PERSONAS\n0 2 ADULTO SANO",TipoGas.MONOXIDO_CARBONO,Intensidad.MEDIA);
        exigir(!m.paso() && !m.terminado && m.resultadoEscenario().contains("Sin progreso"),"imposibilidad sin falso éxito");
    }

    static void residualYAvisos() {
        MotorSimulacion m=iniciar("HDPS",TipoGas.DIOXIDO_CARBONO,Intensidad.BAJA);
        Hazmat h=new Hazmat(45,0,0,100);
        m.mapa.personas.add(h);
        FugaGas g=m.emergencia().fuga;
        g.asignarReparacion(); g.reparar(h); m.tick++; g.reparar(h);
        m.mapa.n[0][0].fijarGas(59);
        ambiente(m); igual(m.mapa.n[0][1].concentracionGas(),0);
        m.mapa.n[0][0].fijarGas(100);
        ambiente(m); igual(m.mapa.n[0][1].concentracionGas(),25);
        ambiente(m); igual(m.mapa.n[0][1].concentracionGas(),31.25);
        igual(g.generado(),0);
        m=iniciar("HPPPS",TipoGas.GAS_NATURAL,Intensidad.BAJA);
        g=m.emergencia().fuga;
        m.mapa.bloquear(0,1,true);
        for(int i=0;i<26;i++) ambiente(m);
        igual(g.pendiente(),160);
        h=new Hazmat(46,0,0,100); m.mapa.personas.add(h);
        g.asignarReparacion(); g.reparar(h); m.tick++; g.reparar(h);
        m.mapa.bloquear(0,1,false);
        ambiente(m);
        igual(g.pendiente(),60); igual(masa(m)+g.ventilado(),260);
        ambiente(m);
        igual(g.pendiente(),0); igual(masa(m)+g.ventilado(),260);

        for(boolean especialista:new boolean[]{false,true}) {
            m=iniciar("HPPPS\n#PERSONAS\n0 0 ADULTO SANO Dep-001\n0 1 ADULTO SANO Dep-002",TipoGas.MONOXIDO_CARBONO,Intensidad.BAJA);
            Persona uno=m.mapa.personas.get(0), dos=m.mapa.personas.get(1);
            m.mapa.n[0][0].departamento=1;
            m.mapa.n[0][1].departamento=2;
            Hazmat tecnico=new Hazmat(m.mapa.sigId++,0,0,100);
            m.mapa.personas.add(tecnico); g=m.emergencia().fuga;
            g.asignarReparacion(); g.reparar(tecnico); m.tick++; g.reparar(tecnico);
            g.avisarDesde(tecnico);
            exigir(uno.alertada && !dos.alertada,"aviso restringido al departamento");
            m.mapa.personas.remove(tecnico);
            PersonalRescate r=especialista?new Hazmat(80,0,4,100):new PersonalRescate(80,EstadoSalud.SANO,0,4);
            m.mapa.personas.add(r);r.alertada=true;
            while(m.tick<100 && m.paso()) {}
            exigir(m.terminado && dos.alertada && dos.evacuada && r.fuera(),"visita a departamento pendiente tras el cierre: "+m.tick+" "+m.resultadoEscenario()+" "+dos.hist+" "+r.hist);
        }
    }

    public static void main(String[] args) {
        configuracionYPuertas(); capasYBalance(); perfiles(); rutasYRescate(); personal(); reemplazosYFin(); residualYAvisos();
        System.out.println("Fuga de gas: perfiles, difusión, puertas, balance, rescate, reparación, reemplazos y finalización OK");
    }
}
