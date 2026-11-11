package evacua;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.Persona;
import evacua.rutas.*;
import evacua.simulacion.*;

import java.nio.file.*;
import java.util.*;

/** Pruebas de pisos, escaleras y recalculo al bloquear. Ejecutar desde la raiz: java -cp out evacua.PruebasPisos */
public class PruebasPisos {
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

    public static void main(String[] args) throws Exception {
        Edificio e = leer("edificio.txt");
        chk(e.pisos == 3 && e.filasPorPiso == 12 && e.filas == 36 && e.personas.size() == 9, "3 pisos de 12 filas, 9 personas");
        chk(e.piso(e.personas.get(6).f) == 1 && e.piso(e.personas.get(8).f) == 2, "personas ubicadas en los pisos 2 y 3");

        // Desde el piso 3 hay ruta a la salida del piso 1, pasando por la escalera de cada piso
        Persona p3 = e.personas.get(8);
        ResultadoRuta r = BuscadorRutas.buscar(e, p3.f, p3.c, e.salidas(), true);
        boolean pasa2 = false, pasa1 = false;
        for (int[] q : r.ruta) {
            if (e.piso(q[0]) == 1) pasa2 = true;
            if (e.piso(q[0]) == 0) pasa1 = true;
        }
        chk(r.ruta != null && pasa2 && pasa1, "la ruta del piso 3 baja por el piso 2 hasta el piso 1");
        int[] fin = r.ruta.get(r.ruta.size() - 1);
        chk(e.n[fin[0]][fin[1]].tipo == TipoNodo.SALIDA && e.piso(fin[0]) == 0, "la ruta termina en una salida del piso 1");

        // La fila final de un piso NO es vecina de la primera del siguiente
        Edificio s = new Edificio(2, 2, 2);
        s.crearNodo(TipoNodo.HABITACION, 1, 0, null);   // ultima fila del piso 1
        s.crearNodo(TipoNodo.SALIDA, 2, 0, null);       // primera fila del piso 2 (sin escalera)
        chk(BuscadorRutas.buscar(s, 1, 0, s.salidas(), false).ruta == null, "sin escalera no hay paso entre pisos");

        // A* admisible: mismo costo que Dijkstra desde cada nodo de todos los pisos
        boolean igual = true;
        for (int f = 0; f < e.filas; f++)
            for (int c = 0; c < e.cols; c++)
                if (e.caminable(f, c)) {
                    ResultadoRuta a = BuscadorRutas.buscar(e, f, c, e.salidas(), false), b = BuscadorRutas.buscar(e, f, c, e.salidas(), true);
                    igual &= (a.ruta == null) == (b.ruta == null) && Math.abs(a.costo - b.costo) < 1e-9;
                }
        chk(igual, "A* admisible entre pisos");

        // Ida y vuelta en texto
        Edificio t2 = GestorEscenarios.deTexto(GestorEscenarios.aTexto(e, true));
        chk(GestorEscenarios.aTexto(t2, true).equals(GestorEscenarios.aTexto(e, true)) && t2.pisos == 3, "ida y vuelta .txt con pisos");

        // Agregar un piso conserva todo
        Edificio e4 = e.copiaConPisos(4);
        chk(e4.pisos == 4 && e4.personas.size() == 9 && e4.n[5][10].tipo == TipoNodo.ESCALERA && e4.n[36][0].tipo == TipoNodo.VACIO, "copiaConPisos agrega un piso vacio");

        // Simulacion completa: evacuan los 9
        MotorSimulacion m = new MotorSimulacion(e.copia(), new ParametrosSimulacion(), x -> { });
        m.iniciar();
        while (m.paso()) { }
        chk(m.evacuadas() == 9, "evacuan las 9 personas de los 3 pisos: " + m.evacuadas());

        // Cola de salidas -> CSV con tiempo y estado de quienes salieron
        chk(m.salidas.tamano() == 9, "la cola de salidas tiene 9 registros");
        java.io.File csv = java.io.File.createTempFile("salidas", ".csv");
        int n1 = evacua.reportes.ExportadorSalidas.exportar(m, csv), n2 = evacua.reportes.ExportadorSalidas.exportar(m, csv);
        List<String> lin = Files.readAllLines(csv.toPath());
        chk(n1 == 9 && n2 == 9 && lin.size() == 10 && lin.get(0).equals(evacua.reportes.ExportadorSalidas.CABECERA), "CSV de salidas: cabecera + 9 filas, exportable dos veces");
        int previo = -1;
        boolean ordenado = true;
        for (int i = 1; i < lin.size(); i++) {
            String[] c = lin.get(i).split(",");
            ordenado &= Integer.parseInt(c[0]) == i && Integer.parseInt(c[4]) >= previo;
            previo = Integer.parseInt(c[4]);
        }
        chk(ordenado, "el CSV esta en orden de salida (tiempos no decrecientes)");

        // Bloquear durante la simulacion recalcula las rutas (RN-07)
        ParametrosSimulacion pr = new ParametrosSimulacion();
        pr.prob = 0;
        MotorSimulacion m2 = new MotorSimulacion(e.copia(), pr, x -> { });
        m2.iniciar();
        m2.paso();
        Persona arriba = m2.mapa.personas.get(8);
        int[] objetivo = arriba.ruta.get(arriba.ruta.size() / 2);   // un nodo de su ruta
        int antes = arriba.historial.recalculos;
        m2.mapa.n[objetivo[0]][objetivo[1]].bloqueado = true;
        m2.alBloquear(objetivo[0], objetivo[1]);
        chk(arriba.historial.recalculos == antes + 1 && arriba.historial.bloqueosSufridos == 1, "al bloquear un nodo de su ruta se recalcula y se cuenta el bloqueo");
        for (int[] q : arriba.ruta) chk(!(q[0] == objetivo[0] && q[1] == objetivo[1]), "la nueva ruta evita el nodo bloqueado");

        // Bloquear la unica escalera del piso 1: arriba no pueden bajar; al desbloquear, si
        MotorSimulacion m3 = new MotorSimulacion(e.copia(), pr, x -> { });
        m3.iniciar();
        m3.mapa.n[5][10].bloqueado = true;
        m3.alBloquear(5, 10);
        chk(m3.mapa.personas.get(8).ruta.isEmpty(), "con la escalera del piso 1 bloqueada, el piso 3 no tiene ruta");
        m3.mapa.n[5][10].bloqueado = false;
        m3.recalcularTodas();
        chk(!m3.mapa.personas.get(8).ruta.isEmpty(), "al desbloquear se recalcula y vuelve a haber ruta");

        System.out.println(fallos == 0 ? "Pisos: TODAS LAS PRUEBAS OK" : "Pisos: " + fallos + " fallos");
        if (fallos > 0) System.exit(1);
    }
}
