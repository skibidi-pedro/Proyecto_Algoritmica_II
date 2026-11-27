package evacua.personas;

/** Una unidad de bomberos conserva el transporte y la resistencia base de PersonalRescate. */
public final class Bombero extends PersonalRescate {
    private final int resistencia;
    public int foco = -1;
    public Bombero(int id, int f, int c, int resistencia) {
        super(id, EstadoSalud.SANO, f, c);
        this.resistencia = Math.min(500, Math.max(100, resistencia));
        alertada = true;
    }
    @Override public double limiteEstres() { return resistencia; }
    @Override public String etiqueta() { return "P" + id + " (Bombero, " + salud + ")"; }

    public int prioridad(Persona p, evacua.edificio.Nodo n) {
        if (p == this || p instanceof Bombero || !p.activa() || p.asistida) return 5;
        if (n.fuegoActivo) return 1;
        if (n.humo >= 95) return 2;
        return p.salud == EstadoSalud.HERIDO || p.salud == EstadoSalud.INCAPACITADO ? 4 : 5;
    }
}
