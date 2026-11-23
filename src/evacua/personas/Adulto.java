package evacua.personas;

import java.util.List;

public class Adulto extends Persona {
    public boolean evacuacionIndividual;

    public Adulto(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.ADULTO, salud, f, c);
    }

    @Override
    public void aumentarEstres(double incremento) {
        super.aumentarEstres(incremento);
        revisarEstres();
    }

    private void revisarEstres() {
        if (estres >= 50 && objetivo != null && objetivo.tipo == TipoPersona.MOVILIDAD) {
            evacuacionIndividual = true;
            objetivo.ayudaFamiliar = false;
            objetivo = null;
        }
    }

    @Override
    public Persona elegirObjetivo(List<Persona> personas) {
        revisarEstres();
        if (evacuacionIndividual || departamentoInicial == 0 || salud == EstadoSalud.INCAPACITADO) return null;
        if (objetivo != null && !objetivo.evacuada) return objetivo;
        Persona candidato = null;
        for (Persona q : personas) {
            if (q.evacuada || q.departamentoInicial != departamentoInicial) continue;
            if (q.tipo != TipoPersona.NINO && (q.tipo != TipoPersona.MOVILIDAD || estres >= 50)) continue;
            boolean reservado = false;
            for (Persona otro : personas)
                if (otro != this && otro.tipo == TipoPersona.ADULTO && !otro.evacuada && otro.objetivo == q)
                    reservado = true;
            if (reservado) continue;
            if (q.tipo == TipoPersona.NINO) return q;
            if (candidato == null) candidato = q;
        }
        return candidato;
    }
}
