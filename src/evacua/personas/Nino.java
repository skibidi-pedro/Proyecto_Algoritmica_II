package evacua.personas;

/** Nino: mas lento que un adulto. */
public class Nino extends Persona {
    public Nino(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.NINO, salud, f, c);
    }
}
