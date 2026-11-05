package evacua;

import evacua.edificio.*;
import evacua.importacion.ParserPlano;
import evacua.persistencia.*;
import evacua.reportes.*;
import evacua.simulacion.*;

import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Pruebas de base de datos, comparacion y exportacion. Ejecutar desde la raiz: java -cp out evacua.PruebasPersistencia */
public class PruebasPersistencia {
    static int fallos = 0;

    static void chk(boolean ok, String msg) {
        if (!ok) {
            fallos++;
            System.out.println("FALLO: " + msg);
        }
    }

    static Edificio leer(String f) throws Exception {
        return ParserPlano.leer(new String(Files.readAllBytes(Paths.get("recursos/planos/" + f))), f);
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("evacua_bd");
        BaseDatos bd = new BaseDatos(tmp.toString());
        Edificio demo = leer("edificio_demo.txt"), grafo = leer("edificio_simple.json");
        demo.n[5][5].bloqueado = true;
        bd.guardar("demo", demo);
        bd.guardar("simple", grafo);
        chk(bd.listar().equals(Arrays.asList("demo", "simple")), "listar: " + bd.listar());
        Edificio d2 = bd.cargar("demo"), g2 = bd.cargar("simple");
        chk(GestorEscenarios.aTexto(d2, true).equals(GestorEscenarios.aTexto(demo, true)), "ida y vuelta del escenario cuadricula (bloqueos y personas)");
        chk(!d2.conexionesExplicitas && g2.conexionesExplicitas && g2.conexiones.size() == 6 && g2.personas.size() == 2, "ida y vuelta del escenario con grafo");
        chk(GestorEscenarios.aJson(g2).equals(GestorEscenarios.aJson(grafo)), "JSON estable tras guardar y cargar");

        // RN-10
        Edificio sinSalida = new Edificio(2, 2);
        sinSalida.crearNodo(TipoNodo.HABITACION, 0, 0, null);
        String msg = "";
        try {
            bd.guardar("malo", sinSalida);
        } catch (IllegalArgumentException ex) {
            msg = ex.getMessage();
        }
        chk(msg.contains("salida") && !bd.listar().contains("malo"), "RN-10 rechaza sin salida y no escribe");
        Edificio persona = leer("edificio_demo.txt");
        persona.n[persona.personas.get(0).f][persona.personas.get(0).c].tipo = TipoNodo.VACIO;
        chk(!GestorEscenarios.validarParaGuardar(persona).isEmpty(), "RN-10 rechaza persona fuera de un nodo valido");
        boolean nombre = false;
        try {
            bd.guardar("../x", demo);
        } catch (IllegalArgumentException ex) {
            nombre = true;
        }
        chk(nombre, "nombre de escenario con ruta se rechaza");

        // Comparacion y resultados
        ParametrosSimulacion p = new ParametrosSimulacion();
        List<ComparadorEscenarios.Fila> filas = ComparadorEscenarios.compararGuardados(bd, Arrays.asList("demo", "simple"), p);
        chk(filas.size() == 2 && filas.get(0).personas == 6 && filas.get(1).personas == 2, "comparador simula ambos escenarios");
        chk(bd.resultados().size() == 3 && bd.resultados().get(0).equals(BaseDatos.CABECERA), "tabla de resultados con cabecera y 2 filas");
        chk(ComparadorEscenarios.aTexto(filas).contains("simple"), "texto comparativo");

        // Exportacion individual (RN-14)
        MotorSimulacion m = new MotorSimulacion(demo.copia(), p, s -> { });
        m.iniciar();
        while (m.paso()) { }
        File dir = new File(tmp.toFile(), "reportes");
        chk(ExportadorIndividual.exportarTodos(m, dir) == 6 && dir.listFiles().length == 6, "un archivo por persona");
        String txt = new String(Files.readAllBytes(new File(dir, "reporte_P1.txt").toPath()));
        chk(txt.contains("Bitacora de recorrido") && txt.contains("Linea de tiempo"), "el reporte incluye bitacora y linea de tiempo");

        System.out.println(fallos == 0 ? "Persistencia/reportes: TODAS LAS PRUEBAS OK" : "Persistencia/reportes: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
