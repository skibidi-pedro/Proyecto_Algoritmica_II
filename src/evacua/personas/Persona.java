package evacua.personas;

import java.util.ArrayList;
import java.util.List;

public abstract class Persona {
    public final int id;
    public final TipoPersona tipo;
    public final EstadoSalud salud;
    public int f, c;
    public double estres, acum;
    public boolean evacuada, asistida;
    public int tEvac = -1, pasos;
    public List<int[]> ruta = new ArrayList<>();
    public Persona objetivo;

    public int esperando;
    public boolean recalculoPorCongestion;
    public final List<String> hist = new ArrayList<>();
    public final Bitacora bitacora = new Bitacora();
    public final HistorialEventos historial = new HistorialEventos();

    protected Persona(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
        this.id = id;
        this.tipo = tipo;
        this.salud = salud;
        this.f = f;
        this.c = c;
    }

    public static Persona crear(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
        switch (tipo) {
            case NINO: return new Nino(id, salud, f, c);
            case MOVILIDAD: return new MovilidadReducida(id, salud, f, c);
            case RESCATE: return new PersonalRescate(id, salud, f, c);
            default: return new Adulto(id, salud, f, c);
        }
    }

    public Persona elegirObjetivo(List<Persona> todas) {
        return null;
    }

    public void aumentarEstres(double d) {
        estres = NivelEstres.limitar(estres + d);
    }

    public void reducirEstres(double d) {
        estres = NivelEstres.limitar(estres - d);
    }

    public String etiqueta() {
        return "P" + id + " (" + tipo.nombre + ", " + salud + ")";
    }
}
