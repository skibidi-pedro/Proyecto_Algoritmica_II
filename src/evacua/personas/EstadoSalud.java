package evacua.personas;

public enum EstadoSalud {
    SANO(1.0), HERIDO(0.6), ENFERMO(0.8), INCAPACITADO(0.0);

    public final double vel;

    EstadoSalud(double vel) {
        this.vel = vel;
    }
}
