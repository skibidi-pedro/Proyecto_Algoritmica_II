package evacua;

import evacua.edificio.Edificio;
import evacua.persistencia.GestorEscenarios;
import evacua.personas.*;
import evacua.simulacion.*;
import java.nio.file.*;

public class PruebasFamiliares {
    static void exigir(boolean condicion, String detalle) {
        if (!condicion) throw new AssertionError(detalle);
    }

    static MotorSimulacion escenario(String personas) {
        Edificio e = GestorEscenarios.deTexto("HHDPS\n#PERSONAS\n" + personas);
        ParametrosSimulacion params = new ParametrosSimulacion();
        params.prob = 0;
        MotorSimulacion m = new MotorSimulacion(e, params, s -> {});
        m.iniciar();
        return m;
    }

    public static void main(String[] args) throws Exception {
        MotorSimulacion m = escenario("0 0 ADULTO SANO\n0 0 NINO INCAPACITADO\n0 1 ADULTO SANO");
        Persona adulto = m.mapa.personas.get(0), menor = m.mapa.personas.get(1);
        exigir(adulto.objetivo == menor && m.mapa.personas.get(2).objetivo == null, "Un acompañante por persona");
        exigir(m.mapa.n[0][0].ocupacion == 1.5, "Un adulto y un niño ocupan 1,5");
        m.paso();
        exigir(menor.ayudaFamiliar && !menor.asistida && menor.acum == 0.25, "Asistencia familiar al 50 %");
        while (m.paso()) {}
        exigir(m.evacuadas() == 3 && adulto.departamentoInicial == 1 && menor.departamentoInicial == 1,
                "Evacuación con acompañamiento y pertenencia estable");

        m = escenario("0 0 ADULTO SANO\n0 0 MOVILIDAD INCAPACITADO");
        adulto = m.mapa.personas.get(0);
        Persona movilidad = m.mapa.personas.get(1);
        m.paso();
        exigir(movilidad.ayudaFamiliar, "Acompaña con estrés bajo");
        adulto.aumentarEstres(50);
        adulto.reducirEstres(40);
        m.paso();
        exigir(!movilidad.ayudaFamiliar && adulto.objetivo == null, "El abandono persiste al bajar el estrés");

        m = escenario("0 0 ADULTO SANO\n0 0 NINO INCAPACITADO\n0 0 RESCATE SANO");
        Persona profesional = m.mapa.personas.get(2);
        menor = m.mapa.personas.get(1);
        exigir(profesional.departamentoInicial == 0 && profesional.objetivo == menor, "Rescate independiente");
        exigir(m.mapa.n[0][0].ocupacion == 2, "Rescatista ocupa media plaza");
        m.paso();
        exigir(menor.asistida && !menor.ayudaFamiliar, "Prevalece el rescatista");
        m = escenario("0 3 ADULTO SANO\n0 0 NINO INCAPACITADO\n0 3 RESCATE SANO");
        exigir(m.mapa.personas.get(0).objetivo == null && m.mapa.personas.get(2).objetivo == m.mapa.personas.get(1),
                "El profesional auxilia fuera de su zona inicial");

        menor = m.mapa.personas.get(1);
        menor.c = 3;
        for (String ext : new String[]{".txt",".xml",".json"}) {
            Path f = Files.createTempFile("familia", ext);
            try {
                GestorEscenarios.guardar(f.toFile(),m.mapa);
                Edificio copia = GestorEscenarios.leer(f.toFile());
                exigir(copia.personas.get(1).departamentoInicial == 1 && copia.personas.get(2).departamentoInicial == 0,
                        "Persistencia de la pertenencia " + ext);
            } finally { Files.delete(f); }
        }
        System.out.println("Familiares: TODAS LAS PRUEBAS OK");
    }
}
