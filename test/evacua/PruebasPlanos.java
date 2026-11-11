package evacua;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.rutas.*;
import evacua.simulacion.*;

import java.nio.file.*;
import java.util.*;

public class PruebasPlanos {
    static int fallos = 0;

    static void chk(boolean ok, String msg) {
        if (!ok) {
            fallos++;
            System.out.println("FALLO: " + msg);
        }
    }

    static Edificio leer(String f) throws Exception {
        return GestorEscenarios.leer(new java.io.File("recursos/planos/" + f));
    }

    static Edificio grafoPrueba() {
        Edificio j = new Edificio(3, 6);
        j.crearNodo(TipoNodo.HABITACION, 1, 1, "H1");
        j.crearNodo(TipoNodo.PASILLO, 1, 2, "P1");
        j.crearNodo(TipoNodo.PASILLO, 1, 3, "P2");
        j.crearNodo(TipoNodo.ESCALERA, 1, 4, "E1");
        j.crearNodo(TipoNodo.SALIDA, 1, 5, "S1");
        j.crearNodo(TipoNodo.HABITACION, 2, 2, "H2");
        j.conectar(1, 1, 1, 2, 1, 3);
        j.conectar(1, 2, 1, 3, 1, 5);
        j.conectar(1, 3, 1, 4, 3, 2);
        j.conectar(1, 4, 1, 5, 1, 4);
        j.conectar(1, 2, 2, 2, 1, 3);
        j.conectar(2, 2, 1, 5, 5, 2);
        j.conexionesExplicitas = true;
        j.agregar(evacua.personas.TipoPersona.ADULTO, evacua.personas.EstadoSalud.SANO, 1, 1);
        j.agregar(evacua.personas.TipoPersona.NINO, evacua.personas.EstadoSalud.SANO, 2, 2);
        return j;
    }

    public static void main(String[] args) throws Exception {
        Edificio j = grafoPrueba();
        chk(j.filas == 3 && j.cols == 6 && j.conexiones.size() == 6 && j.personas.size() == 2, "grafo de prueba: dimensiones/conexiones/personas");
        chk(j.conexionesExplicitas, "el grafo de prueba usa conexiones explicitas");

        String[] malos = {"HPP\n", "HPS\nHP\n", "HPX\n", "HPS\n#PERSONAS\n0 0 ADULTO\n", "HPS\n#PERSONAS\n0 5 ADULTO SANO\n"};
        for (String m : malos) {
            boolean rechazado = false;
            try {
                GestorEscenarios.deTexto(m);
            } catch (IllegalArgumentException ex) {
                rechazado = !ex.getMessage().isEmpty();
            }
            chk(rechazado, "plano .txt invalido se rechaza: " + m.replace("\n", "|"));
        }

        List<int[]> metas = j.salidas();
        ResultadoRuta d = BuscadorRutas.buscar(j, 1, 1, metas, false), a = BuscadorRutas.buscar(j, 1, 1, metas, true);
        chk(d.ruta != null && d.costo == 6 && a.costo == 6, "Dijkstra y A* encuentran costo 6: " + d.costo + "/" + a.costo);
        chk(a.explorados <= d.explorados, "A* explora <= nodos que Dijkstra");
        j.n[1][3].bloqueado = true;
        ResultadoRuta b = BuscadorRutas.buscar(j, 1, 1, metas, true);
        chk(b.ruta != null && b.costo == 7, "Bloqueo de P2: la ruta alterna cuesta 7 (por la ventana): " + b.costo);
        j.n[1][3].bloqueado = false;
        j.bloquearConexion(1, 2, 2, 2, true);
        j.n[1][3].bloqueado = true;
        chk(BuscadorRutas.buscar(j, 1, 1, metas, true).ruta == null, "Sin camino cuando se bloquean nodo y conexion");

        Edificio grid = leer("edificio.txt");
        boolean igual = true;
        for (int f = 0; f < grid.filas; f++)
            for (int c = 0; c < grid.cols; c++)
                if (grid.caminable(f, c)) {
                    ResultadoRuta r1 = BuscadorRutas.buscar(grid, f, c, grid.salidas(), false), r2 = BuscadorRutas.buscar(grid, f, c, grid.salidas(), true);
                    igual &= (r1.ruta == null) == (r2.ruta == null) && Math.abs(r1.costo - r2.costo) < 1e-9;
                }
        chk(igual, "A* admisible: mismo costo que Dijkstra en toda la cuadricula");

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

        HistorialEdicion h = new HistorialEdicion();
        Edificio ed = new Edificio(3, 3);
        ed.crearNodo(TipoNodo.HABITACION, 0, 0, null);
        h.guardar(ed);
        ed.crearNodo(TipoNodo.SALIDA, 0, 2, null);
        h.guardar(ed);
        ed.eliminarNodo(0, 0);
        Edificio u1 = h.deshacer();
        chk(u1.n[0][0].tipo == TipoNodo.HABITACION && u1.n[0][2].tipo == TipoNodo.SALIDA, "deshacer 1: vuelve el nodo borrado");
        Edificio u2 = h.deshacer();
        chk(u2.n[0][0].tipo == TipoNodo.HABITACION && u2.n[0][2].tipo == TipoNodo.VACIO, "deshacer 2: vuelve a antes de crear la salida");
        chk(h.deshacer() == null && !h.puedeDeshacer(), "sin mas cambios que deshacer");
        u2.n[0][1].tipo = TipoNodo.PASILLO;
        chk(u1.n[0][1].tipo == TipoNodo.VACIO, "las copias del historial son independientes");

        Edificio s = grafoPrueba();
        MotorSimulacion m = new MotorSimulacion(s, new ParametrosSimulacion(), t -> { });
        m.iniciar();
        while (m.paso()) { }
        chk(m.evacuadas() == 2, "la simulacion del grafo evacua a las 2 personas: " + m.evacuadas());
        chk(s.personas.get(0).bitacora.tamano() > 1, "la bitacora registra el recorrido");

        System.out.println(fallos == 0 ? "Planos/rutas: TODAS LAS PRUEBAS OK" : "Planos/rutas: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
