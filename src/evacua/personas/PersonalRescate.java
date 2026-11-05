package evacua.personas;

import java.util.List;

/**
 * Personal de rescate (RN-17): antes de evacuar se dirige primero a los nodos con personas
 * incapacitadas y luego a ninos o personas con movilidad reducida.
 */
public class PersonalRescate extends Persona {
    public PersonalRescate(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.RESCATE, salud, f, c);
    }

    @Override
    public Persona elegirObjetivo(List<Persona> todas) {
        Persona incapacitado = masCercano(todas, true);
        return incapacitado != null ? incapacitado : masCercano(todas, false);
    }

    private Persona masCercano(List<Persona> todas, boolean soloIncapacitados) {
        Persona best = null;
        int bd = Integer.MAX_VALUE;
        for (Persona q : todas) {
            if (q == this || q.evacuada || q.asistida) continue;
            boolean incap = q.salud == EstadoSalud.INCAPACITADO;
            boolean prioritaria = incap || q.tipo == TipoPersona.NINO || q.tipo == TipoPersona.MOVILIDAD;
            if (soloIncapacitados ? !incap : !prioritaria) continue;
            int d = Math.abs(q.f - f) + Math.abs(q.c - c);
            if (d < bd) {
                bd = d;
                best = q;
            }
        }
        return best;
    }
}
