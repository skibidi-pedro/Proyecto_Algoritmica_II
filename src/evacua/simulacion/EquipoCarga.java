package evacua.simulacion;

import evacua.personas.*;
import java.util.ArrayList;
import java.util.List;

final class EquipoCarga {
    final Persona paciente;
    final List<PersonalRescate> portadores;
    final List<Persona> integrantes = new ArrayList<>();

    EquipoCarga(Persona paciente, List<PersonalRescate> portadores) {
        this.paciente = paciente;
        this.portadores = portadores;
        integrantes.addAll(portadores);
        integrantes.add(paciente);
    }

    PersonalRescate lider() { return portadores.get(0); }

    double ocupacion() { return portadores.size() * TipoPersona.RESCATE.ocupacion(); }
}
