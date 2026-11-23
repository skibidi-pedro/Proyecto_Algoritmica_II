package evacua.personas;

import java.util.List;

public class PersonalRescate extends Persona {
    public PersonalRescate(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.RESCATE, salud, f, c);
    }

    public static final double FACTOR_ESTRES = 0.30;
    public boolean retirado, transportando;

    @Override
    public void aumentarEstres(double incremento) {
        super.aumentarEstres(incremento * FACTOR_ESTRES);
        if (estres >= NivelEstres.MAX) retirado = true;
    }

    @Override
    public Persona elegirObjetivo(List<Persona> todas) {
        if (estres >= NivelEstres.MAX) retirado = true;
        if (retirado || transportando) return null;
        Persona incapacitado = masCercano(todas, true);
        return incapacitado != null ? incapacitado : masCercano(todas, false);
    }

    private Persona masCercano(List<Persona> todas, boolean soloIncapacitados) {
        Persona best = null;
        int bd = Integer.MAX_VALUE;
        for (Persona q : todas) {
            if (q == this || q.evacuada || q.asistida) continue;
            boolean incap = q.salud == EstadoSalud.INCAPACITADO;
            if (q.tipo != TipoPersona.NINO && q.tipo != TipoPersona.MOVILIDAD || soloIncapacitados && !incap) continue;
            int necesarios = q.tipo == TipoPersona.NINO ? 1 : 2;
            int asignados = 0, disponibles = 1;
            for (Persona otro : todas) {
                if (otro == this || !(otro instanceof PersonalRescate)) continue;
                PersonalRescate r = (PersonalRescate) otro;
                if (r.evacuada || r.retirado || r.transportando || r.salud == EstadoSalud.INCAPACITADO) continue;
                if (r.objetivo == q) asignados++;
                if (r.objetivo == null || r.objetivo == q || r.objetivo.evacuada) disponibles++;
            }
            if (asignados >= necesarios || disponibles < necesarios) continue;
            int d = Math.abs(q.f - f) + Math.abs(q.c - c);
            if (d < bd) {
                bd = d;
                best = q;
            }
        }
        return best;
    }
}
