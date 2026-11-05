package evacua;

import evacua.edificio.*;
import evacua.importacion.ParserPlano;
import evacua.rutas.*;
import evacua.simulacion.*;

import java.nio.file.*;
import java.util.*;

/** Pruebas de importacion, grafo y rutas. Ejecutar desde la raiz del repo: java -cp out evacua.PruebasPlanos */
public class PruebasPlanos {
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
        Edificio j = leer("edificio_simple.json"), x = leer("edificio_simple.xml");
        chk(j.filas == 3 && j.cols == 6 && j.conexiones.size() == 6 && j.personas.size() == 2, "JSON: dimensiones/conexiones/personas");
        chk(x.filas == j.filas && x.cols == j.cols && x.conexiones.size() == j.conexiones.size(), "XML equivale al JSON");
        chk(j.conexionesExplicitas, "plano importado usa conexiones explicitas");

        // RN-15: plano invalido -> mensaje con todos los errores
        String msg = "";
        try {
            leer("invalido.json");
        } catch (IllegalArgumentException ex) {
            msg = ex.getMessage();
        }
        chk(msg.contains("repetido") && msg.contains("tipo invalido") && msg.contains("destino 'Z'") && msg.contains("mayor a cero") && msg.contains("al menos una salida"), "RN-15 informa todos los errores: " + msg);
        boolean sint = false;
        try {
            ParserPlano.leer("{ \"nodos\": [ ", "x.json");
        } catch (IllegalArgumentException ex) {
            sint = ex.getMessage().startsWith("JSON invalido");
        }
        chk(sint, "JSON mal formado se rechaza");

        // Rutas: H1 -> S1. Camino: H1-P1-P2-E1-S1 = 1+1+3+1 = 6; ventana H2-S1 cuesta 5 (H1-P1-H2-S1 = 7)
        List<int[]> metas = j.salidas();
        ResultadoRuta d = BuscadorRutas.buscar(j, 1, 1, metas, false), a = BuscadorRutas.buscar(j, 1, 1, metas, true);
        chk(d.ruta != null && d.costo == 6 && a.costo == 6, "Dijkstra y A* encuentran costo 6: " + d.costo + "/" + a.costo);
        chk(a.explorados <= d.explorados, "A* explora <= nodos que Dijkstra");
        j.n[1][3].bloqueado = true; // bloquear P2
        ResultadoRuta b = BuscadorRutas.buscar(j, 1, 1, metas, true);
        chk(b.ruta != null && b.costo == 7, "Bloqueo de P2: la ruta alterna cuesta 7 (por la ventana): " + b.costo);
        j.n[1][3].bloqueado = false;
        j.bloquearConexion(1, 2, 2, 2, true); // P1-H2
        j.n[1][3].bloqueado = true;
        chk(BuscadorRutas.buscar(j, 1, 1, metas, true).ruta == null, "Sin camino cuando se bloquean nodo y conexion");

        // A* admisible en la cuadricula demo: mismo costo que Dijkstra desde cada celda caminable
        Edificio demo = leer("edificio_demo.txt");
        boolean igual = true;
        for (int f = 0; f < demo.filas; f++)
            for (int c = 0; c < demo.cols; c++)
                if (demo.caminable(f, c)) {
                    ResultadoRuta r1 = BuscadorRutas.buscar(demo, f, c, demo.salidas(), false), r2 = BuscadorRutas.buscar(demo, f, c, demo.salidas(), true);
                    igual &= (r1.ruta == null) == (r2.ruta == null) && Math.abs(r1.costo - r2.costo) < 1e-9;
                }
        chk(igual, "A* admisible: mismo costo que Dijkstra en toda la cuadricula");

        // CRUD
        Edificio e = new Edificio(3, 3);
        e.crearNodo(TipoNodo.HABITACION, 0, 0, "A");
        e.crearNodo(TipoNodo.SALIDA, 0, 2, "S");
        boolean dup = false;
        try {
            e.crearNodo(TipoNodo.PASILLO, 1, 1, "A");
        } catch (IllegalArgumentException ex) {
            dup = true;
        }
        chk(dup, "RN-01 identificador repetido se rechaza");
        e.crearNodo(TipoNodo.PASILLO, 0, 1, "P");
        e.conectar(0, 0, 0, 1, 2, 3);
        e.conectar(0, 1, 0, 2, 2, 3);
        chk(BuscadorRutas.buscar(e, 0, 0, e.salidas(), true).costo == 4, "conectar: costo usa el tiempo de la conexion");
        boolean malo = false;
        try {
            e.conectar(0, 0, 0, 1, 0, 3);
        } catch (IllegalArgumentException ex) {
            malo = true;
        }
        chk(malo, "RN-03 tiempo 0 se rechaza");
        e.eliminarNodo(0, 1);
        chk(e.conexiones.isEmpty() && BuscadorRutas.buscar(e, 0, 0, e.salidas(), true).ruta == null, "eliminarNodo quita conexiones");

        // Simulacion sobre plano importado
        Edificio s = leer("edificio_simple.xml");
        MotorSimulacion m = new MotorSimulacion(s, new ParametrosSimulacion(), t -> { });
        m.iniciar();
        while (m.paso()) { }
        chk(m.evacuadas() == 2, "simulacion del plano XML evacua a las 2 personas: " + m.evacuadas());
        chk(s.personas.get(0).bitacora.tamano() > 1, "la bitacora registra el recorrido");

        System.out.println(fallos == 0 ? "Planos/rutas: TODAS LAS PRUEBAS OK" : "Planos/rutas: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
