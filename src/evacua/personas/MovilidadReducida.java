package evacua.personas;

/** Persona con movilidad reducida: la mas lenta. */
public class MovilidadReducida extends Persona {
    public MovilidadReducida(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.MOVILIDAD, salud, f, c);
    }
}
