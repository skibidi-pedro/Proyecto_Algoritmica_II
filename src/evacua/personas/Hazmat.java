package evacua.personas;

public final class Hazmat extends PersonalRescate {
    public Hazmat(int id, int f, int c, int resistencia) {
        super(id, EstadoSalud.SANO, f, c, resistencia);
        alertada = true;
    }
    @Override public String etiqueta() { return "P" + id + " (HAZMAT, " + salud + ")"; }
}
