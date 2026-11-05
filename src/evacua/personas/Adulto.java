package evacua.personas;

/** Adulto: velocidad normal. */
public class Adulto extends Persona {
    public Adulto(int id, EstadoSalud salud, int f, int c) {
        super(id, TipoPersona.ADULTO, salud, f, c);
    }
}
