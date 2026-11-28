package evacua;

import evacua.edificio.*;
import evacua.persistencia.*;
import evacua.reportes.*;
import evacua.simulacion.*;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class PruebasPersistencia {
    static int fallos = 0;

    static void chk(boolean ok, String msg) {
        if (!ok) {
            fallos++;
            System.out.println("FALLO: " + msg);
        }
    }

    static Edificio leer(String f) throws Exception {
        return GestorEscenarios.leer(new File("recursos/planos/" + f));
    }

    static void probarFormatoAmpliado(Path tmp) throws Exception {
        Edificio original = GestorEscenarios.deTexto("#PISO 1\nHP\n#PISO 2\nES\n#PERSONAS\n1 0 0 ADULTO SANO");
        original.n[0][0].danado = true;
        original.n[1][0].peligroso = true;
        original.n[1][0].danado = true;
        original.n[1][0].bloqueoReparable = true;
        original.n[1][0].bloqueado = true;
        Conexion enlace = original.conectar(0, 1, 1, 1, 2.75, 4);
        enlace.condicion = Condicion.PELIGROSO;
        enlace.bloqueada = true;
        File archivo = tmp.resolve("dos-pisos.txt").toFile();
        for (boolean explicito : new boolean[]{false, true}) {
            original.conexionesExplicitas = explicito;
            GestorEscenarios.guardar(archivo, original);
            Edificio copia = GestorEscenarios.leer(archivo);
            chk(copia.n[0][0].danado && copia.personas.size() == 1
                    && copia.n[1][0].peligroso && copia.n[1][0].danado && copia.n[1][0].bloqueoReparable && copia.n[1][0].bloqueado,
                    "Condiciones y bloqueo de nodos entre pisos");
            Conexion k = copia.conexiones.get(0);
            chk(k.origen == enlace.origen && k.destino == enlace.destino && k.tiempo == 2.75
                    && k.capacidad == 4 && k.bloqueada && k.condicion == Condicion.PELIGROSO,
                    "Extremos y atributos del enlace");
            chk((Grafo.desde(copia).arista(0, 1) == null) == explicito, "Adyacencia según el modo guardado");
        }
        String prefijo = "HS.\n#CONEXIONES\n";
        for (String datos : new String[]{"1:0:0 1:0:2 1 1 NORMAL ABIERTA",
                "1:0:0 2:0:1 1 1 NORMAL ABIERTA", "1:0:0 1:0:1 NaN 2 NORMAL ABIERTA",
                "1:0:0 1:0:1 1 0 NORMAL ABIERTA", "1:0:0 1:0:1 1 2 NORMAL X",
                "1:0:0 1:0:1 1 2 NORMAL ABIERTA\n1:0:1 1:0:0 2 3 DANADO BLOQUEADA"}) {
            try {
                GestorEscenarios.deTexto(prefijo + datos);
                chk(false, "Se aceptó una conexión inválida: " + datos);
            } catch (IllegalArgumentException ex) {
                chk(ex.getMessage().contains("#CONEXIONES"), "Mensaje de conexión inválida");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("evacua_bd");
        probarFormatoAmpliado(tmp);
        Edificio grid = leer("edificio.txt");
        Edificio persona = leer("edificio.txt");
        persona.n[persona.personas.get(0).f][persona.personas.get(0).c].tipo = TipoNodo.VACIO;
        chk(!GestorEscenarios.validarParaGuardar(persona).isEmpty(), "RN-10 rechaza persona fuera de un nodo valido");
        ParametrosSimulacion p = new ParametrosSimulacion();
        MotorSimulacion m = new MotorSimulacion(grid.copia(), p, s -> { });
        m.iniciar();
        while (m.paso()) { }
        File dir = new File(tmp.toFile(), "reportes");
        chk(ExportadorIndividual.exportarTodos(m, dir) == 9 && dir.listFiles().length == 9, "un archivo por persona");
        String txt = new String(Files.readAllBytes(new File(dir, "reporte_P1.txt").toPath()));
        chk(txt.contains("Bitacora de recorrido") && txt.contains("Linea de tiempo"), "el reporte incluye bitacora y linea de tiempo");

        System.out.println(fallos == 0 ? "Persistencia/reportes: TODAS LAS PRUEBAS OK" : "Persistencia/reportes: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
