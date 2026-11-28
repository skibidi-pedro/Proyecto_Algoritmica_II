package evacua.personas;

import java.util.List;

public class PersonalRescate extends Persona {
    public PersonalRescate(int id, EstadoSalud salud, int f, int c) {
        this(id, salud, f, c, 100);
    }

    private final int resistencia;
    public PersonalRescate(int id, EstadoSalud salud, int f, int c, int resistencia) {
        super(id, TipoPersona.RESCATE, salud, f, c);
        this.resistencia = Math.max(100, Math.min(500, resistencia));
    }

    @Override public double limiteEstres() { return resistencia; }

    public static final double FACTOR_ESTRES = 0.30;
    public boolean retirado, transportando;

    @Override
    public void aumentarEstres(double incremento) {
        super.aumentarEstres(incremento * FACTOR_ESTRES);
        if (estres >= limiteEstres()) retirado = true;
    }

    @Override
    public Persona elegirObjetivo(List<Persona> todas) {
        return elegirObjetivo(todas, (r, q) -> true);
    }

    public Persona elegirObjetivo(List<Persona> todas,
                                  java.util.function.BiPredicate<PersonalRescate, Persona> alcanzable) {
        return elegirObjetivo(todas, alcanzable, false);
    }

    public Persona elegirObjetivo(List<Persona> todas, java.util.function.BiPredicate<PersonalRescate, Persona> alcanzable,
                                  boolean adultosVulnerables) {
        if (estres >= limiteEstres()) retirado = true;
        if (retirado || transportando || !activa() || salud == EstadoSalud.INCAPACITADO) return null;
        java.util.List<Persona> candidatos = new java.util.ArrayList<>();
        for (Persona q : todas) {
            if (q.activa() && !q.asistida && (q.tipo == TipoPersona.NINO || q.tipo == TipoPersona.MOVILIDAD
                    || adultosVulnerables && q.tipo == TipoPersona.ADULTO && (q.confundida() || q.salud == EstadoSalud.HERIDO || q.salud == EstadoSalud.INCAPACITADO)))
                candidatos.add(q);
        }
        candidatos.sort(java.util.Comparator.comparing((Persona q) -> q.salud != EstadoSalud.INCAPACITADO)
                .thenComparingInt(q -> Math.abs(q.f - f) + Math.abs(q.c - c)));
        for (Persona q : candidatos) {
            int necesarios = q.tipo == TipoPersona.MOVILIDAD ? 2 : 1;
            int asignados = 0, disponibles = 1;
            if (!alcanzable.test(this, q)) continue;
            for (Persona otro : todas) {
                if (otro == this || !(otro instanceof PersonalRescate)) continue;
                PersonalRescate r = (PersonalRescate) otro;
                if (!r.activa() || r.retirado || r.estres >= r.limiteEstres() || r.transportando
                        || r.salud == EstadoSalud.INCAPACITADO) continue;
                if (r.objetivo != q && (necesarios == 1 || r.objetivo != null && r.objetivo.activa())) continue;
                if (!alcanzable.test(r, q)) continue;
                if (r.objetivo == q) asignados++;
                disponibles++;
            }
            if (asignados < necesarios && disponibles >= necesarios) return q;
        }
        return null;
    }
}
