package evacua;

import evacua.edificio.*;
import evacua.persistencia.GestorEscenarios;
import evacua.rutas.BuscadorRutas;
import java.nio.file.*;

public class PruebasDepartamentos {
    private static void exigir(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    public static void main(String[] args) throws Exception {
        Edificio e = GestorEscenarios.leer(Path.of("recursos/planos/departamentos.txt").toFile());
        for (Edificio m : new Edificio[]{e, e.copiaConPisos(3)}) {
            exigir(m.n[0][0].departamento == 1 && m.n[1][1].departamento == 1, "Primera región y puerta");
            exigir(m.n[0][4].departamento == 2 && m.n[4][0].departamento == 3, "Separación y orden global");
            exigir(m.n[2][0].departamento == 0 && m.n[0][2].departamento == 0, "Pasillo y vacío excluidos");
            m.eliminarNodo(1,1);
            exigir(m.n[0][0].departamento == 0 && m.n[0][4].departamento == 1, "Actualización al borrar");
            m.crearNodo(TipoNodo.PUERTA,1,1,null);
            m.bloquear(1,1,true);
            Departamentos.detectar(m);
            exigir(m.n[1][1].departamento == 1, "Independiente de bloqueo");
            m.bloquear(1,1,false);
        }
        e = GestorEscenarios.deTexto("HDPS\n#PERSONAS\n0 0 ADULTO SANO");
        for (boolean astar : new boolean[]{true,false})
            exigir(BuscadorRutas.buscar(e,0,0,e.salidas(),astar).costo == 3, "Paso por puerta");
        e.n[0][1].peligroso = true;
        for (String ext : new String[]{".json",".xml",".txt"}) {
            Path p = Files.createTempFile("puertas",ext);
            try {
                GestorEscenarios.guardar(p.toFile(),e);
                Edificio copia = GestorEscenarios.leer(p.toFile());
                exigir(copia.n[0][1].tipo == TipoNodo.PUERTA && copia.n[0][1].peligroso
                        && copia.n[0][0].departamento == 1 && copia.personas.size() == 1, "Ida y vuelta " + ext);
            } finally { Files.delete(p); }
        }
        e.modificarNodo(0,2,TipoNodo.ESCALERA);
        exigir(e.n[0][1].departamento == 0, "Puerta incompleta");
        exigir(Departamentos.identificador(1).equals("Dep-001"), "Formato");
        System.out.println("Departamentos: TODAS LAS PRUEBAS OK");
    }
}
