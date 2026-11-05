package evacua.personas;

import java.util.ArrayList;
import java.util.List;

/** Persona dentro del edificio. {@code hist} es su bitacora de recorrido. */
public class Persona {
    public final int id;
    public final TipoPersona tipo;
    public final EstadoSalud salud;
    public int f, c;
    public double estres, acum;
    public boolean evacuada, asistida;
    public int tEvac = -1, pasos, recalculos;
    public List<int[]> ruta = new ArrayList<>();
    public Persona objetivo;
    public final List<String> hist = new ArrayList<>();

    public Persona(int id, TipoPersona tipo, EstadoSalud salud, int f, int c) {
        this.id = id;
        this.tipo = tipo;
        this.salud = salud;
        this.f = f;
        this.c = c;
    }

    public String etiqueta() {
        return "P" + id + " (" + tipo.nombre + ", " + salud + ")";
    }
}
